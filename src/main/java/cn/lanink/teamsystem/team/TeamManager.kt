package cn.lanink.teamsystem.team

import cn.lanink.teamsystem.TeamSystem
import cn.lanink.teamsystem.db.mysql.OnlineTeam
import cn.lanink.teamsystem.db.mysql.teams
import cn.lanink.teamsystem.team.dao.TeamDao
import cn.lanink.teamsystem.team.dao.TeamMySQLDao
import cn.nukkit.Player
import io.netty.util.collection.IntObjectHashMap
import org.ktorm.database.Database
import org.ktorm.entity.add
import org.ktorm.entity.forEach

object TeamManager {
    private val database: Database? = TeamSystem.database

    // 所有队伍列表，获取时会自动更新，没有远程数据库时和 localTeams 等效
    val teams = IntObjectHashMap<TeamDao>()
        get() {
            database?.apply {
                field.clear()
            }?.teams?.forEach {
                field.put(it.id, TeamMySQLDao(
                        it.id,
                        it.teamName,
                        it.maxPlayers,
                        it.teamLeader,
                    )
                )
            }
            return field
        }


    fun createTeam(teamId: Int, name: String, maxPlayersNum: Int, leader: Player) : TeamMySQLDao {
        val team = TeamMySQLDao(teamId, name, maxPlayersNum, leader)
        database?.teams?.add(OnlineTeam{
            id = teamId
            teamName = name
            maxPlayers = maxPlayersNum
            teamLeader = leader.name
        })
        database ?: teams.put(teamId, team)
        team.addPlayer(leader)
        return team
    }
}