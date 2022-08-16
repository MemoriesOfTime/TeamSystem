package cn.lanink.teamsystem.distribute.server

import io.netty.channel.ChannelPipeline
import java.util.concurrent.ConcurrentHashMap

object SessionManager {
    val sessions = ConcurrentHashMap<String, Session>()
}

data class Session(val identity: String, val pipeline: ChannelPipeline)