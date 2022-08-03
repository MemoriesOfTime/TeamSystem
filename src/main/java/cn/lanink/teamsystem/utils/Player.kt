package cn.lanink.teamsystem.utils

import cn.lanink.teamsystem.TeamSystem
import cn.lanink.teamsystem.team.Team
import cn.lanink.teamsystem.team.TeamManager
import cn.lanink.teamsystem.team.dao.TeamDao
import cn.lanink.teamsystem.team.dao.TeamMySQLDao
import cn.nukkit.Player

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
        TeamManager.disbandTeam(team)
    } else {
        team.removePlayer(player)
    }
}

fun Player.getTeam(): Team? {
    for (team in TeamSystem.teams.values) {
        if (team.players.contains(player.name)) {
            return team
        }
    }
    return null
}