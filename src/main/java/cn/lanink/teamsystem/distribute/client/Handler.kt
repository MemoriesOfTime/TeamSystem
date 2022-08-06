package cn.lanink.teamsystem.distribute.client

import cn.lanink.formdsl.dsl.*
import cn.lanink.teamsystem.EventListener
import cn.lanink.teamsystem.TeamSystem
import cn.lanink.teamsystem.distribute.pack.Pack
import cn.lanink.teamsystem.distribute.pack.Packet
import cn.nukkit.Player
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
                val player: Player? = Server.getInstance().getPlayer(mess.target)
                if (player?.isOnline == true) {
                    player.sendMessage(mess.message)
                }
            }
        }
        
        handlerMap[Pack.ID_TELEPORT_REQ] = object : SimpleChannelInboundHandler<Packet.TeleportRequestPacket>() {
            override fun channelRead0(ctx: ChannelHandlerContext, mess: Packet.TeleportRequestPacket) {
                val receiver: Player = Server.getInstance().getPlayer(mess.target) ?: return
                if (!receiver.isOnline) return
                FormModal {
                    target = receiver
                    title = TeamSystem.language.translateString("form.teleport.handle.title")
                    content = TeamSystem.language.translateString("form.teleport.handle.content", mess.sender)
                    trueText = TeamSystem.language.translateString("general.approve")
                    falseText = TeamSystem.language.translateString("general.refuse")
                    onTrue {
                        // 写入传送列表
                        EventListener.transferTeleportQueue[mess.sender] = receiver
                        // 写回发送者服务器
                        TeamSystem.serverChannel?.writeAndFlush(Packet.TeleportResponsePacket(
                            dest = mess.identity,
                            target = mess.sender,
                            ok = true,
                            ip = TeamSystem.exposeHost,
                            port = Server.getInstance().port
                        ))
                    }
                    onFalse {
                        TeamSystem.serverChannel?.writeAndFlush(Packet.TeleportResponsePacket(
                            dest = mess.identity,
                            target = mess.sender,
                            ok = false
                        ))
                    }
                    onClose {
                        TeamSystem.serverChannel?.writeAndFlush(Packet.TeleportResponsePacket(
                            dest = mess.identity,
                            target = mess.sender,
                            ok = false
                        ))
                    }
                }
            }
        }

        handlerMap[Pack.ID_TELEPORT_RESP] = object : SimpleChannelInboundHandler<Packet.TeleportResponsePacket>() {
            override fun channelRead0(ctx: ChannelHandlerContext, mess: Packet.TeleportResponsePacket) {
                val player = Server.getInstance().getPlayer(mess.target) ?: return
                if (!player.isOnline) {
                    return
                }
                if (mess.ok) {
                    // 跨服
                    player.transfer(InetSocketAddress(mess.ip, mess.port))
                } else {
                    player.sendMessage(TeamSystem.language.translateString("form.teleport.refused"))
                }
            }
        }

    }
}