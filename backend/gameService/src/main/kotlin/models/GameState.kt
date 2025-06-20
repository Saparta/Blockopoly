package com.gameservice.models

import com.gameservice.deck
import kotlinx.serialization.Serializable

@Serializable
class GameState {
    lateinit var playerAtTurn: String
    var winningPlayer : String? = null
    val drawPile : MutableList<Card> = deck.shuffled().toMutableList()
    val discardPile : MutableList<Card> = mutableListOf()
    val playerState : MutableMap<String, PlayerState> = mutableMapOf()
}