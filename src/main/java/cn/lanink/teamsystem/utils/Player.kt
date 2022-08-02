package cn.lanink.teamsystem.utils

import cn.lanink.teamsystem.TeamSystem
import cn.lanink.teamsystem.team.dao.TeamDao
import cn.lanink.teamsystem.team.dao.TeamMySQLDao
import cn.nukkit.Player

// 一些增强的方法，为 kt 开发准备
fun Player.applyFor(team: TeamMySQLDao) {
    team.applyFrom(this)
}

fun Player.cancelApplyFor(team: TeamMySQLDao) {
    team.cancelApplyFrom(this)
}

fun Player.join(team: TeamMySQLDao) {
    team.addPlayer(this)
}

fun Player.quit(team: TeamMySQLDao) {
    if (team.isTeamLeader(player)) {
        team.disband()
    } else {
        team.removePlayer(player)
    }
}

fun Player.getTeam(): TeamDao? {
    for (team in TeamSystem.teams.values) {
        if (team.players.contains(player.name)) {
            return team
        }
    }
    return null
}