package com.gameservice.models

import kotlinx.serialization.Serializable

// Data should be JSON
@Serializable
data class SocketMessage(val type: String, val data: String)
