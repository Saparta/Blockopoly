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

    fun addProperty(property: Card.Property, withColor: Color) : String? = propertyCollection.addProperty(property, withColor)
    fun removeProperty(property: Card.Property) = propertyCollection.removeProperty(property)
    fun isPropertyInCollection(property: Card.Property) = propertyCollection.isPropertyInCollection(property)
    fun addDevelopment(development: Card.Action, propertySetId: String) = propertyCollection.addDevelopment(development, propertySetId)
    fun removeDevelopment(development: Card.Action) = propertyCollection.removeDevelopment(development)
    fun isDevelopmentInCollection(development: Card.Action): Boolean = propertyCollection.isDevelopmentInCollection(development)
    fun getPropertySet(setId: String): PropertySet? = propertyCollection.getPropertySet(setId)

    fun getNumOfSellableCards() : Int = propertyCollection.getNumOfSellableCards() + bank.size
    fun totalValue() : Int = propertyCollection.totalValue() + bank.sumOf { it.value ?: 0 }
}