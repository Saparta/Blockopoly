package com.gameservice

import java.util.concurrent.ConcurrentHashMap

object GameManager {
    private val rooms = ConcurrentHashMap<String, RoomManager>()

    fun addRoom(room: RoomManager) {
        rooms.putIfAbsent(room.roomId, room)
    }
}