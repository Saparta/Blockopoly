package com.gameservice.models

import com.gameservice.MAX_CARDS_PER_TURN
import com.gameservice.deck
import kotlinx.serialization.Serializable

@Serializable
class GameState(var playerAtTurn: String?,
                var winningPlayer: String?,
                val drawPile: MutableList<Card>,
                val discardPile: MutableList<Card>,
                val playerState: MutableMap<String, PlayerState>) {

    lateinit var playerOrder : List<String>
    var cardsLeftToPlay: Int = MAX_CARDS_PER_TURN

    constructor() : this(
        null,
        null,
        deck.shuffled().toMutableList(),
        mutableListOf(),
        mutableMapOf()
    )

    constructor(
        playerAtTurn: String?,
        winningPlayer: String?,
        drawPile: MutableList<Card>,
        discardPile: MutableList<Card>,
        playerState: MutableMap<String, PlayerState>,
        playerOrder : List<String>,
        cardsLeftToPlay : Int) : this(playerAtTurn, winningPlayer, drawPile, discardPile, playerState) {
        this.playerOrder = playerOrder
        this.cardsLeftToPlay = cardsLeftToPlay
    }

    fun draw() : GameState {
        if (playerAtTurn == null) return this
        var numToDraw = 2
        playerState[playerAtTurn]?.hand?.size?.let {
            if (it <= 0) {
                numToDraw = 5
            }
        }
        var cardsDrawn = 0
        val cards : MutableList<Card> = mutableListOf()
        while (cardsDrawn < numToDraw) {
            if (drawPile.isEmpty()) {
                drawPile.addAll(discardPile.shuffled())
                discardPile.clear()
            }
            cards.add(drawPile.removeFirst())
            cardsDrawn++
        }
        playerState[playerAtTurn]?.hand?.addAll(cards)
        return this.copy()
    }

    fun copy(
        playerAtTurn: String? = this.playerAtTurn,
        winningPlayer: String? = this.winningPlayer,
        drawPile: MutableList<Card> = this.drawPile,
        discardPile: MutableList<Card> = this.discardPile,
        playerState: MutableMap<String, PlayerState> = this.playerState,
        playerOrder: List<String> = this.playerOrder,
        cardsLeftToPlay: Int = this.cardsLeftToPlay
    ): GameState {
        return GameState(playerAtTurn, winningPlayer, drawPile, discardPile, playerState, playerOrder, cardsLeftToPlay)
    }

    fun isCardInHand(player: String, card: Card): Boolean {
        return playerState[player]?.hand?.find { it.id == card.id } != null
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
    val cardsLeftToPlay : Int

    constructor(gameState: GameState, playerId: String) {
        playerAtTurn = gameState.playerAtTurn
        winningPlayer = gameState.winningPlayer
        drawPileSize = gameState.drawPile.size
        discardPile = gameState.discardPile
        cardsLeftToPlay = gameState.cardsLeftToPlay
        gameState.playerState.forEach {
                (id, state) ->  if (id == playerId) playerState[id] = state else playerState[id] = state.getStateWithHandHidden() }
    }
}