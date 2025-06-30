package com.roomservice.models

import kotlinx.coroutines.channels.Channel

data class RoomSubChannel(
    val channel: Channel<String>,
    val channelKey: String,
    val channelId: String,
    val playerId: String
)
