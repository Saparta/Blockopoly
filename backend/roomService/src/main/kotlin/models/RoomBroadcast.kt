package com.roomservice.models

import com.roomservice.Constants
import kotlinx.serialization.Serializable

@Serializable
data class RoomBroadcast(val type: Constants.RoomBroadcastType, val message: String) {
    override fun toString(): String {
        return "$type;$message"
    }
}