package com.gameservice.models

import kotlinx.serialization.Serializable

@Serializable
data class PlayerState(val hand: MutableList<Card>, val properties: PropertyCollection, val bank: MutableList<Card>)