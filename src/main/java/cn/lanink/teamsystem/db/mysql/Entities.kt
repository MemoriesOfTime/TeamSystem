package cn.lanink.teamsystem.db.mysql

import org.ktorm.entity.Entity
import java.time.LocalDateTime

interface OnlinePlayer : Entity<OnlinePlayer> {
    companion object : Entity.Factory<OnlinePlayer>()
    val id: Int
    var name: String
    var ofOnlineTeam: OnlineTeam?
    var quitAt: LocalDateTime?
}

interface OnlineTeam : Entity<OnlineTeam> {
    companion object : Entity.Factory<OnlineTeam>()
    var id: Int
    var teamName: String
    var maxPlayers: Int
    var teamLeader: String
}

interface ApplyEntry : Entity<ApplyEntry> {
    companion object : Entity.Factory<ApplyEntry>()
    val id: Int
    var player: OnlinePlayer
    var onlineTeam: OnlineTeam
}