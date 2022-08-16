package cn.lanink.teamsystem.team.view

import cn.nukkit.Player

interface View {
    fun showTeamInfo(player: Player)
    fun showQuitConfirm(player: Player)
    fun showTeamLeaderTransfer(player: Player)
    fun showTeamApplicationList(player: Player)
    fun showTeamTeleport(player: Player)

}