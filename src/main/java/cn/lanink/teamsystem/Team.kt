package cn.lanink.teamsystem

import cn.lanink.teamsystem.dao.*
import cn.nukkit.Player
import cn.nukkit.Server
import io.netty.util.collection.IntObjectHashMap
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
class Team(val id: Int, val name: String, val maxPlayers: Int, leader: String) {

    // 获取时会触发更新，如果没有远程数据库则不更新 TODO Cache

    constructor(id: Int, name: String, maxPlayers: Int, leader: Player) : this(
        id, name, maxPlayers, leader.name
    )

    var leaderName: String = leader
        get() {
            database?.teams?.find {
                it.id eq this.id
            }?.teamLeader?.let {
                field = it
            }
            return field
        }

    // players 里面可能有这个服务器没上线的玩家(在其他服务器是上线的)，如果只有一个服务器的话，那么不会有这种情况
    // 注意，请不要使用 getPlayers().add("xxx") 的方式添加
    val players = HashSet<String>()
        get() {
            database?.apply {
                field.clear()
            }?.onlinePlayers?.filter {
                it.ofTeam eq this.id
            }?.map {
                it.name
            }?.forEach {
                field.add(it)
            }
            return field
        }

    // 申请加入列表, 可能有这个服务器没上线的玩家(在其他服务器是上线的)，如果只有一个服务器的话，那么不会有这种情况
    // 注意，请不要使用 getApplicationList().add("xxx") 的方式添加
    val applicationList = HashSet<String>()
        get() {
            database?.apply {
                field.clear()
            }?.applies?.filter {
                it.team eq this.id
            }?.map {
                it.player.name
            }?.forEach {
                field.add(it)
            }
            return field
        }

    internal fun isTeamLeader(other: Player): Boolean {
        return this.leaderName == other.name
    }

    fun isTeamLeader(other: String): Boolean {
        return this.leaderName == other
    }

    internal fun setTeamLeader(leader: Player) {
        setTeamLeader(leader.name)
    }

    fun setTeamLeader(leader: String) {
        database?.teams?.update(OnlineTeam{
            id = this@Team.id
            teamLeader = leader
        })
        this.leaderName = leader
    }

    /**
     * 多服下可能返回 null
     */
    fun getTeamLeader(): Player? {
        return Server.getInstance().getPlayer(this.leaderName)
    }

    /**
     * 添加玩家
     */
    fun addPlayer(player: Player) {
        addPlayer(player.name)
    }

    /**
     * 多服下请使用这个方法
     */
    fun addPlayer(playerName: String) {
        database?.update(OnlinePlayers) {
            set(it.ofTeam, this@Team.id)
            where {
                it.playerName eq playerName
            }
        }
        database?:players.add(playerName) // 没有远程数据库就更新本地缓存
    }

    /**
     * 移除玩家
     */
    internal fun removePlayer(player: Player) {
        removePlayer(player.name)
    }

    fun removePlayer(name: String) {
        database?.update(OnlinePlayers) {
            set(it.ofTeam, null)
            where {
                it.playerName eq name
            }
        }
        database?:players.remove(name)
    }

    /**
     * 申请加入队伍，仅适用于 kt 开发
     */
    internal fun applyFrom(p: Player) {
        applyFrom(p.name)
    }

    fun applyFrom(playerName: String) {
        database?.insert(ApplyList) { col ->
            set(col.player, database.onlinePlayers.find { it.playerName eq playerName }!!.id)
            set(col.team, this@Team.id)
        }
        database?:applicationList.add(playerName)
    }

    /**
     * 取消申请加入队伍
     */
    internal fun cancelApplyFrom(p: Player) {
        cancelApplyFrom(p.name)
    }

    fun cancelApplyFrom(playerName: String) {
        database?.applies?.removeIf { col ->
            (col.team eq this.id) and (col.player eq database.onlinePlayers.find { it.playerName eq playerName }!!.id)
        }
        database?:applicationList.remove(playerName)
    }

    /**
     * 适用所有情况（多服，单服）
     */
    fun isOnline(playerName: String): Boolean {
        if (database == null)
            return Server.getInstance().getPlayer(playerName) != null
        return database.onlinePlayers.find {
            it.playerName eq playerName
        }?.quitAt == null
    }

    /**
     * 解散队伍
     */
    fun disband() {
        database?.update(OnlinePlayers) {
            set(it.ofTeam, null)
            where {
                it.ofTeam eq this@Team.id
            }
        }
        database?.applies?.removeIf {
            it.team eq this.id
        }
        database?.teams?.removeIf {
            it.id eq this.id
        }

        for (playerName in this.players) {
            val player = Server.getInstance().getPlayer(playerName)
            if (player != null && player.isOnline) { // 检查是否是本服玩家
                player.sendMessage(TeamSystem.getInstance().language.translateString("tips.teamDisbanded"))
            }
        }
        database?:players.clear()
        database?:applicationList.clear()
        database?:teams.remove(this.id)
        localTeams.remove(this.id)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is Team) {
            return false
        }
        return id == other.id
    }

    override fun hashCode(): Int {
        return Objects.hash(id)
    }

    companion object TeamManager {
        private val database: Database? = SystemProvider.Database

        // 所有队伍列表，获取时会自动更新，没有远程数据库时和 localTeams 等效
        val teams = IntObjectHashMap<Team>()
            get() {
                database?.apply {
                    field.clear()
                }?.teams?.forEach {
                    field.put(it.id, Team(
                            it.id,
                            it.teamName,
                            it.maxPlayers,
                            it.teamLeader,
                        )
                    )
                }
                return field
            }

        // 本地队伍列表，仅仅只用于缓存
        val localTeams = IntObjectHashMap<Team>()

        fun createTeam(teamId: Int, name: String, maxPlayersNum: Int, leader: Player) : Team {
            val team = Team(teamId, name, maxPlayersNum, leader)
            database?.teams?.add(OnlineTeam{
                id = teamId
                teamName = name
                maxPlayers = maxPlayersNum
                teamLeader = leader.name
            })
            localTeams.put(teamId, team)
            database?:teams.put(teamId, team)
            team.addPlayer(leader)
            return team
        }
    }
}
// 一些增强的方法，为 kt 开发准备
fun Player.applyFor(team: Team) {
    team.applyFrom(this)
}

fun Player.cancelApplyFor(team: Team) {
    team.cancelApplyFrom(this)
}

fun Player.join(team: Team) {
    team.addPlayer(this)
}

fun Player.quit(team: Team) {
    if (team.isTeamLeader(player)) {
        SystemProvider.Teams.remove(team.id)
        team.disband()
    } else {
        team.removePlayer(player)
    }
}

fun Player.getTeam(): Team? {
    for (team in SystemProvider.Teams.values) {
        if (team.players.contains(player.name)) {
            return team
        }
    }
    return null
}