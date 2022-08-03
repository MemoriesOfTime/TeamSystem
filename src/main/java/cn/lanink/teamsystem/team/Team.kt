package cn.lanink.teamsystem.team

import cn.lanink.teamsystem.team.dao.Dao
import cn.lanink.teamsystem.team.dao.TeamDao
import cn.lanink.teamsystem.team.dao.TeamRedisDao
import cn.lanink.teamsystem.team.view.TeamForm
import cn.lanink.teamsystem.team.view.TeamInventory
import cn.lanink.teamsystem.team.view.View
import java.util.*

class Team(private val dao: Dao) : Dao by dao {

    // form 式 UI
    val formUI: TeamForm = TeamForm(this)
    // 背包式 UI
    val inveUI: TeamInventory = TeamInventory(this)

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is Team) {
            return false
        }
        return id == other.id
    }

    override fun hashCode(): Int {
        return Objects.hash(id)
    }
}