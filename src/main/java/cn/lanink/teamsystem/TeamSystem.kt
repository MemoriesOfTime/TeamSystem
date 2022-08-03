package cn.lanink.teamsystem

import cn.lanink.formdsl.dsl.Button
import cn.lanink.formdsl.dsl.FormSimple
import cn.lanink.formdsl.dsl.onPlayerClick
import cn.lanink.gamecore.utils.Language
import cn.lanink.teamsystem.db.Db.checkInit
import cn.lanink.teamsystem.db.Db.connectMysql
import cn.lanink.teamsystem.db.Db.initDatabase
import cn.lanink.teamsystem.team.Team
import cn.lanink.teamsystem.team.TeamManager
import cn.lanink.teamsystem.team.dao.TeamDao
import cn.lanink.teamsystem.team.dao.TeamMySQLDao
import cn.nukkit.Player
import cn.nukkit.command.Command
import cn.nukkit.command.CommandSender
import cn.nukkit.plugin.PluginBase
import cn.nukkit.plugin.PluginLogger
import cn.nukkit.utils.Config
import io.netty.util.collection.IntObjectHashMap
import org.ktorm.database.Database
import redis.clients.jedis.Jedis
import redis.clients.jedis.JedisPool

/**
 * @author iGxnon
 */
class TeamSystem : PluginBase() {

    companion object {
        const val VERSION = "1.0.0-SNAPSHOT git-a26ac6f"
        var debug = false

        lateinit var instance: TeamSystem
            private set

        val logger: PluginLogger by lazy {
            instance.logger
        }
        val teams: IntObjectHashMap<Team>
            get() = TeamManager.teams
        var mysqlDb: Database? = null
            private set
        var redisDb: JedisPool? = null
            private set
        lateinit var language: Language
            private set

        fun showMainForm(player: Player) {
            val team = getTeamByPlayer(player)

            FormSimple {
                target = player
                title = language.translateString("form.main.title")
                if (team == null) {
                    Button {
                        text = language.translateString("form.main.button.createTeam")
                        onPlayerClick {
                            TeamManager.showCreateForm(player)
                        }
                    }
                    Button {
                        text = language.translateString("form.main.button.joinTeam")
                        onPlayerClick {
                            TeamManager.showJoinTeam(player)
                        }
                    }
                }else {
                    Button {
                        text = language.translateString("form.main.button.myTeam")
                        onPlayerClick {
                            team.formUI.showTeamInfo(player)
                        }
                    }
                    Button {
                        text = language.translateString("form.main.button.quitTeam")
                        onPlayerClick {
                            team.formUI.showQuitConfirm(player)
                        }
                    }
                }
            }
        }

        fun createTeam(teamId: Int, teamName: String, maxPlayer: Int, leader: Player): Team {
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
                TeamManager.disbandTeam(team)
            } else {
                team.removePlayer(playerName)
            }
        }

        fun getTeamByPlayer(player: Player): Team? {
            return getTeamByPlayer(player.name)
        }

        fun getTeamByPlayer(playerName: String): Team? {
            for (team in teams.values) {
                if (team.players.contains(playerName)) {
                    return team
                }
            }
            return null
        }

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
        val language = Config(Config.PROPERTIES).run {
            load(getResource("languages/$lang.properties"))
            Language(this)
        }
        val mysqlEnabled = config.getBoolean("MySQL.enable")
        val redisEnabled = config.getBoolean("Redis.enable")
        if (mysqlEnabled && redisEnabled) {
            logger.error(language.translateString("info.databaseConflict"))
            server.pluginManager.disablePlugin(this)
        }
        logger.info(language.translateString("info.connectingToDatabase"))
        try {
            if (mysqlEnabled) {
                val sqlConfig = this.config.get("MySQL", HashMap<String, Any>())

                try {
                    Class.forName("com.mysql.cj.jdbc.Driver")
                } catch (e: ClassNotFoundException) {
                    logger.error(language.translateString("info.loadMysqlDriverFailed"))
                    throw RuntimeException(e)
                }
                mysqlDb = connectMysql(
                    sqlConfig["host"] as String,
                    sqlConfig["port"] as Int,
                    sqlConfig["database"] as String,
                    sqlConfig["user"] as String,
                    sqlConfig["password"] as String
                )
                if (!checkInit()) {
                    initDatabase()
                }
            }
            if (redisEnabled) {
                val sqlConfig = this.config.get("Redis", HashMap<String, Any?>())
                val pool = JedisPool(
                    sqlConfig["host"] as String,
                    sqlConfig["port"] as Int,
                    sqlConfig["user"] as String?,
                    sqlConfig["password"] as String?,
                )
                pool.resource.use {
                    if (!it.isConnected || it.ping() != "PONG") {
                        throw RuntimeException("connect to redis failed!")
                    }
                }
                redisDb = pool
            }
        } catch (e: Exception) {
            logger.error(language.translateString("info.connectToDatabaseFailed"), e)
            mysqlDb = null
            redisDb = null
        }
        server.pluginManager.registerEvents(EventListener(), this)
        logger.info(language.translateString("info.pluginEnabled", VERSION))
    }

    override fun onDisable() {

    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        if ("team".equals(command.name, ignoreCase = true)) {
            if (sender is Player) {
                showMainForm(sender)
            } else {
                sender.sendMessage(language.translateString("tips.useInGame"))
            }
            return true
        }
        return false
    }

}