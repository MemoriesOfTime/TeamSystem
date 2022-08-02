package cn.lanink.teamsystem.team.dao

import cn.lanink.teamsystem.TeamSystem
import cn.lanink.teamsystem.db.mysql.*
import cn.lanink.teamsystem.team.TeamManager
import cn.nukkit.Player
import cn.nukkit.Server
import org.ktorm.database.Database
import org.ktorm.dsl.and
import org.ktorm.dsl.eq
import org.ktorm.dsl.insert
import org.ktorm.dsl.update
import org.ktorm.entity.*
import java.util.*

/**
 * @author iGxnon
 */
@Suppress("UNUSED")
class TeamMySQLDao(val id: Int, val name: String, val maxPlayers: Int, leader: String) : TeamDao() {

    private val database: Database = TeamSystem.database!!

    constructor(id: Int, name: String, maxPlayers: Int, leader: Player) : this(
        id, name, maxPlayers, leader.name
    )

    // 获取时会触发更新
    override var leaderName: String = leader
        get() {
            database.teams.find {
                it.id eq this.id
            }?.teamLeader?.let {
                field = it
            }
            return field
        }
        set(value) {
            database.teams.update(OnlineTeam {
                id = this@TeamMySQLDao.id
                teamLeader = value
            })
            field = value
        }

    // players 里面可能有这个服务器没上线的玩家(在其他服务器是上线的)
    // 注意，请不要使用 getPlayers().add("xxx") 的方式添加
    override val players = HashSet<String>()
        get() {
            database.apply {
                field.clear()
            }.onlinePlayers.filter {
                it.ofTeam eq this.id
            }.map {
                it.name
            }.forEach {
                field.add(it)
            }
            return field
        }


    // 申请加入列表
    // 注意，请不要使用 getApplicationList().add("xxx") 的方式添加
    override val applicationList = HashSet<String>()
        get() {
            database.apply {
                field.clear()
            }.applies.filter {
                it.team eq this.id
            }.map {
                it.player.name
            }.forEach {
                field.add(it)
            }
            return field
        }

    override fun isTeamLeader(other: Player): Boolean {
        return this.leaderName == other.name
    }

    override fun isTeamLeader(other: String): Boolean {
        return this.leaderName == other
    }

    override fun setTeamLeader(leader: Player) {
        this.leaderName = leader.name
    }

    /**
     * 多服下可能返回 null
     */
    override fun getTeamLeader(): Player? {
        return Server.getInstance().getPlayer(this.leaderName)
    }

    override fun addPlayer(player: Player) {
        this.addPlayer(player.name)
    }

    override fun addPlayer(playerName: String) {
        database.update(OnlinePlayers) {
            set(it.ofTeam, this@TeamMySQLDao.id)
            where {
                it.playerName eq playerName
            }
        }
    }

    override fun removePlayer(player: Player) {
        this.removePlayer(player.name)
    }

    override fun removePlayer(name: String) {
        database.update(OnlinePlayers) {
            set(it.ofTeam, null)
            where {
                it.playerName eq name
            }
        }
    }

    override fun applyFrom(player: Player) {
        this.applyFrom(player.name)
    }

    override fun applyFrom(playerName: String) {
        database.insert(ApplyList) { col ->
            set(col.player, database.onlinePlayers.find { it.playerName eq playerName }!!.id)
            set(col.team, this@TeamMySQLDao.id)
        }
    }

    override fun cancelApplyFrom(player: Player) {
        this.cancelApplyFrom(player.name)
    }

    override fun cancelApplyFrom(playerName: String) {
        database.applies.removeIf { col ->
            (col.team eq this.id) and (col.player eq database.onlinePlayers.find { it.playerName eq playerName }!!.id)
        }
    }

    override fun isOnline(playerName: String): Boolean {
        if (Server.getInstance().getPlayer(playerName)?.isOnline == true) {
            return true
        }
        return database.onlinePlayers.find {
            it.playerName eq playerName
        }?.quitAt == null
    }

    /**
     * 解散队伍
     * todo 同步解散消息给其他服务器玩家
     */
    override fun disband() {
        database.update(OnlinePlayers) {
            set(it.ofTeam, null)
            where {
                it.ofTeam eq this@TeamMySQLDao.id
            }
        }
        database.applies.removeIf {
            it.team eq this.id
        }
        database.teams.removeIf {
            it.id eq this.id
        }

        for (playerName in this.players) {
            val player = Server.getInstance().getPlayer(playerName)
            if (player != null && player.isOnline) { // 检查是否是本服玩家
                player.sendMessage(TeamSystem.language.translateString("tips.teamDisbanded"))
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is TeamMySQLDao) {
            return false
        }
        return id == other.id
    }

    override fun hashCode(): Int {
        return Objects.hash(id)
    }

}