package com.roomservice.models

import com.roomservice.Constants
import kotlinx.serialization.Serializable

@Serializable
data class Player(val playerId: String, val name: String) {
    override fun toString(): String {
        return "$playerId${Constants.ROOM_BROADCAST_MSG_DELIMITER}$name"
    }
}