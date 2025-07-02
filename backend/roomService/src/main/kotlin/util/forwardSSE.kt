package com.roomservice.util

import com.roomservice.PUBSUB_MANAGER_KEY
import com.roomservice.ROOM_BROADCAST_MSG_DELIMITER
import com.roomservice.ROOM_BROADCAST_TYPE_DELIMITER
import com.roomservice.RoomBroadcastType.CLOSED
import com.roomservice.RoomBroadcastType.JOIN
import com.roomservice.RoomBroadcastType.LEAVE
import com.roomservice.RoomBroadcastType.RECONNECT
import com.roomservice.models.RoomSubChannel
import io.ktor.server.sse.ServerSSESession
import io.ktor.sse.ServerSentEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun forwardSSe(roomSubChannel: RoomSubChannel, session: ServerSSESession) {
    val pubSubManager = session.call.application.attributes[PUBSUB_MANAGER_KEY]
    withContext(Dispatchers.IO) {
        for (msg in roomSubChannel.channel) {
            val (type, msg) = msg.split(ROOM_BROADCAST_TYPE_DELIMITER)
            when (type) {
                "START" -> {
                    session.send(ServerSentEvent("", type))
                    pubSubManager.unsubscribe(roomSubChannel.channelKey, roomSubChannel.channel)
                    session.close()
                }
                RECONNECT.toString() -> {
                    val (playerId, channelId) = msg.split(ROOM_BROADCAST_MSG_DELIMITER)
                    if (roomSubChannel.playerId == playerId && roomSubChannel.channelId != channelId) {
                        session.send(ServerSentEvent("", type))
                        pubSubManager.unsubscribe(roomSubChannel.channelKey, roomSubChannel.channel)
                        session.close()
                    }
                }
                CLOSED.toString() -> {
                    session.send(ServerSentEvent("", type))
                    pubSubManager.unsubscribe(roomSubChannel.channelKey, roomSubChannel.channel)
                    session.close()
                }
                LEAVE.toString() -> {
                    if (msg.split(ROOM_BROADCAST_MSG_DELIMITER).first() == roomSubChannel.playerId) {
                        pubSubManager.unsubscribe(roomSubChannel.channelKey, roomSubChannel.channel)
                        session.close()
                    } else {
                        session.send(ServerSentEvent(msg, type))
                    }
                }
                JOIN.toString() -> {
                    if (msg.split(ROOM_BROADCAST_MSG_DELIMITER).first() != roomSubChannel.playerId) {
                        session.send(ServerSentEvent(msg, type))
                    }
                } else -> session.send(ServerSentEvent(msg, type))
            }
        }
    }
}