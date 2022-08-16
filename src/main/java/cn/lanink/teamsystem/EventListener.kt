@file:Suppress("UNUSED")
package cn.lanink.teamsystem

import cn.lanink.teamsystem.db.mysql.OnlinePlayer
import cn.lanink.teamsystem.db.mysql.OnlinePlayers
import cn.lanink.teamsystem.db.mysql.onlinePlayers
import cn.lanink.teamsystem.team.dao.TeamRedisDao
import cn.nukkit.Player
import cn.nukkit.Server
import cn.nukkit.event.EventHandler
import cn.nukkit.event.Listener
import cn.nukkit.event.player.PlayerJoinEvent
import cn.nukkit.event.player.PlayerQuitEvent
import org.ktorm.dsl.eq
import org.ktorm.dsl.update
import org.ktorm.entity.add
import org.ktorm.entity.find
import redis.clients.jedis.params.SetParams
import java.time.LocalDateTime

/**
 * @author iGxnon
 */
class EventListener : Listener {

    companion object {
        val transferTeleportQueue: HashMap<String, Player> = HashMap()  // senderName -> targetPos
    }

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val playerName = event.player.name
        val player = event.player
        TeamSystem.mysqlDb?.onlinePlayers?.apply {
            val found = find { it.playerName eq playerName }
            if (found == null) {
                add(OnlinePlayer {
                    name = playerName
                    ofOnlineTeam = null
                    quitAt = null
                    loginAt = TeamSystem.identity
                })
            } else {
                TeamSystem.mysqlDb!!.update(OnlinePlayers) {
                    set(it.quitAt, null)  // 置空
                    where {
                        it.playerName eq playerName
                    }
                }
            }
        }
        TeamSystem.redisDb?.resource?.use {
            it.set("${TeamRedisDao.loginRootKey}$playerName", TeamSystem.identity)
            it.del("${TeamRedisDao.quitRootKey}$playerName")
        }
        if (transferTeleportQueue.contains(playerName)) {
            Server.getInstance().scheduler.scheduleDelayedTask(TeamSystem.instance, {
                transferTeleportQueue[playerName] ?: run {
                    transferTeleportQueue.remove(playerName)
                    return@scheduleDelayedTask
                }
                if (transferTeleportQueue[playerName]?.isOnline == true) {
                    player?.teleport(transferTeleportQueue[playerName])
                }
                transferTeleportQueue.remove(playerName)
            }, 20)  // 延迟 1 s
        }
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        val name = event.player.name
        TeamSystem.mysqlDb?.apply {
            Server.getInstance().scheduler.scheduleDelayedTask(TeamSystem.instance, {
                this.onlinePlayers.find {
                    it.playerName eq name
                }?.quitAt?.apply { // quitAt 不是 null
                    // 判断是否已经超过 15 分钟了，期间可能登录过其他服务器并退出了，这时候交给其他服务器管理
                    if (this.plusMinutes(15).isBefore(LocalDateTime.now())) {
                        TeamSystem.quitTeam(name)
                    }
                }
            }, 15*60*20+40)  // 15min + 2s
        }?.update(OnlinePlayers) {
            set(it.quitAt, LocalDateTime.now())
            where {
                it.playerName eq name
            }
        }
        TeamSystem.redisDb?.resource?.use {
            Server.getInstance().scheduler.scheduleDelayedTask(TeamSystem.instance, {
                TeamSystem.redisDb!!.resource.use { inner ->
                    if (!inner.exists("${TeamRedisDao.quitRootKey}$name") ||
                        inner.ttl("${TeamRedisDao.quitRootKey}$name") <= 0) {
                        // 已经超过 15 分钟
                        TeamSystem.quitTeam(name)
                    }
                }
            }, 15*60*20+40)  // 15min + 2s
            // 15 分钟后失效
            it.set("${TeamRedisDao.quitRootKey}$name", "label", SetParams.setParams().ex(15*60))
        }
    }
}