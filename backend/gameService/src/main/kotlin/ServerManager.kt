package com.gameservice

import io.ktor.websocket.WebSocketSession
import java.util.concurrent.ConcurrentHashMap

object ServerManager {
    private val rooms = ConcurrentHashMap<String, RoomManager>()

    fun addRoom(room: RoomManager) {
        rooms.putIfAbsent(room.roomId, room)
    }

    suspend fun connectToRoom(roomId: String, playerId: String, session: WebSocketSession) {
        rooms.get(roomId)?.connectPlayer(playerId, session)
    }
}