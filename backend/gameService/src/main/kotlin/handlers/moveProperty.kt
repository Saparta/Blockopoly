package com.gameservice.handlers

import com.gameservice.DealGame
import com.gameservice.models.Card
import com.gameservice.models.Color
import com.gameservice.models.MoveProperty
import com.gameservice.models.PropertyMovedMessage
import com.gameservice.models.GameState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.updateAndGet

suspend fun moveProperty(room: DealGame, game: MutableStateFlow<GameState>, playerId: String, action: MoveProperty): GameState {
    return game.updateAndGet { current ->
        // Validate it's the player's turn
        if (current.playerAtTurn != playerId) return current
        
        // Validate no pending interactions
        if (current.pendingInteractions.isNotEmpty()) return current
        
        val playerState = current.playerState[playerId] ?: return current
        
        // Find the property in the player's collection
        val propertyToMove = playerState.propertyCollection.collection.values
            .flatMap { it.properties }
            .find { it.id == action.cardId } ?: return current
        
        // Get the source set (if specified)
        val fromSet = if (action.fromSetId != null) {
            playerState.propertyCollection.getPropertySet(action.fromSetId) ?: return current
        } else {
            playerState.propertyCollection.getSetOfProperty(action.cardId) ?: return current
        }
        
        // Get the target set
        val toSet = playerState.propertyCollection.getPropertySet(action.toSetId) ?: return current
        
        // Validate both sets belong to the same player (already validated by getting from playerState)
        // Validate the move is legal according to game rules
        
        // Check if the property can be moved to the target set
        val targetColor = toSet.color
        val propertyColors = propertyToMove.colors
        
        // Determine the effective color for wild cards
        val effectiveColor = when {
            // Rainbow wild (ALL_COLOR_SET) can go anywhere
            propertyColors == com.gameservice.models.ALL_COLOR_SET -> targetColor
            // Regular wild with multiple colors - must match target set color
            propertyColors.size > 1 -> if (propertyColors.contains(targetColor)) targetColor else return current
            // Single color property - must match target set color
            else -> if (propertyColors.contains(targetColor)) targetColor else return current
        }
        
        // Validate target set can accept the property (not complete, compatible color)
        if (toSet.isComplete) return current
        if (targetColor != null && effectiveColor != targetColor) return current
        
        // Remove property from source set
        val (wasRemoved, removedDevelopments) = fromSet.removeProperty(propertyToMove)
        if (!wasRemoved) return current
        
        // Add removed developments to bank
        removedDevelopments?.forEach { playerState.bank.add(it) }
        
        // If source set is now empty, remove it
        if (fromSet.isSetEmpty()) {
            playerState.propertyCollection.removePropertySet(fromSet.propertySetId)
        }
        
        // Add property to target set with effective color
        toSet.addProperty(propertyToMove, effectiveColor)
        
        // Update property-to-set mapping by removing and re-adding the set
        // This ensures the internal mapping is updated correctly
        val tempSet = playerState.propertyCollection.removePropertySet(toSet.propertySetId)
        if (tempSet != null) {
            playerState.propertyCollection.addPropertySet(tempSet)
        }
        
        // Broadcast the move event
        room.sendBroadcast(PropertyMovedMessage(
            playerId = playerId,
            cardId = action.cardId,
            fromSetId = action.fromSetId,
            toSetId = action.toSetId,
            newIdentityIfWild = if (propertyColors.size > 1) effectiveColor else null
        ))
        
        return current
    }
}
