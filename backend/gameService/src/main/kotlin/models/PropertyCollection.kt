package com.gameservice.models

import com.gameservice.HOTEL_ADDITIONAL_RENT
import com.gameservice.HOUSE_ADDITIONAL_RENT
import com.gameservice.colorToRent
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
class PropertyCollection {
    val collection : MutableList<PropertySet> = emptyList<PropertySet>().toMutableList()

    fun addProperty(property: Card.Property, withColor: Color) {
        if (!property.colors.contains(withColor)) return
        val result = collection.find {
            propertySet -> propertySet.color == withColor && !propertySet.isComplete
        }?.addProperty(property)
        if (result == null) {
            collection.add(
                PropertySet(
                    UUID.randomUUID().toString().replace("-",""),
                    mutableListOf(property),
                    withColor
                )
            )
        }
    }
}

@Serializable
data class PropertySet(val id: String, val properties: MutableList<Card.Property>, val color: Color, var house: Card.Action? = null, var hotel: Card.Action? = null, var isComplete: Boolean = false) {

    fun calculateRent() : Int {
        if (house != null) {
            if (hotel != null) {
                return colorToRent[color]!![properties.size - 1] + HOUSE_ADDITIONAL_RENT + HOTEL_ADDITIONAL_RENT
            }
            return colorToRent[color]!![properties.size - 1] + HOUSE_ADDITIONAL_RENT
        }
        return colorToRent[color]!![properties.size - 1]
    }

    fun addProperty(property: Card.Property) {
        properties.add(property)
        if (isCompleteSet()) {
            isComplete = true
        }
    }

    fun removeProperty(property: Card.Property) {
        properties.removeIf { prop -> prop.id == property.id }
        if (!isCompleteSet()) {
            isComplete = false
        }
    }

    private fun isCompleteSet() : Boolean {
        return properties.size == colorToRent[color]!!.size
    }
}