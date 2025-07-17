package com.gameservice.models

import com.gameservice.colorToRent
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
class PropertyCollection {
    private val collection : MutableMap<String, PropertySet> = mutableMapOf()
    private val propertyToSetId : MutableMap<Int, String> = mutableMapOf()
    private val developmentsToSetId : MutableMap<Int, String> = mutableMapOf()
    fun addProperty(property: Card.Property, withColor: Color) : String? {
        if (!property.colors.contains(withColor)) return null
        val result = collection.entries.find {
                (_, propertySet) -> propertySet.color == withColor && !propertySet.isComplete
        }
        if (result == null) {
            val setId = UUID.randomUUID().toString().replace("-","")
            collection.put(setId,
                PropertySet(
                    setId,
                    mutableListOf(property),
                    if (property.colors != Color.entries.toSet()) withColor else null
                )
            )
            propertyToSetId[property.id] = setId
            return setId
        } else {
            result.value.addProperty(property)
            propertyToSetId[property.id] = result.key
            return result.key
        }
    }

    fun removeProperty(property: Card.Property) {
        val removedFrom: PropertySet? = propertyToSetId[property.id]?.let {
            return@let if (collection[it]?.removeProperty(property) ?: false) collection[it] else null
        }

        removedFrom?.let {
            propertyToSetId.remove(property.id)
            if (it.isSetEmpty()) collection.remove(it.propertySetId)
        }
    }

    fun isPropertyInCollection(property: Card.Property): Boolean {
        return propertyToSetId[property.id] != null
    }

    fun removeDevelopment(development: Card.Action) {
        if (development.actionType !in arrayOf(ActionType.HOTEL, ActionType.HOTEL)) return
        developmentsToSetId[development.id]?.let { setId ->
            when (development.actionType) {
                ActionType.HOTEL -> collection[setId]?.hotel = null
                ActionType.HOUSE -> collection[setId]?.house = null
                else -> return
            }
            developmentsToSetId.remove(development.id)
            if (collection[setId]?.isSetEmpty() == true) collection.remove(setId)
        }
    }

    fun addDevelopment(development: Card.Action, propertySetId: String) {
        if (development.actionType !in arrayOf(ActionType.HOTEL, ActionType.HOTEL)) return
        val propertySet = collection[propertySetId] ?: return
        if (!propertySet.isComplete) return
        when (development.actionType) {
            ActionType.HOUSE -> {
                if (propertySet.house != null) return
                propertySet.house = development
            }
            ActionType.HOTEL -> {
                if (propertySet.house == null || propertySet.hotel != null) return
                propertySet.hotel = development
            }
            else -> return
        }
    }

    fun isDevelopmentInCollection(development: Card.Action): Boolean {
        return developmentsToSetId[development.id] != null
    }

    fun getPropertySet(setId: String): PropertySet? {
        return collection[setId]
    }

    fun totalValue() : Int {
        return collection.values.sumOf { it.totalValue() }
    }

    fun getNumOfSellableCards() : Int {
        return collection.values.fold(0) { acc, propertySet -> acc + propertySet.getNumOfSellableCards() }
    }
}

@Serializable
data class PropertySet(val propertySetId: String, val properties: MutableList<Card.Property>, var color: Color?, var house: Card.Action? = null, var hotel: Card.Action? = null, var isComplete: Boolean = false) {
    fun calculateRent() : Int {
        if (color == null) return 0
        if (!isComplete) return colorToRent[color]!![properties.size - 1]
        return colorToRent[color]!![properties.size - 1] + (house?.value ?: 0) + (hotel?.value ?: 0)
    }

    fun addProperty(property: Card.Property) {
        properties.add(property)
        if (isCompleteSet()) {
            isComplete = true
        }
    }

    fun removeProperty(property: Card.Property) : Boolean {
        val wasRemoved = properties.removeIf { prop -> prop.id == property.id }
        if (wasRemoved) {
            if (!isCompleteSet()) {
                isComplete = false
            }
            if (properties.map { it.value }.all { it == null }) {
                color = null
            }
        }
        return wasRemoved
    }

    fun totalValue(): Int {
        return properties.sumOf { prop -> prop.value ?: 0 } + (house?.value ?: 0) + (hotel?.value ?: 0)
    }

    fun isSetEmpty(): Boolean {
        return (properties.isEmpty() && house == null && hotel == null)
    }

    fun getNumOfSellableCards(): Int {
        return properties.filter { it.value != null }.size + (if (house == null) 0 else 1) + (if (hotel == null) 0 else 1)
    }

    private fun isCompleteSet() : Boolean {
        return properties.size == colorToRent[color]!!.size
    }
}