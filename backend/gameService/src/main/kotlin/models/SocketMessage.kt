package com.gameservice.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// File for messages to be sent to players
@Serializable
sealed class SocketMessage {
    fun toJson() = Json.encodeToString(this)
}

@Serializable
@SerialName("LEAVE")
data class LeaveMessage(val playerId: String) : SocketMessage()

@Serializable
@SerialName("PLAY_ORDER")
data class PlayOrderMessage(val playOrder: List<String>) : SocketMessage()

@Serializable
@SerialName("STATE")
data class StateMessage(val gameState: VisibleGameState) : SocketMessage()

@Serializable
@SerialName("DRAW")
data class DrawMessage(val playerId: String, val cards: List<Card>) : SocketMessage()

@Serializable
@SerialName("DISCARD")
data class DiscardMessage(val playerId: String, val card: Card) : SocketMessage()
