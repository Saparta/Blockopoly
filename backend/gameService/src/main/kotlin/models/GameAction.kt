package com.gameservice.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// File for messages received from players
data class Command(val playerId: String, val command: GameAction)

@Serializable
sealed interface GameAction {
    fun toJson(): String = Json.encodeToString(serializer(),this)
}

@Serializable
@SerialName("StartTurn")
class StartTurn() : GameAction

@Serializable
@SerialName("EndTurn")
class EndTurn() : GameAction

@Serializable
@SerialName("PlayProperty")
data class PlayProperty(val id: Int, val color: Color) : GameAction

@Serializable
@SerialName("PlayMoney")
data class PlayMoney(val id: Int) : GameAction

@Serializable
@SerialName("RequestRent")
data class RequestRent(val rentCardId: Int, val rentDoublers: List<Int>, val rentingSetId: String, val target: String? = null) : GameAction

@Serializable
@SerialName("AcceptCharge")
data class AcceptCharge(val payment: List<Int>) : GameAction

@Serializable
@SerialName("JustSayNo")
data class JustSayNo(val id: Int) : GameAction

