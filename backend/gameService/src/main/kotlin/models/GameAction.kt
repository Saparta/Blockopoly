package com.gameservice.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

data class Command(val playerId: String, val command: GameAction)

@Serializable
sealed class GameAction

@Serializable
@SerialName("StartTurn")
class StartTurn() : GameAction()

@Serializable
@SerialName("EndTurn")
class EndTurn() : GameAction()

@Serializable
@SerialName("PlayProperty")
data class PlayProperty(val id: Int, val color: Color) : GameAction()

@Serializable
@SerialName("MoveProperty")
data class PlayProperty(val id: Int, val color: Color) : GameAction()

@Serializable
@SerialName("Discard")
data class PlayProperty(val id: Int, val color: Color) : GameAction()

@Serializable
@SerialName("PlayMoney")
data class PlayMoney(val id: Int) : GameAction()