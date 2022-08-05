package cn.lanink.teamsystem.distribute.client

import cn.lanink.teamsystem.TeamSystem
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

fun startClient(identity: String, host: String, port: Int) {
    val worker = NioEventLoopGroup()
    val bootstrap = Bootstrap()
    bootstrap.group(worker).channel(NioSocketChannel::class.java)
        .handler(object : ChannelInitializer<SocketChannel>() {
            @Throws(Exception::class)
            override fun initChannel(channel: SocketChannel) {
                channel.pipeline().addLast(PacketCodecHandler)
                channel.pipeline().addLast(ClientIdleHandler())
                channel.pipeline().addLast(ClientLogin(identity))
            }
        })

    val future: ChannelFuture = bootstrap.connect(host, port).addListener(object : ChannelFutureListener {
        @Throws(Exception::class)
        override fun operationComplete(channelFuture: ChannelFuture) {
            if (channelFuture.isSuccess) {
                TeamSystem.logger.info("已经连接群组服")
            } else {
                TeamSystem.logger.warning("连接群组服失败")
            }
        }
    })
    try {
        future.channel().closeFuture().sync()
        TeamSystem.logger.warning("与群组服断开连接")
    } catch (e: InterruptedException) {
        e.printStackTrace()
    }
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
        packet.encode(byteBuf)
        ctx.channel().writeAndFlush(byteBuf)
    }
}