package com.gameservice.models

import com.gameservice.FAKE_CARD
import kotlinx.serialization.Serializable

@Serializable
data class PlayerState(val hand: MutableList<Card>, val propertyCollection: PropertyCollection, val bank: MutableList<Card>) {
    fun getStateWithHandHidden() : PlayerState {
        return PlayerState(
            MutableList(hand.size, { FAKE_CARD }),
            propertyCollection,
            bank
        )
    }
}