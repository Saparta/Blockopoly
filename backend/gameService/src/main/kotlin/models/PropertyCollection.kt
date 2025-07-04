package com.gameservice.models

import com.gameservice.colorToRent
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
class PropertyCollection {
    val collection : MutableList<PropertySet> = emptyList<PropertySet>().toMutableList()

    fun addProperty(property: Card.Property, withColor: Color) : String? {
        if (!property.colors.contains(withColor)) return null
        val result = collection.find {
            propertySet -> propertySet.color == withColor && !propertySet.isComplete
        }
        if (result == null) {
            val setId = UUID.randomUUID().toString().replace("-","")
            collection.add(
                PropertySet(
                    setId,
                    mutableListOf(property),
                    withColor
                )
            )
            return setId
        } else {
            result.addProperty(property)
            return result.id
        }
    }

    fun removeProperty(property: Card.Property) {
        var removedFrom: PropertySet? = null

        collection.forEach {
            val wasRemoved = it.removeProperty(property)
            if (wasRemoved) {
                removedFrom = it
            }

        }

        removedFrom?.let {
            if (it.properties.isEmpty() && it.house == null && it.hotel == null) collection.remove(it)
        }
    }
}

@Serializable
data class PropertySet(val id: String, val properties: MutableList<Card.Property>, var color: Color?, var house: Card.Action? = null, var hotel: Card.Action? = null, var isComplete: Boolean = false) {

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
        if (!isCompleteSet()) {
            isComplete = false
        }
        if (properties.size == 1 && properties[0].value == null) {
            color = null
        }
        return wasRemoved
    }

    fun totalValue(): Int {
        return properties.sumOf { prop -> prop.value ?: 0 } + (house?.value ?: 0) + (hotel?.value ?: 0)
    }

    private fun isCompleteSet() : Boolean {
        return properties.size == colorToRent[color]!!.size
    }
}