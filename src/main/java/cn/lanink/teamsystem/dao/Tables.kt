package cn.lanink.teamsystem.dao

import org.ktorm.database.Database
import org.ktorm.entity.sequenceOf
import org.ktorm.schema.Table
import org.ktorm.schema.int
import org.ktorm.schema.varchar

val Database.teams get() = this.sequenceOf(Teams)
val Database.onlinePlayers get() = this.sequenceOf(OnlinePlayers)
val Database.applies get() = this.sequenceOf(ApplyList)

object Teams : Table<OnlineTeam>("t_team_system") {
    val id = int("id").primaryKey().bindTo { it.id }
    val teamName = varchar("team_name").bindTo { it.teamName }
    val maxPlayers = int("max_players").bindTo { it.maxPlayers }
    val teamLeader = int("team_leader").references(OnlinePlayers) { it.teamLeader }
}

object OnlinePlayers : Table<OnlinePlayer>("t_online_players") {
    val id = int("id").primaryKey().bindTo { it.id }
    val playerName = varchar("player_name").bindTo { it.name }
    val ofTeam = int("of_team").references(Teams) { it.ofOnlineTeam }
}

object ApplyList : Table<ApplyEntry>("t_applies") {
    val id = int("id").primaryKey().bindTo { it.id }
    val player = int("player_id").references(OnlinePlayers) { it.player }
    val team = int("team_id").references(Teams) { it.onlineTeam }
}