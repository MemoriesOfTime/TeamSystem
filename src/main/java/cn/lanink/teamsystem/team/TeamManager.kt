package cn.lanink.teamsystem.team

import cn.lanink.formdsl.dsl.*
import cn.lanink.gamecore.form.element.ResponseElementButton
import cn.lanink.teamsystem.TeamSystem
import cn.lanink.teamsystem.TeamSystem.Companion.language
import cn.lanink.teamsystem.db.mysql.OnlineTeam
import cn.lanink.teamsystem.db.mysql.teams
import cn.lanink.teamsystem.team.dao.Dao
import cn.lanink.teamsystem.team.dao.TeamLocalDao
import cn.lanink.teamsystem.team.dao.TeamMySQLDao
import cn.lanink.teamsystem.team.dao.TeamRedisDao
import cn.nukkit.Player
import cn.nukkit.Server
import cn.nukkit.form.response.FormResponseData
import cn.nukkit.form.window.FormWindow
import io.netty.util.collection.IntObjectHashMap
import org.ktorm.database.Database
import org.ktorm.entity.add
import org.ktorm.entity.forEach
import redis.clients.jedis.JedisPool
import java.util.*
import kotlin.math.min

object TeamManager {
    private val databaseMysql: Database? = TeamSystem.mysqlDb
    private val databaseRedis: JedisPool? = TeamSystem.redisDb

    // 所有队伍列表，有远程时获取会自动更新
    val teams = IntObjectHashMap<Team>()
        get() {
            databaseMysql?.apply {
                field.clear()
            }?.teams?.forEach {
                field[it.id] = Team(TeamMySQLDao(
                    it.id,
                    it.teamName,
                    it.maxPlayers,
                    it.teamLeader,
                ))
            }
            databaseRedis?.apply {
                field.clear()
            }?.resource?.use {
                it.smembers("team_sys:ids")
                    .map(Integer::parseInt).forEach { id ->
                    field[id] = Team(TeamRedisDao(
                        id,
                        it.hget("team_sys:$id", "teamName"),
                        it.hget("team_sys:$id", "maxPlayers").toInt(),
                        it.hget("team_sys:$id", "leaderName")
                    ))
                }
            }
            return field
        }


    fun createTeam(teamId: Int, name: String, maxPlayersNum: Int, leader: Player) : Team {
        databaseMysql?.teams?.add(OnlineTeam{
            id = teamId
            teamName = name
            maxPlayers = maxPlayersNum
        })
        databaseRedis?.resource?.use {
            it.sadd("team_sys:ids", teamId.toString())
        }

        val dao: Dao = if (databaseMysql == null && databaseRedis == null) {
            TeamLocalDao(teamId, name, maxPlayersNum, leader.name).apply {
                teams[teamId] = Team(this)
            }
        } else if (databaseMysql == null){
            TeamRedisDao(teamId, name, maxPlayersNum, leader.name)
        } else {
            TeamMySQLDao(teamId, name, maxPlayersNum, leader.name)
        }
        return Team(dao)
    }

    fun disbandTeam(team: Team) {
        team.disband()
        databaseRedis?.resource?.use {
            it.srem("team_sys:ids", team.id.toString())
        }
        teams.remove(team.id)
    }

    fun showJoinTeam(player: Player) {
        FormSimple {
            target = player
            title = language.translateString("form.join.title")
            Button {
                text = language.translateString("form.join.button.searchTeam")
                onPlayerClick {
                    showFindTeam(player)
                }
            }
            Button {
                text = language.translateString("form.join.button.teamsList")
                onPlayerClick {
                    showTeamsForm(player, teams.values.toList().chunked(10))
                }
            }
            onClose {
                TeamSystem.showMainForm(player)
            }
        }
    }

    class FindResp : FormCustomResponseModel {
        lateinit var findOption: FormResponseData
        lateinit var searchText: String
    }

