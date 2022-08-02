package cn.lanink.teamsystem.team.dao

import cn.nukkit.Player
import java.util.HashSet

abstract class TeamDao : Dao {

    override var leaderName: String
        get() = TODO("Not yet implemented")
        set(value) {}
    override val players: HashSet<String>
        get() = TODO("Not yet implemented")
    override val applicationList: HashSet<String>
        get() = TODO("Not yet implemented")

    override fun isTeamLeader(other: Player): Boolean {
        TODO("Not yet implemented")
    }

    override fun isTeamLeader(other: String): Boolean {
        TODO("Not yet implemented")
    }

    override fun setTeamLeader(leader: Player) {
        TODO("Not yet implemented")
    }

    override fun getTeamLeader(): Player? {
        TODO("Not yet implemented")
    }

    override fun addPlayer(player: Player) {
        TODO("Not yet implemented")
    }

    override fun addPlayer(playerName: String) {
        TODO("Not yet implemented")
    }

    override fun removePlayer(player: Player) {
        TODO("Not yet implemented")
    }

    override fun removePlayer(name: String) {
        TODO("Not yet implemented")
    }

    override fun applyFrom(player: Player) {
        TODO("Not yet implemented")
    }

    override fun applyFrom(playerName: String) {
        TODO("Not yet implemented")
    }

    override fun cancelApplyFrom(player: Player) {
        TODO("Not yet implemented")
    }

    override fun cancelApplyFrom(playerName: String) {
        TODO("Not yet implemented")
    }

    override fun isOnline(playerName: String): Boolean {
        TODO("Not yet implemented")
    }

    override fun disband() {
        TODO("Not yet implemented")
    }
}

class TeamLocalDao() : TeamDao()