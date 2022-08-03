package cn.lanink.teamsystem.team.dao

import cn.lanink.teamsystem.TeamSystem
import cn.lanink.teamsystem.db.mysql.*
import cn.nukkit.Player
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
class TeamMySQLDao(
    override val id: Int,
    override val name: String,
    override val maxPlayers: Int,
    leader: String
) : TeamDao(id, name, maxPlayers, leader) {

    private val database: Database = TeamSystem.mysqlDb!!

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

    init {
        leaderName = leader
        addPlayer(leader)
    }

    override fun addPlayer(playerName: String) {
        database.update(OnlinePlayers) {
            set(it.ofTeam, this@TeamMySQLDao.id)
            where {
                it.playerName eq playerName
            }
        }
    }

    override fun removePlayer(name: String) {
        database.update(OnlinePlayers) {
            set(it.ofTeam, null)
            where {
                it.playerName eq name
            }
        }
    }

    override fun applyFrom(playerName: String) {
        database.insert(ApplyList) { col ->
            set(col.player, database.onlinePlayers.find { it.playerName eq playerName }!!.id)
            set(col.team, this@TeamMySQLDao.id)
        }
    }

    override fun cancelApplyFrom(playerName: String) {
        database.applies.removeIf { col ->
            (col.team eq this.id) and (col.player eq database.onlinePlayers.find { it.playerName eq playerName }!!.id)
        }
    }

    override fun isOnline(playerName: String): Boolean {
        if (super.isOnline(playerName)) {
            return true
        }
        return database.onlinePlayers.find {
            it.playerName eq playerName
        }?.quitAt == null
    }

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
        super.disband()
    }

}