package com.gameservice.models

import kotlinx.serialization.Serializable

@Serializable
data class PendingInteraction(
    val fromPlayer: String,
    val toPlayer: String,
    val action: SocketMessage,
    var awaitingResponseFrom: String,
    val jsnStack: MutableList<Int> = mutableListOf()
)
