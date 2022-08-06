package cn.lanink.teamsystem

import cn.lanink.formdsl.dsl.Button
import cn.lanink.formdsl.dsl.FormSimple
import cn.lanink.formdsl.dsl.onPlayerClick
import cn.lanink.gamecore.utils.Language
import cn.lanink.teamsystem.db.Db.checkInit
import cn.lanink.teamsystem.db.Db.connectMysql
import cn.lanink.teamsystem.db.Db.initDatabase
import cn.lanink.teamsystem.distribute.client.startClient
import cn.lanink.teamsystem.distribute.pack.Packet
import cn.lanink.teamsystem.distribute.server.startServer
import cn.lanink.teamsystem.team.Team
import cn.lanink.teamsystem.team.TeamManager
import cn.nukkit.Player
import cn.nukkit.command.Command
import cn.nukkit.command.CommandSender
import cn.nukkit.plugin.PluginBase
import cn.nukkit.plugin.PluginLogger
import cn.nukkit.utils.Config
import io.netty.channel.Channel
import io.netty.channel.EventLoopGroup
import io.netty.util.collection.IntObjectHashMap
import org.ktorm.database.Database
import redis.clients.jedis.JedisPool

/**
 * @author iGxnon
 */
class TeamSystem : PluginBase() {

    companion object {
        const val VERSION = "1.0.0-SNAPSHOT git-259d55e"
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
        var serverChannel: Channel? = null
            private set
        var serverLoops: Pair<EventLoopGroup, EventLoopGroup>? = null
            private set
        var clientLoop: EventLoopGroup? = null
            private set
        var identity: String = ""
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
        language = Config(Config.PROPERTIES).run {
            load(getResource("languages/$lang.properties"))
            Language(this)
        }
        val mysqlEnabled = config.getBoolean("MySQL.enable")
        val redisEnabled = config.getBoolean("Redis.enable")
        val distributionEnabled = config.getBoolean("Distribute.enable")
        if (mysqlEnabled && redisEnabled) {
            logger.error(language.translateString("info.databaseConflict"))
            server.pluginManager.disablePlugin(this)
            return
        }
        if (distributionEnabled && !mysqlEnabled && !redisEnabled) {
            logger.error(language.translateString("info.distributeConfigWrong1"))
            server.pluginManager.disablePlugin(this)
            return
        }
        if ((mysqlEnabled xor redisEnabled) && !distributionEnabled) {
            logger.error(language.translateString("info.distributeConfigWrong2"))
            server.pluginManager.disablePlugin(this)
            return
        }
        logger.info(language.translateString("info.connectingToDatabase"))
        try {
            if (mysqlEnabled) {
                val sqlConfig = config.get("MySQL", HashMap<String, Any>())

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
                logger.info(language.translateString("info.connectToMysqlDatabase"))
            } else if (redisEnabled) {
                val sqlConfig = config.get("Redis", HashMap<String, Any?>())
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
                logger.info(language.translateString("info.connectToRedisDatabase"))
            } else {
                logger.info(language.translateString("info.connectToLocalDatabase"))
            }
        } catch (e: Exception) {
            logger.error(language.translateString("info.connectToRemoteDatabaseFailed"), e)
            mysqlDb = null
            redisDb = null
            logger.info(language.translateString("info.connectToLocalDatabase"))
        }
        if (distributionEnabled) {
            val distributeConfig = config.get("Distribute", HashMap<String, Any>())
            val host = distributeConfig["host"] as String
            val port = distributeConfig["port"] as Int
            identity = distributeConfig["id"] as String
            if (identity == "") {
                logger.error(language.translateString("info.distributeConfigWrong3"))
                server.pluginManager.disablePlugin(this)
                return
            }
            if (distributeConfig["type"] == "master") {
                serverLoops = startServer(port)
                val out = startClient(identity, host, port)
                serverChannel = out.first
                clientLoop = out.second
            }else if (distributeConfig["type"] == "slave") {
                val out = startClient(identity, host, port)
                serverChannel = out.first
                clientLoop = out.second
            } else {
                logger.warning(language.translateString("info.distributeConfigWrong4"))
            }
        }
        server.pluginManager.registerEvents(EventListener(), this)
        logger.info(language.translateString("info.pluginEnabled", VERSION))
    }

    override fun onDisable() {
        clientLoop?.shutdownGracefully()?.sync()
        serverLoops?.apply {
            first.shutdownGracefully().sync()
            second.shutdownGracefully().sync()
        }
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