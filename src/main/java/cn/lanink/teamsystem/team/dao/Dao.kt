package cn.lanink.teamsystem.team.dao

import cn.nukkit.Player
import java.util.HashSet

interface Dao {

    var leaderName: String
    val players: HashSet<String>
    val applicationList: HashSet<String>

    fun isTeamLeader(other: Player): Boolean
    fun isTeamLeader(other: String): Boolean

    fun setTeamLeader(leader: Player)
    fun getTeamLeader(): Player?

    fun addPlayer(player: Player)
    fun addPlayer(playerName: String)

    fun removePlayer(player: Player)
    fun removePlayer(name: String)

    fun applyFrom(player: Player)
    fun applyFrom(playerName: String)

    fun cancelApplyFrom(player: Player)
    fun cancelApplyFrom(playerName: String)

    fun isOnline(playerName: String): Boolean

    fun disband()

}