package com.gameservice.models

import com.gameservice.ACCEPT_CHARGE_ACTION
import com.gameservice.ACCEPT_JUST_SAY_NO_ACTION
import com.gameservice.END_TURN_ACTION
import com.gameservice.JUST_SAY_NO_ACTION
import com.gameservice.PLAY_MONEY_ACTION
import com.gameservice.PLAY_PROPERTY_ACTION
import com.gameservice.REQUEST_RENT_ACTION
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// File for messages received from players
data class Command(val playerId: String, val command: GameAction)

@Serializable
sealed interface GameAction

@Serializable
@SerialName("StartTurn")
class StartTurn() : GameAction

@Serializable
@SerialName(END_TURN_ACTION)
class EndTurn() : GameAction

@Serializable
@SerialName(PLAY_PROPERTY_ACTION)
data class PlayProperty(val id: Int, val color: Color) : GameAction

@Serializable
@SerialName(PLAY_MONEY_ACTION)
data class PlayMoney(val id: Int) : GameAction

@Serializable
@SerialName(REQUEST_RENT_ACTION)
data class RequestRent(val rentCardId: Int, val rentDoublers: List<Int>, val rentingSetId: String, val target: String? = null) : GameAction

@Serializable
@SerialName(ACCEPT_CHARGE_ACTION)
data class AcceptCharge(val payment: List<Int>) : GameAction

@Serializable
@SerialName(JUST_SAY_NO_ACTION)
data class JustSayNo(val ids: List<Int>, val respondingTo: String? = null) : GameAction

@Serializable
@SerialName(ACCEPT_JUST_SAY_NO_ACTION)
data class AcceptJsn(val respondingTo: String) : GameAction
