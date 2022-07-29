package cn.lanink.teamsystem

import cn.lanink.gamecore.utils.Language
import cn.nukkit.utils.Logger
import io.netty.util.collection.IntObjectHashMap
import org.ktorm.database.Database

object SystemProvider {

    val Plugin: TeamSystem
        get() = TeamSystem.getInstance()
    val Database: Database?
        get() = TeamSystem.getInstance().database
    val Teams: IntObjectHashMap<Team>
        get() = TeamSystem.getInstance().teams
    val Language: Language
        get() = TeamSystem.getInstance().language
    val Logger: Logger
        get() = TeamSystem.getInstance().logger

}