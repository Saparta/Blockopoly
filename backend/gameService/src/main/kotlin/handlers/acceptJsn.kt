package com.gameservice.handlers

import com.gameservice.DealGame
import com.gameservice.models.AcceptJsn
import com.gameservice.models.GameState
import com.gameservice.models.PendingInteraction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.updateAndGet

fun acceptJsn(room: DealGame, gameState: MutableStateFlow<GameState>, playerId: String, acceptJsn: AcceptJsn) : GameState{
    return gameState.updateAndGet { current ->
        if (!current.pendingInteractions.isInitiator(playerId)) return current
        val interaction = current.pendingInteractions.getTargetedInteraction(acceptJsn.respondingTo) ?: return current
        if (interaction.offense.isEmpty() && interaction.defense.isEmpty()) return current // No JSNs to accept
        if (isActionCancelled(interaction)) {
            current.pendingInteractions.remove(interaction)
        } else {
            interaction.awaitingResponseFrom = acceptJsn.respondingTo
            interaction.resolved = true
        }
        return@updateAndGet current.copy()
    }
}

fun isActionCancelled(interaction: PendingInteraction) : Boolean {
    return (interaction.initial.size + interaction.offense.size) == interaction.defense.size
}