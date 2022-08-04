package cn.lanink.teamsystem.team.dao

import cn.lanink.teamsystem.TeamSystem

class TeamRedisDao(
    override val id: Int,
    override val name: String,
    override val maxPlayers: Int,
    leader: String
) : TeamDao(id, name, maxPlayers, leader) {

    private val database = TeamSystem.redisDb!!
    private val key: String = "team_sys:$id"
    private val playersKey: String = "$key:players"
    private val appliesKey: String = "$key:applies"
    companion object {
        val quitRootKey: String = "team_sys:players:quit:"
    }

    override var leaderName: String = leader
        get() {
            database.resource.use {
                field = it.hget(key, "leaderName")
            }
            return field
        }
        set(value) {
            database.resource.use {
                it.hset(key, "leaderName", value)
            }
            field = value
        }

    override val players: HashSet<String> = HashSet()
        get() {
            database.resource.use {
                it.smembers(playersKey)?.apply {
                    field.clear()
                }?.forEach { member ->
                    field.add(member)
                }
            }
            return field
        }

    override val applicationList: HashSet<String> = HashSet()
        get() {
            database.resource.use {
                it.smembers(appliesKey)?.apply {
                    field.clear()
                }?.forEach { member ->
                    field.add(member)
                }
            }
            return field
        }

    init {
        leaderName = leader
        addPlayer(leader)
        database.resource.use {
            it.hset(key, "teamName", name)
            it.hset(key, "maxPlayers", maxPlayers.toString())
        }
    }

    override fun addPlayer(playerName: String) {
        database.resource.use {
            it.sadd(playersKey, playerName)
        }
    }

    override fun removePlayer(playerName: String) {
        database.resource.use {
            it.srem(playersKey, playerName)
        }
    }

    override fun applyFrom(playerName: String) {
        database.resource.use {
            it.sadd(appliesKey, playerName)
        }
    }

    override fun cancelApplyFrom(playerName: String) {
        database.resource.use {
            it.srem(appliesKey, playerName)
        }
    }

    override fun isOnline(playerName: String): Boolean {
        if (super.isOnline(playerName)) {
            return true
        }
        return database.resource.use {
            !it.exists("$quitRootKey$playerName")
        }
    }

    override fun disband() {
        database.resource.use {
            it.del(key)
            it.del(playersKey)
            it.del(appliesKey)
        }
    }
}