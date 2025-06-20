package com.roomservice.models

import com.roomservice.ROOM_BROADCAST_MSG_DELIMITER
import kotlinx.serialization.Serializable

@Serializable
data class Player(val playerId: String, val name: String) {
    override fun toString(): String {
        return "$playerId${ROOM_BROADCAST_MSG_DELIMITER}$name"
    }
}