package com.gameservice.models

import com.gameservice.FAKE_CARD
import kotlinx.serialization.Serializable

@Serializable
data class PlayerState(val hand: MutableList<Card>, val propertyCollection: PropertyCollection, val bank: MutableSet<Card>) {
    fun getPublicPlayerState() : PlayerState {
        return PlayerState(
            MutableList(hand.size) { FAKE_CARD },
            propertyCollection,
            bank
        )
    }

    fun totalValue() : Int {
        return propertyCollection.totalValue() + bank.sumOf { it.value ?: 0 }
    }
}