    fun showFindTeam(player: Player) {
        FormCustom<FindResp> {
            title = language.translateString("form.search.title")
            target = player
            Dropdown(FindResp::findOption) {
                text = language.translateString("form.search.dropdown.text")
                option {
                    +language.translateString("general.teamID")
                    -language.translateString("general.teamName")
                    -language.translateString("form.search.dropdown.teamMemberName")
                }
            }
            Input(FindResp::searchText) {
                text = language.translateString("form.search.input.title")
                placeHolder = language.translateString("form.search.input.placeHolder")
            }
            onElementRespond {
                if (searchText == "") {
                    player.sendMessage(language.translateString("form.search.emptyParameterTip"))
                    return@onElementRespond
                }
                when (findOption.elementID) {
                    0 -> {
                        try {
                            val id = searchText.toInt()
                            val team = teams[id]
                            team ?: showFail(
                                player,
                                this@FormCustom,
                                language.translateString("form.error.search.notFoundByID.content", id),
                                language.translateString("form.error.search.fail.title")
                            )
                            team?.formUI?.showTeamInfo(player)
                        } catch (_: Exception) {
                            showFail(
                                player,
                                this@FormCustom,
                                language.translateString("form.error.search.formatError.content"),
                                language.translateString("general.error")
                            )
                        }
                    }
                    1 -> {
                        for ((_, team) in teams) {
                            if (team.name.lowercase() == searchText.lowercase()) {
                                team.formUI.showTeamInfo(player)
                                return@onElementRespond
                            }
                        }
                        showFail(
                            player,
                            this@FormCustom,
                            language.translateString("form.error.search.notFoundByName.content", searchText),
                            language.translateString("form.error.search.fail.title")
                        )
                    }
                    else -> {
                        val team = TeamSystem.getTeamByPlayer(searchText)
                        team ?: showFail(
                            player,
                            this@FormCustom,
                            language.translateString("form.error.search.playerHasNoTeam", searchText),
                            language.translateString("form.error.search.fail.title")
                        )
                        team?.formUI?.showTeamInfo(player)
                    }
                }
            }
        }
    }

    fun showTeamsForm(player: Player, pages: List<List<Team>>) {
        fun page(teams: List<Team>, receiver: AdvancedFormWindowSimpleAdapter) {
            teams.forEach { team ->
                receiver.apply {
                    title = language.translateString("form.list.title")
                    Button {
                        text = "ID: ${team.id}\n${language.translateString("general.teamName")}: ${team.name}"
                        onPlayerClick {
                            team.formUI.showTeamInfo(player)
                        }
                    }
                }
            }
        }
        val formPages = mutableListOf<AdvancedFormWindowSimpleAdapter>()
        formPages.apply {
            if (pages.isEmpty()) {
                add(FormSimple {
                    content = language.translateString("form.list.emptyContent")+"\n\n"
                    Button { // back
                        text = language.translateString("general.return")
                        onPlayerClick {
                            showJoinTeam(player)
                        }
                    }
                })
            }
            pages.take(1).forEach {
                add(FormSimple {
                    page(it, this)
                    Button { // 下一页
                        text = language.translateString("general.page.next")
                    }
                })
            }
            pages.drop(1).dropLast(1).forEach { // 拿掉第一个和最后一个
                val before = formPages.last()
                val now = FormSimple {
                    Button { // 上一页
                        text = language.translateString("general.page.back")
                        onPlayerClick {
                            before.showToPlayer(player)
                        }
                    }
                    page(it, this)
                    Button { // 下一页
                        text = language.translateString("general.page.next")
                    }
                }
                (before.buttons.last() as ResponseElementButton).onPlayerClick {
                    now.showToPlayer(player)
                }
                add(now)
            }
            pages.takeLast(1).forEach {
                val before = formPages.last()
                add(FormSimple {
                    Button { // 上一页
                        text = language.translateString("general.return")
                        onPlayerClick {
                            before.showToPlayer(player)
                        }
                    }
                    page(it, this)
                })
            }
        }
        formPages.first().showToPlayer(player)
    }

    fun showFail(player: Player, parent: FormWindow, detail: String, tip: String) {
        FormSimple {
            title = tip
            content = "$detail\n\n"
            target = player
            Button {
                text = language.translateString("general.return")
                onPlayerClick {
                    showFormWindow(parent)
                }
            }
        }
    }

    class CreateResp : FormCustomResponseModel {
        lateinit var teamName: String
        lateinit var teamSize: FormResponseData
    }

    private val random = Random()
    fun showCreateForm(player: Player) {
        var id: Int
        do {
            id = random.nextInt(99999.coerceAtMost(Server.getInstance().maxPlayers * 3))
        } while (teams.contains(id))
        FormCustom<CreateResp> {
            title = language.translateString("form.create.title")
            target = player
            Label("${language.translateString("general.teamID")}: $id")
            Input(CreateResp::teamName) {
                text = language.translateString("general.teamName")
                placeHolder = language.translateString("general.teamName")
                default = "$id"
            }
            Dropdown(CreateResp::teamSize) {
                text = language.translateString("general.teamSize")
                option {
                    +"2"
                    -"3"
                    -"4"
                    -"5"
                }
            }
            onElementRespond {
                createTeam(
                    id,
                    teamName,
                    teamSize.elementContent.toInt(),
                    player
                ).formUI.showTeamInfo(player)
            }
            onClose {
                TeamSystem.showMainForm(player)
            }
        }
    }
}