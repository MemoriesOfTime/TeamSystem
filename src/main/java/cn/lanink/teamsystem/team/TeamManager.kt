package cn.lanink.teamsystem.team

import cn.lanink.formdsl.dsl.*
import cn.lanink.teamsystem.TeamSystem
import cn.lanink.teamsystem.db.mysql.OnlineTeam
import cn.lanink.teamsystem.db.mysql.teams
import cn.lanink.teamsystem.team.dao.Dao
import cn.lanink.teamsystem.team.dao.TeamLocalDao
import cn.lanink.teamsystem.team.dao.TeamMySQLDao
import cn.lanink.teamsystem.team.dao.TeamRedisDao
import cn.nukkit.Player
import cn.nukkit.form.response.FormResponseData
import io.netty.util.collection.IntObjectHashMap
import org.ktorm.database.Database
import org.ktorm.entity.add
import org.ktorm.entity.forEach
import redis.clients.jedis.JedisPool

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

        dao.addPlayer(leader)
        dao.setTeamLeader(leader)
        return Team(dao)
    }

    fun disbandTeam(team: Team) {
        team.disband()
        teams.remove(team.id)
    }

    fun showJoinTeam(player: Player) {
        FormSimple {
            target = player
            title = TeamSystem.language.translateString("form.join.title")
            Button {
                text = TeamSystem.language.translateString("form.join.button.searchTeam")
                onPlayerClick {
                    showFindTeam(player)
                }
            }
            Button {
                text = TeamSystem.language.translateString("form.join.button.teamsList")
                onPlayerClick {
                    showTeamsForm(player)
                }
            }
            onClose {
                TeamSystem.showMainForm(player)
            }
        }
    }

    fun showFindTeam(player: Player) {

    }

    fun showTeamsForm(player: Player) {
        FormSimple {
            title = TeamSystem.language.translateString("form.list.title")
            target = player
            teams.forEach {
                Button {
                    text = it.value.name
                    onPlayerClick {
                        it.value.formUI.showTeamInfo(player)
                    }
                }
            }
            onClose {
                showJoinTeam(player)
            }
        }
    }

    class CreateResp : FormCustomResponseModel {
        lateinit var input: String
        lateinit var dropdown: FormResponseData

    }

    fun showCreateForm(player: Player) {
        FormCustom<CreateResp> {

        }
    }

//    fun showTeamsInve(player: Player) {
//
//    }
}