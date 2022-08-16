package cn.lanink.teamsystem.distribute.server

import cn.lanink.teamsystem.TeamSystem
import cn.lanink.teamsystem.distribute.pack.Pack
import cn.lanink.teamsystem.distribute.pack.Packet
import cn.nukkit.Server
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import java.net.InetSocketAddress
import java.util.concurrent.ConcurrentHashMap

object Handler {

    @JvmStatic
    internal val handlerMap: ConcurrentHashMap<Byte, SimpleChannelInboundHandler<out Packet>> = ConcurrentHashMap()

    init {
        handlerMap[Pack.ID_MESSAGE] = object : SimpleChannelInboundHandler<Packet.Message>() {
            override fun channelRead0(ctx: ChannelHandlerContext, mess: Packet.Message) {
                SessionManager.sessions[mess.dest]?.pipeline?.writeAndFlush(mess)  // 发送给 dest
            }
        }

        handlerMap[Pack.ID_LOGIN] = object : SimpleChannelInboundHandler<Packet.LoginPacket>() {
            override fun channelRead0(ctx: ChannelHandlerContext, mess: Packet.LoginPacket) {
                // 写入已经连接的 session
                SessionManager.sessions[mess.identity] = Session(mess.identity, ctx.pipeline())
                val socket = ctx.channel().remoteAddress() as InetSocketAddress
                val clientIP = socket.address.hostAddress
                val clientPort = socket.port
                TeamSystem.logger.info(TeamSystem.language.translateString("info.newConnection", clientIP, clientPort))
            }
        }

        handlerMap[Pack.ID_TELEPORT_REQ] = object : SimpleChannelInboundHandler<Packet.TeleportRequestPacket>() {
            override fun channelRead0(ctx: ChannelHandlerContext, mess: Packet.TeleportRequestPacket) {
                SessionManager.sessions[mess.dest]?.pipeline?.writeAndFlush(mess)
            }
        }

        handlerMap[Pack.ID_TELEPORT_RESP] = object : SimpleChannelInboundHandler<Packet.TeleportResponsePacket>() {
            override fun channelRead0(ctx: ChannelHandlerContext, mess: Packet.TeleportResponsePacket) {
                SessionManager.sessions[mess.dest]?.pipeline?.writeAndFlush(mess)
            }
        }
    }
}