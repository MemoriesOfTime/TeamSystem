@file:Suppress("UNUSED")
package cn.lanink.teamsystem.distribute.pack

import cn.lanink.teamsystem.TeamSystem
import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufAllocator
import io.netty.util.CharsetUtil

interface Pack {
    companion object {
        const val MAGIC_NUM: Int = 114514
        const val ID_HEARTBEAT: Byte = 0
        const val ID_MESSAGE: Byte = 1
        const val ID_LOGIN: Byte = 2

        fun encode(pack: Packet, buf: ByteBuf) {
            buf.writeInt(pack.magic)  // 4
            buf.writeByte(pack.version.toInt())  // 1
            buf.writeByte(pack.packID.toInt())  // 1
            val data = pack.encode()
            buf.writeInt(data.readableBytes())  // 4  length
            buf.writeBytes(data)
        }

        fun decode(buf: ByteBuf): Packet {
            buf.skipBytes(5)  // 4 + 1
            val packID = buf.readByte()  // 1
            buf.skipBytes(4)  // 4
            return when (packID) {
                ID_HEARTBEAT -> Packet.HeartBeatPacket()
                ID_MESSAGE -> Packet.Message().decode(buf)
                ID_LOGIN -> Packet.LoginPacket().decode(buf)
                else -> {
                    Packet.ErrorPack()
                }
            }
        }
    }
    val magic: Int          // 4 bytes
        get() = MAGIC_NUM
    val version: Byte       // 1 byte
        get() = 1
    val packID: Byte        // 1 byte
    // size 4 bytes
    var identity: String  // 如果一个包有 identity 就重写该属性
        get() = ""
        set(_) = Unit

    fun encode(): ByteBuf {
        val buf = ByteBufAllocator.DEFAULT.ioBuffer()
        if (identity != "") {
            buf.writeInt(identity.length)
            buf.writeCharSequence(identity, CharsetUtil.UTF_8)
        }
        return buf
    }

    fun decode(buf: ByteBuf): Packet {
        if (identity != "") {
            val idLen = buf.readInt()
            identity = buf.readCharSequence(idLen, CharsetUtil.UTF_8).toString()
        }
        return this as Packet
    }
}

sealed class Packet : Pack {
    data class Message(
        override val packID: Byte = Pack.ID_MESSAGE,
        var target: String = "",
        var message: String = ""
    ): Packet() {

        override var identity: String = TeamSystem.identity

        override fun encode(): ByteBuf {
            val buf = super.encode()
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

    data class LoginPacket(
        override val packID: Byte = Pack.ID_LOGIN,
        override var identity: String = TeamSystem.identity
    ) : Packet()

    data class ErrorPack(override val packID: Byte = -1) : Packet()
}