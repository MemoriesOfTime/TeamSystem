package cn.lanink.teamsystem.distribute.server

import cn.lanink.teamsystem.TeamSystem
import cn.lanink.teamsystem.distribute.pack.Pack
import cn.lanink.teamsystem.distribute.pack.Packet
import cn.nukkit.Server
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import java.util.concurrent.ConcurrentHashMap

object Handler {

    @JvmStatic
    internal val handlerMap: ConcurrentHashMap<Byte, SimpleChannelInboundHandler<out Packet>> = ConcurrentHashMap()

    init {
        handlerMap[Pack.ID_MESSAGE] = object : SimpleChannelInboundHandler<Packet.Message>() {
            override fun channelRead0(ctx: ChannelHandlerContext, mess: Packet.Message) {
                SessionManager.sessions.filterNot {  // 排除发送者
                    it.key == mess.identity
                }.forEach { (_, session) ->
                    session.pipeline.writeAndFlush(mess)  // 分发给所有的 session TODO 加上 dest
                }
            }
        }

        handlerMap[Pack.ID_LOGIN] = object : SimpleChannelInboundHandler<Packet.LoginPacket>() {
            override fun channelRead0(ctx: ChannelHandlerContext, mess: Packet.LoginPacket) {
                // 写入已经连接的 session
                SessionManager.sessions[mess.identity] = Session(mess.identity, ctx.pipeline())
            }
        }
    }
}