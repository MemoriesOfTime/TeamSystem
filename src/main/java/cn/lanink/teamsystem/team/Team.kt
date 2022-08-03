package cn.lanink.teamsystem.team

import cn.lanink.teamsystem.team.dao.Dao
import cn.lanink.teamsystem.team.dao.TeamDao
import cn.lanink.teamsystem.team.dao.TeamRedisDao
import java.util.*

class Team(private val dao: TeamDao) : Dao by dao{

    val id: Int
        get() = dao.id

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is TeamRedisDao) {
            return false
        }
        return id == other.id
    }

    override fun hashCode(): Int {
        return Objects.hash(id)
    }
}