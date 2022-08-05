package cn.lanink.teamsystem.distribute.client

import cn.lanink.teamsystem.TeamSystem.Companion.logger
import cn.lanink.teamsystem.distribute.pack.Pack
import cn.lanink.teamsystem.distribute.pack.Packet
import cn.lanink.teamsystem.distribute.server.PacketCodecHandler
import io.netty.bootstrap.Bootstrap
import io.netty.buffer.ByteBufAllocator
import io.netty.channel.*
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.timeout.IdleStateEvent
import io.netty.handler.timeout.IdleStateHandler
import java.net.InetSocketAddress

fun startClient(identity: String, host: String, port: Int): Pair<Channel, EventLoopGroup> {
    val worker = NioEventLoopGroup()
    val bootstrap = Bootstrap()
    bootstrap.group(worker).channel(NioSocketChannel::class.java)
        .handler(object : ChannelInitializer<SocketChannel>() {
            @Throws(Exception::class)
            override fun initChannel(channel: SocketChannel) {
                channel.pipeline().addLast(PacketCodecHandler)
                channel.pipeline().addLast(ClientIdleHandler())
                channel.pipeline().addLast(ClientLogin(identity))
                channel.pipeline().addLast(InboundResponseHandler)
            }
        })

    val future: ChannelFuture = bootstrap.connect(host, port).addListener(object : ChannelFutureListener {
        @Throws(Exception::class)
        override fun operationComplete(channelFuture: ChannelFuture) {
            if (channelFuture.isSuccess) {
                logger.info("Client: 已经连接群组服")
            } else {
                logger.warning("Client: 连接群组服失败")
            }
        }
    })
    future.channel().closeFuture().addListener {
        if (it.isSuccess)
            logger.info("Client: 已从群组服中断开连接")
        else
            logger.warning("Client: 连接已经不可用，但未从群组服中断开连接")
    }
    return Pair<Channel, EventLoopGroup>(future.channel(), worker)
}

class ClientIdleHandler : IdleStateHandler(0, 0, HEART_BEAT_TIME) {

    @Throws(Exception::class)
    override fun channelIdle(ctx: ChannelHandlerContext, evt: IdleStateEvent?) {
        ctx.writeAndFlush(Packet.HeartBeatPacket())
    }

    companion object {
        private const val HEART_BEAT_TIME = 30
    }
}

@ChannelHandler.Sharable
class ClientLogin(private val identity: String): ChannelInboundHandlerAdapter() {
    @Throws(Exception::class)
    override fun channelActive(ctx: ChannelHandlerContext) {
        val packet = Packet.LoginPacket(identity = identity)
        val byteBuf = ByteBufAllocator.DEFAULT.ioBuffer()
        Pack.encode(packet, byteBuf)
        ctx.channel().writeAndFlush(byteBuf)
    }
}

object InboundResponseHandler : SimpleChannelInboundHandler<Packet>() {
    override fun channelRead0(ctx: ChannelHandlerContext, pack: Packet) {
        if (pack.packID == Pack.ID_HEARTBEAT) return  // 忽略心跳包
        val handler: SimpleChannelInboundHandler<out Packet>? = Handler.handlerMap[pack.packID]
        handler ?: logger.warning("Client: 未找到对应数据包的 Handler") //TODO translate
        handler?.channelRead(ctx, pack)
    }

    @Throws(Exception::class)
    override fun channelInactive(ctx: ChannelHandlerContext) {
        val socket = ctx.channel().remoteAddress() as InetSocketAddress
        val ip = socket.address.hostAddress
        val port = socket.port
        logger.warning("Client: 群组服master节点断开: $ip : $port")
        super.channelInactive(ctx)
    }
}