package com.gameservice.models

import com.gameservice.DEVELOPMENT_ACTION_CARDS
import com.gameservice.FAKE_CARD
import kotlinx.serialization.Serializable

@Serializable
data class PlayerState(val hand: MutableList<Card>, private val propertyCollection: PropertyCollection, val bank: MutableSet<Card>) {
    fun getPublicPlayerState() : PlayerState {
        return PlayerState(
            MutableList(hand.size) { FAKE_CARD },
            propertyCollection,
            bank
        )
    }

    fun addProperty(property: Card.Property, withColor: Color?) : String? = propertyCollection.addProperty(property, withColor)
    fun removeProperty(property: Card.Property) : Unit? {
        val developmentsRemoved = propertyCollection.removeProperty(property)
        if (developmentsRemoved == null) {
            return null
        } else {
            developmentsRemoved.forEach(bank::add)
        }
        return Unit
    }
    fun isPropertyInCollection(property: Card.Property) : Boolean = propertyCollection.isPropertyInCollection(property)
    fun addDevelopment(development: Card.Action, color: Color) : String? = propertyCollection.addDevelopment(development, color)?.propertySetId
    fun addDevelopment(development: Card.Action, propertySetId: String) : Unit? = propertyCollection.addDevelopment(development, propertySetId)
    fun removeDevelopment(development: Card.Action) : Unit? = propertyCollection.removeDevelopment(development)
    fun isDevelopmentInCollection(development: Card.Action): Boolean = propertyCollection.isDevelopmentInCollection(development)
    fun getPropertySet(setId: String): PropertySet? = propertyCollection.getPropertySet(setId)
    fun getSetOfProperty(property: Card.Property) : PropertySet? = propertyCollection.getSetOfProperty(property.id)
    fun getNumOfSellableCards() : Int = propertyCollection.getNumOfSellableCards() + bank.size
    fun totalValue() : Int = propertyCollection.totalValue() + bank.sumOf { it.value ?: 0 }

    fun getSetOfDevelopment(development: Card.Action) : PropertySet? {
        if (development.actionType !in DEVELOPMENT_ACTION_CARDS) return null
        return propertyCollection.getSetOfDevelopment(development.id)
    }

    fun removePropertySet(setId: String) : PropertySet? = propertyCollection.removePropertySet(setId)

    fun addPropertySet(set: PropertySet) = propertyCollection.addPropertySet(set)
}