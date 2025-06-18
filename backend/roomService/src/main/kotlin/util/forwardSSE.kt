package com.roomservice.util

import com.roomservice.Constants
import com.roomservice.RedisPubSubManager
import io.ktor.server.sse.ServerSSESession
import io.ktor.sse.ServerSentEvent
import kotlinx.coroutines.channels.Channel

suspend fun forwardSSe(channel: Channel<String>, channelKey: String, session: ServerSSESession, pubSubManager: RedisPubSubManager, playerId: String) {
    for (msg in channel) {
        val (type, msg) = msg.split(Constants.ROOM_BROADCAST_TYPE_DELIMITER)
        if (type == "START") {
            session.send(ServerSentEvent("", type))
            pubSubManager.unsubscribe(channelKey, channel)
            session.close()
        } else if (type == Constants.RoomBroadcastType.CLOSED.toString()) {
            session.send(ServerSentEvent("", type))
            pubSubManager.unsubscribe(channelKey, channel)
            session.close()
        } else if (type == Constants.RoomBroadcastType.LEAVE.toString()
            && msg.split(Constants.ROOM_BROADCAST_MSG_DELIMITER).first() == playerId) {
            pubSubManager.unsubscribe(channelKey, channel)
            session.close()
        } else if (type !in arrayOf(Constants.RoomBroadcastType.JOIN.toString(), Constants.RoomBroadcastType.LEAVE.toString())
            || msg.split(Constants.ROOM_BROADCAST_MSG_DELIMITER).first() != playerId) {
            session.send(ServerSentEvent(msg, type))
        }
    }
}