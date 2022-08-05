@file:Suppress("UNUSED")
package cn.lanink.teamsystem.distribute.pack

import io.netty.buffer.ByteBuf
import io.netty.util.CharsetUtil

interface Pack {
    companion object {
        const val MAGIC_NUM: Int = 114514
        const val ID_HEARTBEAT: Byte = 0
        const val ID_MESSAGE: Byte = 1
        const val ID_LOGIN: Byte = 2

        fun decode(buf: ByteBuf): Packet {
            buf.skipBytes(4 + 1)
            return when (buf.readByte()) {
                ID_HEARTBEAT -> Packet.HeartBeatPacket()
                ID_MESSAGE -> Packet.Message().decode(buf)
                ID_LOGIN -> Packet.LoginPacket().decode(buf)
                else -> {
                    Packet.ErrorPack()
                }
            }
        }
    }
    val magic: Int
        get() = MAGIC_NUM
    val version: Byte
        get() = 1
    val packID: Byte
    var identity: String
        get() = ""
        set(_) = Unit

    fun encode(buf: ByteBuf): ByteBuf {
        buf.writeInt(magic)
        buf.writeByte(version.toInt())
        buf.writeByte(packID.toInt())
        if (identity != "") {
            buf.writeInt(identity.length)
            buf.writeCharSequence(identity, CharsetUtil.UTF_8)
        }
        return buf
    }

    fun decode(buf: ByteBuf): Packet {
        if (buf.isReadable(4)) {
            val idLen = buf.readInt()
            identity = buf.readCharSequence(idLen, CharsetUtil.UTF_8).toString()
        }
        return this as Packet
    }
}

sealed class Packet : Pack {
    data class Message(
        override val packID: Byte = Pack.ID_MESSAGE,
        override var identity: String = "",
        var target: String = "",
        var message: String = ""
    ): Packet() {

        override fun encode(buf: ByteBuf): ByteBuf {
            super.encode(buf)
            buf.writeInt(target.length)
            buf.writeCharSequence(target, CharsetUtil.UTF_8)

            buf.writeInt(message.length)
            buf.writeCharSequence(message, CharsetUtil.UTF_8)
            return buf
        }

        override fun decode(buf: ByteBuf): Packet {
            super.decode(buf)
            val targetLen = buf.readInt()
            target = buf.readCharSequence(targetLen, CharsetUtil.UTF_8).toString()

            val messLen = buf.readInt()
            message = buf.readCharSequence(messLen, CharsetUtil.UTF_8).toString()
            return this
        }
    }

    data class HeartBeatPacket(override val packID: Byte = Pack.ID_HEARTBEAT) : Packet()

    data class LoginPacket(override val packID: Byte = Pack.ID_LOGIN, override var identity: String = "") : Packet()

    data class ErrorPack(override val packID: Byte = -1) : Packet()
}