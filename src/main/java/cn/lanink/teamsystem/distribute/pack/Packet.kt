@file:Suppress("UNUSED")
package cn.lanink.teamsystem.distribute.pack

import cn.lanink.teamsystem.TeamSystem
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.util.CharsetUtil

interface Pack {
    companion object {
        const val MAGIC_NUM: Int = 114514  // 臭包（确信
        const val ID_HEARTBEAT: Byte = 0
        const val ID_MESSAGE: Byte = 1
        const val ID_LOGIN: Byte = 2
        const val ID_TELEPORT_REQ: Byte = 3
        const val ID_TELEPORT_RESP: Byte = 4

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
            return getPacket(packID).decode(buf)
        }

        private fun getPacket(id: Byte): Packet {
            return when (id) {
                ID_HEARTBEAT -> Packet.HeartBeatPacket()
                ID_MESSAGE -> Packet.Message(dest = "", message = "", target = "")
                ID_LOGIN -> Packet.LoginPacket()
                ID_TELEPORT_REQ -> Packet.TeleportRequestPacket(dest = "", target = "", sender = "")
                ID_TELEPORT_RESP -> Packet.TeleportResponsePacket(dest = "", target = "", ok = false)
                else -> Packet.ErrorPack()
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
        val buf = Unpooled.buffer(identity.length + 4)
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
        var dest: String,
        var target: String,
        var message: String
    ): Packet() {

        override fun encode(): ByteBuf {
            val buf = super.encode()
            buf.writeInt(dest.length)
            buf.writeCharSequence(dest, CharsetUtil.UTF_8)

            buf.writeInt(target.length)
            buf.writeCharSequence(target, CharsetUtil.UTF_8)

            buf.writeInt(message.length)
            buf.writeCharSequence(message, CharsetUtil.UTF_8)
            return buf
        }

        override fun decode(buf: ByteBuf): Packet {
            super.decode(buf)
            val destLen = buf.readInt()
            dest = buf.readCharSequence(destLen, CharsetUtil.UTF_8).toString()

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

    data class TeleportRequestPacket(
        override val packID: Byte = Pack.ID_TELEPORT_REQ,
        override var identity: String = TeamSystem.identity,
        var dest: String,
        var sender: String,
        var target: String
    ) : Packet() {

        override fun encode(): ByteBuf {
            val buf = super.encode()
            buf.writeInt(dest.length)
            buf.writeCharSequence(dest, CharsetUtil.UTF_8)

            buf.writeInt(sender.length)
            buf.writeCharSequence(sender, CharsetUtil.UTF_8)

            buf.writeInt(target.length)
            buf.writeCharSequence(target, CharsetUtil.UTF_8)
            return buf
        }

        override fun decode(buf: ByteBuf): Packet {
            super.decode(buf)
            val destLen = buf.readInt()
            dest = buf.readCharSequence(destLen, CharsetUtil.UTF_8).toString()

            val senderLen = buf.readInt()
            sender = buf.readCharSequence(senderLen, CharsetUtil.UTF_8).toString()

            val targetLen = buf.readInt()
            target = buf.readCharSequence(targetLen, CharsetUtil.UTF_8).toString()
            return this
        }

    }

    data class TeleportResponsePacket(
        override val packID: Byte = Pack.ID_TELEPORT_RESP,
        var dest: String,
        var target: String,
        var ok: Boolean,
        var ip: String = "",
        var port: Int = 0,
    ) : Packet() {

        override fun encode(): ByteBuf {
            val buf = super.encode()
            buf.writeInt(dest.length)
            buf.writeCharSequence(dest, CharsetUtil.UTF_8)

            buf.writeInt(target.length)
            buf.writeCharSequence(target, CharsetUtil.UTF_8)

            buf.writeBoolean(ok)

            buf.writeInt(ip.length)
            buf.writeCharSequence(ip, CharsetUtil.UTF_8)

            buf.writeInt(port)

            return buf
        }

        override fun decode(buf: ByteBuf): Packet {
            super.decode(buf)
            val destLen = buf.readInt()
            dest = buf.readCharSequence(destLen, CharsetUtil.UTF_8).toString()

            val targetLen = buf.readInt()
            target = buf.readCharSequence(targetLen, CharsetUtil.UTF_8).toString()

            ok = buf.readBoolean()

            val ipLen = buf.readInt()
            ip = buf.readCharSequence(ipLen, CharsetUtil.UTF_8).toString()

            port = buf.readInt()
            return this
        }

    }


    data class ErrorPack(override val packID: Byte = -1) : Packet()
}