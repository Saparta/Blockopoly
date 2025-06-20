package com.roomservice.models

import com.roomservice.ROOM_BROADCAST_TYPE_DELIMITER
import com.roomservice.RoomBroadcastType
import kotlinx.serialization.Serializable

@Serializable
data class RoomBroadcast(val type: RoomBroadcastType, val message: String) {
    override fun toString(): String {
        return "$type${ROOM_BROADCAST_TYPE_DELIMITER}$message"
    }
}