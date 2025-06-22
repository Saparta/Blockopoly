package com.gameservice.models

import kotlinx.serialization.Serializable

@Serializable
enum class Color{
    BROWN, BLUE, GREEN, TURQOUISE, UTILITY, RAILROAD, ORANGE, MAGENTA, RED, YELLOW
}

@Serializable
enum class CardType {
    ACTION,
    PROPERTY,
    MONEY
}

// Action cards that don't need state
enum class ActionType {
    FORCED_DEAL,
    SLY_DEAL,
    DEAL_BREAKER,
    BIRTHDAY,
    JUST_SAY_NO,
    HOUSE,
    HOTEL,
    DEBT_COLLECTOR,
    WILD_RENT,
    RENT,
    DOUBLE_RENT,
    PASS_GO
}

@Serializable
sealed class Card {
    abstract val id: Int
    abstract val cardType: CardType
    abstract val value: Int?


    @Serializable
    sealed class ActionCard() : Card() {
        abstract val actionType: ActionType
    }

    // Client will send id, who's using it, and optionally what player/properties/sets it's being used on
    @Serializable
    data class Action(override val id: Int, override val value: Int, override val actionType: ActionType) : ActionCard() {
        override val cardType = CardType.ACTION
    }

    // Client will send id, who's using it, and the color being used
    @Serializable
    data class Rent(override val id: Int, override val value: Int, override val actionType : ActionType, val colors: Set<Color>) : ActionCard() {
        override val cardType = CardType.ACTION
    }

    @Serializable
    sealed class PropertyCard() : Card() {
        abstract val color: Color?
    }

    // Client will send id of card and who's using it
    @Serializable
    data class Property(override val id: Int, override val color: Color, override val value: Int) : PropertyCard() {
        override val cardType = CardType.PROPERTY
    }

    // Client will send id of card, who's using it, and the color it's being placed as
    @Serializable
    data class WildProperty(override val id: Int, override var color: Color?, override val value: Int?, val possibleColors: Set<Color>) : PropertyCard() {
        override val cardType = CardType.PROPERTY
    }

    // Client will send id of card and who's using it
    @Serializable
    data class Money(override val id: Int, override val value: Int) : Card() {
        override val cardType = CardType.MONEY
    }
}

fun createCardMapping() : Map<Int, Card> {
    var id = 0
    return buildMap {
        // Action cards
        repeat(2) { id++; put(id, Card.Action(id, 5, ActionType.DEAL_BREAKER))}
        repeat(3) { id++; put(id, Card.Action(id, 4, ActionType.JUST_SAY_NO))}
        repeat(3) { id++; put(id, Card.Action(id, 3, ActionType.SLY_DEAL))}
        repeat(3) { id++; put(id, Card.Action(id, 3, ActionType.FORCED_DEAL))}
        repeat(3) { id++; put(id, Card.Action(id, 3, ActionType.DEBT_COLLECTOR))}
        repeat(3) { id++; put(id, Card.Action(id, 2, ActionType.BIRTHDAY))}
        repeat(3) { id++; put(id, Card.Action(id, 3, ActionType.HOUSE))}
        repeat(2) { id++; put(id, Card.Action(id, 4, ActionType.HOTEL))}
        repeat(2) { id++; put(id, Card.Action(id, 1, ActionType.DOUBLE_RENT))}
        repeat(10) { id++; put(id, Card.Action(id, 1, ActionType.PASS_GO))}

        // Rent cards
        repeat(2) { id++; put(id, Card.Rent(id, 1, ActionType.RENT, setOf(Color.BLUE, Color.GREEN)))}
        repeat(2) { id++; put(id, Card.Rent(id, 1, ActionType.RENT, setOf(Color.BROWN, Color.TURQOUISE)))}
        repeat(2) { id++; put(id, Card.Rent(id, 1, ActionType.RENT, setOf(Color.MAGENTA, Color.ORANGE)))}
        repeat(2) { id++; put(id, Card.Rent(id, 1, ActionType.RENT, setOf(Color.RED, Color.YELLOW)))}
        repeat(2) { id++; put(id, Card.Rent(id, 1, ActionType.RENT, setOf(Color.RAILROAD, Color.UTILITY)))}
        repeat(3) { id++; put(id, Card.Rent(id, 3, ActionType.WILD_RENT, Color.entries.toSet()))}

        // Properties
        repeat(2) { id++; put(id, Card.Property(id, Color.BLUE, 4))}
        repeat(3) { id++; put(id, Card.Property(id, Color.GREEN, 4))}
        repeat(2) { id++; put(id, Card.Property(id, Color.BROWN, 1))}
        repeat(3) { id++; put(id, Card.Property(id, Color.TURQOUISE, 1))}
        repeat(3) { id++; put(id, Card.Property(id, Color.MAGENTA, 2))}
        repeat(3) { id++; put(id, Card.Property(id, Color.ORANGE, 2))}
        repeat(3) { id++; put(id, Card.Property(id, Color.RED, 3))}
        repeat(3) { id++; put(id, Card.Property(id, Color.YELLOW, 3))}
        repeat(4) { id++; put(id, Card.Property(id, Color.RAILROAD, 2))}
        repeat(2) { id++; put(id, Card.Property(id, Color.UTILITY, 2))}
        // Wild Properties
        repeat(1) {id++; put(id, Card.WildProperty(id, null, 4, setOf(Color.BLUE, Color.GREEN)))}
        repeat(1) {id++; put(id, Card.WildProperty(id, null, 1, setOf(Color.BROWN, Color.TURQOUISE)))}
        repeat(2) {id++; put(id, Card.WildProperty(id, null, 2, setOf(Color.MAGENTA, Color.ORANGE)))}
        repeat(1) {id++; put(id, Card.WildProperty(id, null, 4, setOf(Color.RAILROAD, Color.GREEN)))}
        repeat(1) {id++; put(id, Card.WildProperty(id, null, 4, setOf(Color.RAILROAD, Color.TURQOUISE)))}
        repeat(1) {id++; put(id, Card.WildProperty(id, null, 2, setOf(Color.RAILROAD, Color.UTILITY)))}
        repeat(2) {id++; put(id, Card.WildProperty(id, null, 3, setOf(Color.RED, Color.YELLOW)))}
        repeat(2) {id++; put(id, Card.WildProperty(id, null, null, Color.entries.toSet()))}
        // Money cards
        repeat(6) { id++; put(id, Card.Money(id, 1))}
        repeat(5) { id++; put(id, Card.Money(id, 2))}
        repeat(3) { id++; put(id, Card.Money(id, 3))}
        repeat(3) { id++; put(id, Card.Money(id, 4))}
        repeat(2) { id++; put(id, Card.Money(id, 5))}
        repeat(1) { id++; put(id, Card.Money(id, 10))}
    }
}


