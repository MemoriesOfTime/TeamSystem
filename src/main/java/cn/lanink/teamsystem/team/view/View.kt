package cn.lanink.teamsystem.team.view

import cn.nukkit.Player

interface View {
    fun showTeamInfo(player: Player)
    fun showQuitConfirm(player: Player)
}