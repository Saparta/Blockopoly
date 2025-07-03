package com.gameservice.models

import com.gameservice.FAKE_CARD
import kotlinx.serialization.Serializable

@Serializable
data class PlayerState(val hand: MutableList<Card>, val properties: PropertyCollection, val bank: MutableList<Card>) {
    fun getStateWithHandHidden() : PlayerState {
        return PlayerState(
            MutableList(hand.size, { FAKE_CARD }),
            properties,
            bank
        )
    }
}