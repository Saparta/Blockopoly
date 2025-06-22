package com.gameservice.models

import com.gameservice.HOTEL_ADDITIONAL_RENT
import com.gameservice.HOUSE_ADDITIONAL_RENT
import com.gameservice.colorToRent
import kotlinx.serialization.Serializable

@Serializable
class PropertyCollection {
    val collection : MutableList<PropertySet> = emptyList<PropertySet>().toMutableList()

    fun addProperty(property: Card.Property) {
        collection.find {
            propertySet -> propertySet.color == property.color && !propertySet.isComplete()
        }?.properties?.add(property)
    }
}

@Serializable
data class PropertySet(val id: String, val properties: MutableList<Card.PropertyCard>, val color: Color, var house: Card.Action? = null, var hotel: Card.Action? = null) {

    fun calculateRent() : Int {
        if (house != null) {
            if (hotel != null) {
                return colorToRent[color]!![properties.size - 1] + HOUSE_ADDITIONAL_RENT + HOTEL_ADDITIONAL_RENT
            }
            return colorToRent[color]!![properties.size - 1] + HOUSE_ADDITIONAL_RENT
        }
        return colorToRent[color]!![properties.size - 1]
    }

    fun isComplete() : Boolean {
        return properties.size == colorToRent[color]!!.size
    }
}