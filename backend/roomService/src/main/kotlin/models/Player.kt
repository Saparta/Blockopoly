package com.roomservice.models

import kotlinx.serialization.Serializable

@Serializable
data class Player(val playerId: String, val name: String)