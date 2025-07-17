package com.gameservice.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// File for messages to be sent to players
@Serializable
sealed interface SocketMessage {
    fun toJson() = Json.encodeToString(serializer(),this)
}

sealed interface MultiStepInteraction : SocketMessage

@Serializable
@SerialName("LEAVE")
data class LeaveMessage(val playerId: String) : SocketMessage

@Serializable
@SerialName("PLAY_ORDER")
data class PlayOrderMessage(val playOrder: List<String>) : SocketMessage

@Serializable
@SerialName("STATE")
data class StateMessage(val gameState: VisibleGameState) : SocketMessage

@Serializable
@SerialName("DRAW")
data class DrawMessage(val playerId: String, val cards: List<Card>) : SocketMessage

@Serializable
@SerialName("START_TURN")
data class StartMessage(val playerId: String) : SocketMessage

@Serializable
@SerialName("DISCARD")
data class DiscardMessage(val playerId: String, val card: Card) : SocketMessage

@Serializable
@SerialName("PLACE_IN_BANK")
data class PlaceInBankMessage(val playerId: String, val card: Card) : SocketMessage

@Serializable
@SerialName("PLACE_PROPERTY")
data class PlacePropertyMessage(val playerId: String, val card: Card.Property, val propertySetId: String) : SocketMessage

@Serializable
@SerialName("PAYMENT_EARNINGS")
data class PaymentEarningsMessage(val receiver: String, val giver: String, val propertyToDestination: Map<Int, String>, val bankCards: Set<Int>) : SocketMessage

@Serializable
@SerialName("RENT_REQUEST")
data class RentRequestMessage(val requester: String, val targets: List<String>, val cardsUsed: List<Int>, val amount: Int) : SocketMessage, MultiStepInteraction

@Serializable
@SerialName("JUST_SAY_NO")
data class JustSayNoMessage(val playerId: String, val respondingTo: String) : SocketMessage