package com.gameservice.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
class Interactions {
    private val _pendingInteractions: MutableList<PendingInteraction> = mutableListOf()
    @Transient
    private val targetToInteraction = mutableMapOf<String, PendingInteraction>()
    @Transient
    private var initiator: String? = null
    val pendingInteractions: List<PendingInteraction>
        get() = _pendingInteractions

    fun add(pendingInteraction: PendingInteraction) : Unit? {
        if (pendingInteraction.toPlayer in targetToInteraction) return null // Target already in interaction
        // New initiator implies previous interaction should be complete
        if (initiator != null && initiator != pendingInteraction.fromPlayer) return null
        initiator = pendingInteraction.fromPlayer
        targetToInteraction[pendingInteraction.toPlayer] = pendingInteraction
        _pendingInteractions.add(pendingInteraction)
        return Unit
    }

    fun remove(pendingInteraction: PendingInteraction) {
        if (_pendingInteractions.remove(pendingInteraction)) {
            initiator = if (_pendingInteractions.isEmpty()) null else initiator
            targetToInteraction.remove(pendingInteraction.toPlayer)
        }
    }

    fun getTargetedInteraction(targetedPlayer: String) : PendingInteraction? {
        return targetToInteraction[targetedPlayer]
    }

    fun isInitiator(playerId: String) = initiator != null && playerId == initiator

    fun isEmpty() : Boolean = pendingInteractions.isEmpty()
    fun isNotEmpty() : Boolean = pendingInteractions.isNotEmpty()
}

@Serializable
data class PendingInteraction(
    val fromPlayer: String,
    val toPlayer: String,
    val action: MultiStepInitiator,
    val initial: List<Int>,
    var awaitingResponseFrom: String,
    val offense: MutableList<Int> = mutableListOf(),
    val defense: MutableList<Int> = mutableListOf(),
    var resolved: Boolean = false
)