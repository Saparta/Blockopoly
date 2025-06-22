package com.gameservice.models

import com.gameservice.FAKE_CARD_ID
import kotlinx.serialization.Serializable

@Serializable
data class PlayerState(val hand: MutableList<Card>, val properties: PropertyCollection, val bank: MutableList<Card>) {
    fun getStateWithHandHidden() : PlayerState {
        return PlayerState(
            MutableList(hand.size, {_ -> Card.Action(FAKE_CARD_ID, 0, ActionType.PASS_GO)}),
            properties,
            bank)
    }
}