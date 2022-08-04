package cn.lanink.teamsystem.team.dao

import cn.nukkit.Player
import cn.nukkit.Server

abstract class TeamDao(
    override val id: Int,
    override val name: String,
    override val maxPlayers: Int,
    leader: String
) : Dao {

    override var leaderName: String = leader
    override val players: HashSet<String> = HashSet<String>().apply { add(leader) }
    override val applicationList: HashSet<String> = HashSet()

    override fun isTeamLeader(other: Player): Boolean {
        return leaderName == other.name
    }

    override fun isTeamLeader(other: String): Boolean {
        return leaderName == other
    }

    override fun setTeamLeader(leader: Player) {
        leaderName = leader.name
    }

    /**
     * 多服下可能返回 null
     */
    override fun getTeamLeader(): Player? {
        return Server.getInstance().getPlayer(leaderName)
    }

    override fun addPlayer(player: Player) {
        addPlayer(player.name)
    }

    override fun addPlayer(playerName: String) {
        players.add(playerName)
    }

    override fun removePlayer(player: Player) {
        removePlayer(player.name)
    }

    override fun removePlayer(playerName: String) {
        players.remove(playerName)
    }

    override fun applyFrom(player: Player) {
        applyFrom(player.name)
    }

    override fun applyFrom(playerName: String) {
        applicationList.add(playerName)
    }

    override fun cancelApplyFrom(player: Player) {
        cancelApplyFrom(player.name)
    }

    override fun cancelApplyFrom(playerName: String) {
        applicationList.remove(playerName)
    }

    override fun isOnline(playerName: String): Boolean {
        return (Server.getInstance().getPlayer(playerName)?.isOnline == true)
    }

    override fun disband() {
        applicationList.clear()
        players.clear()
    }
}

class TeamLocalDao(
    override val id: Int,
    override val name: String,
    override val maxPlayers: Int,
    leader: String
) : TeamDao(id, name, maxPlayers, leader)