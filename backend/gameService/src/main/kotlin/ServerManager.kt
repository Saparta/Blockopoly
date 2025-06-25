package com.gameservice

import com.gameservice.models.GameState
import io.ktor.websocket.WebSocketSession
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.concurrent.ConcurrentHashMap

object ServerManager {
    private val rooms = ConcurrentHashMap<String, DealGame>()

    fun addRoom(room: DealGame) {
        rooms.putIfAbsent(room.roomId, room)
    }

    suspend fun connectToRoom(roomId: String, playerId: String, session: WebSocketSession) : CompletableDeferred<MutableStateFlow<GameState>>? {
        return rooms.get(roomId)?.connectPlayer(playerId, session)
    }
}