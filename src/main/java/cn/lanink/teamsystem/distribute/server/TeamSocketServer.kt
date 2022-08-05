package cn.lanink.teamsystem.distribute.server

import cn.lanink.teamsystem.TeamSystem
import cn.lanink.teamsystem.distribute.pack.Pack
import cn.lanink.teamsystem.distribute.pack.Packet
import io.netty.bootstrap.ServerBootstrap
import io.netty.buffer.ByteBuf
import io.netty.channel.*
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.codec.LengthFieldBasedFrameDecoder
import io.netty.handler.codec.MessageToMessageCodec
import io.netty.handler.timeout.IdleStateEvent
import io.netty.handler.timeout.IdleStateHandler
import java.net.InetSocketAddress

fun startServer(port: Int): Pair<EventLoopGroup, EventLoopGroup> {
    val boss = NioEventLoopGroup(2)
    val worker = NioEventLoopGroup()
    val bootstrap = ServerBootstrap()
    bootstrap.group(boss, worker).channel(NioServerSocketChannel::class.java)
        .option(ChannelOption.SO_BACKLOG, 64)  // 最大 64 个连接 (不会一个群组服数量超过背包格子一组吧
        .childOption(ChannelOption.SO_KEEPALIVE, true)
        .childOption(ChannelOption.SO_REUSEADDR, true)
        .childOption(ChannelOption.TCP_NODELAY, true)
        .childHandler(object : ChannelInitializer<NioSocketChannel>() {

            @Throws(Exception::class)
            override fun initChannel(nioSocketChannel: NioSocketChannel) {
                val pipeline = nioSocketChannel.pipeline()
                pipeline.addLast(ServerIdleHandler())
                pipeline.addLast(MagicNumValidator())
                pipeline.addLast(PacketCodecHandler)
                pipeline.addLast(HeartBeatHandler)
                pipeline.addLast(ResponseHandler)
            }
        })

    val future: ChannelFuture = bootstrap.bind(port).sync()

    future.addListener(object : ChannelFutureListener {
        @Throws(Exception::class)
        override fun operationComplete(channelFuture: ChannelFuture) {
            if (channelFuture.isSuccess) {
                TeamSystem.logger.info("Server: 群组服在端口 $port 上已经开启")
            } else {
                TeamSystem.logger.info("Server: 群组服开启失败")
            }
        }
    })
    return Pair<EventLoopGroup, EventLoopGroup>(boss, worker)
}

class ServerIdleHandler : IdleStateHandler(0, 0, HEART_BEAT_TIME) {

    @Throws(Exception::class)
    override fun channelIdle(ctx: ChannelHandlerContext, evt: IdleStateEvent) {
        ctx.channel().close()
    }

    companion object {
        private const val HEART_BEAT_TIME = 300
    }
}

class MagicNumValidator : LengthFieldBasedFrameDecoder(Int.MAX_VALUE, LENGTH_FIELD_OFFSET, LENGTH_FIELD_LENGTH) {
    @Throws(Exception::class)
    override fun decode(ctx: ChannelHandlerContext, `in`: ByteBuf): Any? {
        if (`in`.getInt(`in`.readerIndex()) != Pack.MAGIC_NUM) {
            ctx.channel().close()
            return null
        }
        return super.decode(ctx, `in`)
    }

    companion object {
        private const val LENGTH_FIELD_OFFSET = 6
        private const val LENGTH_FIELD_LENGTH = 4
    }
}

@ChannelHandler.Sharable
object PacketCodecHandler : MessageToMessageCodec<ByteBuf, Packet>() {

    override fun encode(ctx: ChannelHandlerContext, msg: Packet, list: MutableList<Any>) {
        val byteBuf = ctx.channel().alloc().ioBuffer()
        Pack.encode(msg, byteBuf)
        list.add(byteBuf)
    }

    override fun decode(ctx: ChannelHandlerContext, msg: ByteBuf, list: MutableList<Any>) {
        list.add(Pack.decode(msg))
    }

}

@ChannelHandler.Sharable
object HeartBeatHandler : SimpleChannelInboundHandler<Packet.HeartBeatPacket>(){
    override fun channelRead0(ctx: ChannelHandlerContext, msg: Packet.HeartBeatPacket) {
        ctx.writeAndFlush(msg)
    }
}

object ResponseHandler: SimpleChannelInboundHandler<Packet>() {

    override fun channelRead0(ctx: ChannelHandlerContext, msg: Packet) {
        val handler: SimpleChannelInboundHandler<out Packet>? = Handler.handlerMap[msg.packID]
        handler ?: TeamSystem.logger.warning("Server: 未找到对应数据包的 Handler") //TODO translate
        handler?.channelRead(ctx, msg)
    }

    @Throws(Exception::class)
    override fun channelInactive(ctx: ChannelHandlerContext) {
        val socket = ctx.channel().remoteAddress() as InetSocketAddress
        val clientIP = socket.address.hostAddress
        val clientPort = socket.port
        TeamSystem.logger.warning("Server: 子服务器掉线: $clientIP : $clientPort")
        super.channelInactive(ctx)
    }
}