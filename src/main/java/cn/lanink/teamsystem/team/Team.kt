package cn.lanink.teamsystem.team

import cn.lanink.teamsystem.team.dao.Dao

class Team(val dao: Dao) : Dao by dao{

}