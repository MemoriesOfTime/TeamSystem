package cn.lanink.teamsystem

import cn.lanink.teamsystem.dao.*
import cn.nukkit.Player
import cn.nukkit.Server
import org.ktorm.database.Database
import org.ktorm.dsl.and
import org.ktorm.dsl.eq
import org.ktorm.dsl.insert
import org.ktorm.dsl.update
import org.ktorm.entity.add
import org.ktorm.entity.find
import org.ktorm.entity.removeIf
import org.ktorm.entity.update
import java.util.*

/**
 * @author lt_name
 */
class Team(val id: Int, val name: String, val maxPlayers: Int, leader: Player) {

    private val database: Database? = TeamSystem.getInstance().database
    private var teamLeader: String
    val players = HashSet<String>()
    val applicationList = HashSet<String>()

    init {
        // cache-aside
        database?.teams?.add(OnlineTeam{
            id = this@Team.id
            teamName = this@Team.name
            maxPlayers = this@Team.maxPlayers
            teamLeader = database.onlinePlayers.find { it.playerName eq leader.name }!!
        })
        this.teamLeader = leader.name
        players.add(leader.name)
    }

    /**
     * 注意该方法在多个服务器间可能不具备一致性，如果需要一致性使用 isTeamLeaderRight
     */
    fun isTeamLeader(leader: Player): Boolean {
        var checked = this.teamLeader == leader.name
        if (database != null && "" == this.teamLeader) {  // 缓存失效
            val updated = database.teams.find {
                it.id eq this.id
            }?.teamLeader?.name
            checked = this.teamLeader == updated
            if (updated != null) {
                this.teamLeader = updated
                Server.getInstance().scheduler.scheduleDelayedTask(TeamSystem.getInstance(), {
                    this.teamLeader = ""
                }, 2*20*60)  // 2 分钟后将缓存失效 TODO 时间
            }
        }
        return checked
    }

    /**
     * 该方法只适用于多个服务器之间
     */
    fun isTeamLeaderRight(leader: Player): Boolean {
        return leader.name == database?.teams?.find {
            it.id eq this.id
        }?.teamLeader?.name
    }

    fun setTeamLeader(leader: Player) {
        database?.teams?.update(OnlineTeam{
            id = this@Team.id
            teamLeader = database.onlinePlayers.find { it.playerName eq leader.name }!!
        })
        this.teamLeader = leader.name
    }

    /**
     * 这个方法在多个服务器间可能不具备一致性，如果需要一致性请使用 getTeamLeaderRight
     */
    fun getTeamLeader(): Player {
        return Server.getInstance().getPlayer(teamLeader)
    }

    /**
     * 该方法只适用于多个服务器之间
     */
    fun getTeamLeaderRight(): Player {
        val leaderName: String = database?.teams?.find {
            it.id eq this.id
        }?.teamLeader?.name?:""
        return Server.getInstance().getPlayer(leaderName)
    }

    fun addPlayer(player: Player) {
        database?.update(OnlinePlayers) {
            set(it.ofTeam, this@Team.id)
            where {
                it.playerName eq player.name
            }
        }
        players.add(player.name)
    }

    fun removePlayer(player: Player) {
        database?.update(OnlinePlayers) {
            set(it.ofTeam, null)
            where {
                it.playerName eq player.name
            }
        }
        players.remove(player.name)
    }

    fun addApplyForPlayer(p: Player) {
        database?.insert(ApplyList) { col ->
            set(col.player, database.onlinePlayers.find { it.playerName eq p.name }!!.id)
            set(col.team, this@Team.id)
        }
        applicationList.add(p.name)
    }

    fun removeApplyForPlayer(p: Player) {
        database?.applies?.removeIf { col ->
            (col.team eq this.id) and (col.player eq database.onlinePlayers.find { it.playerName eq p.name }!!.id)
        }
        applicationList.remove(p.name)
    }

    /**
     * 解散队伍
     */
    fun disband() {
        database?.teams?.removeIf {
            it.id eq this.id
        }
        database?.applies?.removeIf {
            it.team eq this.id
        }
        database?.update(OnlinePlayers) {
            set(it.ofTeam, null)
            where {
                it.ofTeam eq this@Team.id
            }
        }

        for (playerName in players) {
            val player = Server.getInstance().getPlayer(playerName)
            if (player != null && player.isOnline) {
                player.sendMessage(TeamSystem.getInstance().language.translateString("tips.teamDisbanded"))
            }
        }
        players.clear()
        applicationList.clear()
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) {
            return true
        }
        if (o !is Team) {
            return false
        }
        return id == o.id
    }

    override fun hashCode(): Int {
        return Objects.hash(id)
    }
}
