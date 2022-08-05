package cn.lanink.teamsystem.distribute.client

import cn.lanink.teamsystem.distribute.pack.Pack
import cn.lanink.teamsystem.distribute.pack.Packet
import cn.nukkit.Player
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
                val player: Player? = Server.getInstance().getPlayer(mess.target)
                if (player?.isOnline == true) {
                    player.sendMessage(mess.message)
                }
            }
        }

    }
}