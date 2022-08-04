package cn.lanink.teamsystem.team

import cn.lanink.teamsystem.TeamSystem.Companion.language
import cn.lanink.teamsystem.team.dao.Dao
import cn.lanink.teamsystem.team.view.TeamForm
import cn.lanink.teamsystem.team.view.TeamInventory
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
        // TODO 发送消息给队长
    }

    override fun addPlayer(playerName: String) {
        dao.addPlayer(playerName)
        Server.getInstance().getPlayer(playerName)?.sendMessage(language.translateString("tip.joined"))
        // TODO 广播
    }

    override fun disband() {
        dao.disband()
        for (playerName in this.players) {
            val player = Server.getInstance().getPlayer(playerName)
            if (player != null && player.isOnline) { // 检查是否是本服玩家
                player.sendMessage(language.translateString("tips.teamDisbanded"))
                // TODO 广播
            }
        }
    }

    override fun hashCode(): Int {
        return Objects.hash(id)
    }
}