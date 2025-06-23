package com.gameservice.models

import com.gameservice.deck
import kotlinx.serialization.Serializable

@Serializable
class GameState(var playerAtTurn: String?,
                var winningPlayer: String?,
                val drawPile: MutableList<Card>,
                val discardPile: MutableList<Card>,
                val playerState: MutableMap<String, PlayerState>) {

    lateinit var playerOrder : List<String>

    constructor() : this(
        null,
        null,
        deck.shuffled().toMutableList(),
        mutableListOf(),
        mutableMapOf()
    )

    fun draw() {
        if (playerAtTurn == null) return
        var cardsDrawn = 0
        val cards : MutableList<Card> = mutableListOf()
        while (cardsDrawn < 2) {
            if (drawPile.isEmpty()) {
                drawPile.addAll(discardPile.shuffled())
                discardPile.clear()
            }
            cards.add(drawPile.removeFirst())
            cardsDrawn++
        }
        playerState[playerAtTurn]?.hand?.addAll(cards)
    }

    fun stateVisibleToPlayer(playerId: String) : VisibleGameState {
        return VisibleGameState(this, playerId)
    }
}

@Serializable
class VisibleGameState {
    val playerAtTurn : String?
    val winningPlayer: String?
    val drawPileSize: Int
    val discardPile : MutableList<Card>
    val playerState : MutableMap<String, PlayerState> = mutableMapOf()

    constructor(gameState: GameState, playerId: String) {
        playerAtTurn = gameState.playerAtTurn
        winningPlayer = gameState.winningPlayer
        drawPileSize = gameState.drawPile.size
        discardPile = gameState.discardPile
        gameState.playerState.forEach {
                (id, state) ->  if (id == playerId) playerState[id] = state else playerState[id] = state.getStateWithHandHidden() }
    }
}