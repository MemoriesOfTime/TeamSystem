package cn.lanink.teamsystem.dao

import org.ktorm.entity.Entity

interface OnlinePlayer : Entity<OnlinePlayer> {
    companion object : Entity.Factory<OnlinePlayer>()
    val id: Int
    var name: String
    var ofOnlineTeam: OnlineTeam?
}

interface OnlineTeam : Entity<OnlineTeam> {
    companion object : Entity.Factory<OnlineTeam>()
    var id: Int
    var teamName: String
    var maxPlayers: Int
    var teamLeader: OnlinePlayer
}

interface ApplyEntry : Entity<ApplyEntry> {
    companion object : Entity.Factory<ApplyEntry>()
    val id: Int
    var player: OnlinePlayer
    var onlineTeam: OnlineTeam
}