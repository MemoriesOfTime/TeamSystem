package cn.lanink.teamsystem.team.dao

import cn.nukkit.Player
import java.util.HashSet

interface Dao {

    var leaderName: String
    val players: HashSet<String>
    val applicationList: HashSet<String>

    val id: Int
    val maxPlayers: Int
    val name: String

    /**
     * 判断玩家是否是队长
     */
    fun isTeamLeader(other: Player): Boolean
    fun isTeamLeader(other: String): Boolean

    /**
     * 设置队伍队长
     */
    fun setTeamLeader(leader: Player)

    /**
     * 获取队伍队长
     */
    fun getTeamLeader(): Player?

    fun addPlayer(player: Player)
    fun addPlayer(playerName: String)

    fun removePlayer(player: Player)
    fun removePlayer(playerName: String)

    fun applyFrom(player: Player)
    fun applyFrom(playerName: String)

    fun cancelApplyFrom(player: Player)
    fun cancelApplyFrom(playerName: String)

    /**
     * 判断玩家是否在线（当前服务器）
     */
    fun isMemberOnline(playerName: String): Boolean

    /**
     * 判断队伍中的所有玩家是否在线（当前服务器）
     */
    fun isAllMemberOnline(): Boolean {
        return players.all { isMemberOnline(it) }
    }

    fun getMemberLoginAt(playerName: String): String

    fun disband()

}