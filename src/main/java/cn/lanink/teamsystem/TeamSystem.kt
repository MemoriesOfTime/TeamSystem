package cn.lanink.teamsystem

import cn.lanink.gamecore.utils.Language
import cn.lanink.teamsystem.db.Db.checkInit
import cn.lanink.teamsystem.db.Db.connect
import cn.lanink.teamsystem.db.Db.initDatabase
import cn.lanink.teamsystem.team.dao.TeamMySQLDao
import cn.lanink.teamsystem.team.TeamManager
import cn.lanink.teamsystem.team.dao.TeamDao
import cn.lanink.teamsystem.utils.FormHelper
import cn.nukkit.Player
import cn.nukkit.command.Command
import cn.nukkit.command.CommandSender
import cn.nukkit.plugin.PluginBase
import cn.nukkit.utils.Config
import io.netty.util.collection.IntObjectHashMap
import org.ktorm.database.Database
import java.util.*

/**
 * @author iGxnon
 */
class TeamSystem : PluginBase() {

    companion object {
        const val VERSION = "?"
        var debug = false
        val RANDOM = Random()

        // public static final Gson GSON = new Gson();
        lateinit var instance: TeamSystem
            private set

        val logger by lazy {
            instance.logger
        }
        val teams: IntObjectHashMap<TeamDao>
            get() = TeamManager.teams
        var database: Database? = null
            private set
        lateinit var language: Language
            private set
    }

    /**
     * 获取所有的 Team
     * 此方法适用于跨服的情况
     */

    override fun onLoad() {
        instance = this
        saveDefaultConfig()
    }

    override fun onEnable() {
        val lang = Config("$dataFolder/config.yml", Config.YAML).getString("language", "zh_CN")
        val config = Config(Config.PROPERTIES)
        config.load(getResource("languages/$lang.properties"))
        language = Language(config)
        if (this.config.getBoolean("MySQL.enable")) {
            logger.info(language.translateString("info.connectingToDatabase"))
            val sqlConfig = this.config.get("MySQL", HashMap<String, Any>())
            try {
                try {
                    Class.forName("com.mysql.cj.jdbc.Driver")
                } catch (e: ClassNotFoundException) {
                    logger.error(language.translateString("info.loadMysqlDriverFailed"))
                    throw RuntimeException(e)
                }
                database = connect(
                    (sqlConfig["host"] as String?)!!,
                    sqlConfig["port"] as Int,
                    (sqlConfig["database"] as String?)!!,
                    (sqlConfig["user"] as String?)!!,
                    (sqlConfig["password"] as String?)!!
                )
                if (!checkInit()) {
                    initDatabase()
                }
            } catch (e: Exception) {
                logger.error(language.translateString("info.connectToDatabaseFailed"), e)
                database = null
            }
        }
        server.pluginManager.registerEvents(EventListener(), this)
        logger.info(language.translateString("info.pluginEnabled", VERSION))
    }

    override fun onDisable() {

    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        if ("team".equals(command.name, ignoreCase = true)) {
            if (sender is Player) {
                FormHelper.showMain(sender)
            } else {
                sender.sendMessage(language.translateString("tips.useInGame"))
            }
            return true
        }
        return false
    }

    fun createTeam(teamId: Int, teamName: String, maxPlayer: Int, leader: Player): TeamMySQLDao {
        return TeamManager.createTeam(teamId, teamName, maxPlayer, leader)
    }

    fun quitTeam(player: Player) {
        quitTeam(player.name)
    }

    /**
     * 当需要踢出一个已经下线的玩家时
     */
    fun quitTeam(playerName: String) {
        val team = this.getTeamByPlayer(playerName) ?: return
        if (team.isTeamLeader(playerName)) {
            team.disband()
        } else {
            team.removePlayer(playerName)
        }
    }

    fun getTeamByPlayer(player: Player): TeamMySQLDao? {
        return getTeamByPlayer(player.name)
    }

    fun getTeamByPlayer(playerName: String): TeamMySQLDao? {
        for (team in teams.values) {
            if (team.players.contains(playerName)) {
                return team
            }
        }
        return null
    }
}