package cn.lanink.teamsystem.team

import cn.lanink.teamsystem.TeamSystem.Companion.language
import cn.lanink.teamsystem.TeamSystem.Companion.serverChannel
import cn.lanink.teamsystem.distribute.pack.Packet
import cn.lanink.teamsystem.team.dao.Dao
import cn.lanink.teamsystem.team.view.TeamForm
import cn.lanink.teamsystem.team.view.TeamInventory
import cn.nukkit.Player
import cn.nukkit.Server
import java.util.*

class Team(private val dao: Dao) : Dao by dao {

    // form 式 UI
    val formUI: TeamForm = TeamForm(this)
    // 背包式 UI
    val inveUI: TeamInventory = TeamInventory(this)

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is Team) {
            return false
        }
        return id == other.id
    }

    override fun applyFrom(playerName: String) {
        dao.applyFrom(playerName)
        val leader = leaderName
        if (!isOnline(leader))  // leader 暂时离线
            return
        val player: Player? = Server.getInstance().getPlayer(leader)
        if (player?.isOnline == true) { // leader 在本服
            player.sendMessage(language.translateString("tips.teamReceiveApplication", playerName))
            return
        }
        // 发送给群组服 master
        serverChannel?.writeAndFlush(Packet.Message(target = leader, message = language.translateString("tips.teamReceiveApplication", playerName)))
    }

    override fun addPlayer(playerName: String) {
        dao.addPlayer(playerName)
        val player: Player? = Server.getInstance().getPlayer(playerName)
        if (player?.isOnline == true) {
            player.sendMessage(language.translateString("tip.joined"))
            return
        }
        // 告诉其他服务器里面的这个玩家
        serverChannel?.writeAndFlush(Packet.Message(target = playerName, message = language.translateString("tip.joined")))
    }

    override fun disband() {
        dao.disband()
        for (playerName in this.players) {
            val player = Server.getInstance().getPlayer(playerName)
            if (player != null && player.isOnline) { // 检查是否是本服玩家
                player.sendMessage(language.translateString("tips.teamDisbanded"))
            } else {
                serverChannel?.writeAndFlush(Packet.Message(target = playerName, message = language.translateString("tips.teamDisbanded")))
            }
        }
    }

    override fun hashCode(): Int {
        return Objects.hash(id)
    }
}