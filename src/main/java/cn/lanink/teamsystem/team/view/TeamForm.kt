package cn.lanink.teamsystem.team.view

import cn.lanink.formdsl.dsl.*
import cn.lanink.teamsystem.TeamSystem
import cn.lanink.teamsystem.TeamSystem.Companion.language
import cn.lanink.teamsystem.team.Team
import cn.nukkit.Player

class TeamForm(private val team: Team) : View {
    override fun showTeamInfo(player: Player) {
        val leaderName = team.leaderName
        val members = team.players
        val maxSize = team.maxPlayers
        FormSimple {
            title = language.translateString("form.info.title")
            target = player
            content = """
                ${language.translateString("general.teamID")}: ${team.id}
                ${language.translateString("general.teamName")}: ${team.name}
                ${language.translateString("general.teamSize")}: $maxSize
                ${language.translateString("general.leader")}: ${
                if (team.isOnline(leaderName)) {
                    language.translateString("general.teamPlayerNameOnline", leaderName)
                } else {
                    language.translateString("general.teamPlayerNameOffline", leaderName)
                }
            }
                ${language.translateString("general.teammates")}: ${
                if (members.isEmpty()) {
                    language.translateString("general.empty")
                } else ""
            }
                ${
                if (members.size > 1) members.filterNot {
                    it == leaderName
                }.map {
                    if (team.isOnline(it)) {
                        language.translateString("general.teamPlayerNameOnline", it)
                    } else {
                        language.translateString("general.teamPlayerNameOffline", it)
                    }
                }.reduce { a, b ->
                    "$a\n$b"
                } else ""
            }
            """.trimIndent()
            if (members.contains(player.name)) {
                if (leaderName == player.name) {
                    Button {
                        text = language.translateString("form.info.button.transfer")
                        onPlayerClick {
                            showTeamLeaderTransfer(player)
                        }
                    }
                    Button {
                        text = language.translateString("form.info.button.checkApplications")
                        onPlayerClick {
                            showTeamApplicationList(player)
                        }
                    }
                }
                Button {
                    text = language.translateString("form.info.button.teleport")
                    onPlayerClick {
                        showTeamTeleport(player)
                    }
                }
            } else if (members.size < maxSize) {
                Button {
                    text = language.translateString("form.info.button.sendRequest")
                    onPlayerClick {
                        team.applyFrom(player)
                        FormModal {
                            target = player
                            title = language.translateString("tips.requestApproved")
                            content = language.translateString("form.info.sendApplicationSuccess", team.name)
                            trueText = language.translateString("general.confirm")
                            falseText = language.translateString("general.return")
                        }
                    }
                }
            } else {
                Button {
                    text = language.translateString("tips.teamFull")
                }
            }
            Button {
                text = language.translateString("general.return")
                onPlayerClick {
                    TeamSystem.showMainForm(player)
                }
            }
        }
    }

    override fun showQuitConfirm(player: Player) {
        FormModal {
            target = player
            title = language.translateString("form.quit.title")
            content = if (team.leaderName == player.name) {
                language.translateString("form.quit.ownerDescription") + "\n" + language.translateString(
                    "form.quit.description", team.name
                )
            } else {
                language.translateString("form.quit.description", team.name)
            }
            trueText = language.translateString("general.confirm")
            falseText = language.translateString("general.return")
            onTrue {
                TeamSystem.quitTeam(player)
            }
            onFalse {
                TeamSystem.showMainForm(player)
            }
            onClose {
                TeamSystem.showMainForm(player)
            }
        }
    }

    override fun showTeamLeaderTransfer(player: Player) {
        val members = team.players
        FormSimple {
            target = player
            title = language.translateString("form.transfer.title")
            content = if (members.size > 1) {
                language.translateString("form.transfer.content.description")
            } else {
                language.translateString("form.transfer.content.noPerson") + "\n\n"
            }
            members.filterNot {
                it == team.leaderName
            }.forEach {
                Button {
                    text = it
                    onPlayerClick {
                        team.leaderName = it
                        FormSimple {
                            target = player
                            title = language.translateString("form.transfer.success.title")
                            content = language.translateString("form.transfer.success.content", it) + "\n\n"
                            Button {
                                text = language.translateString("general.return")
                                onPlayerClick {
                                    showTeamInfo(player)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun showTeamApplicationList(player: Player) {
        val applies = team.applicationList
        FormSimple {
            target = player
            title = language.translateString("form.application.title")
            content = if (applies.isEmpty()) {
                language.translateString("form.application.empty") + "\n\n"
            } else ""
            applies.forEach {
                Button {
                    text = it
                    onPlayerClick {
                        FormModal {
                            target = player
                            title = language.translateString("form.application.handle.title")
                            content = language.translateString("form.application.handle.content", it)
                            trueText = language.translateString("general.approve")
                            falseText = language.translateString("general.refuse")
                            onTrue {
                                // 防止一些并发请求问题
                                if (team.players.size >= team.maxPlayers) {
                                    player.sendMessage(language.translateString("tips.teamFull"))
                                    return@onTrue
                                }
                                team.cancelApplyFrom(it)
                                team.addPlayer(it)
                                showTeamApplicationList(player)
                            }
                            onFalse {
                                team.cancelApplyFrom(it)
                                showTeamApplicationList(player)
                            }
                        }
                    }
                }
            }
            Button {
                text = language.translateString("general.return")
                onPlayerClick {
                    showTeamInfo(player)
                }
            }
        }
    }

    override fun showTeamTeleport(player: Player) {
        FormSimple {
            target = player
            title = language.translateString("form.teleport.select.title")
            team.players.filterNot {
                it == player.name
            }.forEach {
                Button {
                    text = it
                    onPlayerClick {
                        sendMessage(language.translateString("tips.sendTeleportRequest"))
                        // TODO 广播 + 跨服传送
                    }
                }
            }
        }
    }
}