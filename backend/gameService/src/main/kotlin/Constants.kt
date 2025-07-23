package com.gameservice

import com.gameservice.models.ActionType
import com.gameservice.models.Card
import com.gameservice.models.Color
import com.gameservice.models.createCardMapping

const val ROOM_START_CHANNEL = "START"
const val NUM_COMPLETE_SETS_TO_WIN = 3
const val ROOM_BROADCAST_MSG_DELIMITER = ":"
val cardMapping = createCardMapping()
val deck = cardMapping.values
const val MAX_CARDS_PER_TURN = 3
const val INITIAL_DRAW_COUNT = 5
const val BIRTHDAY_PAYMENT_AMOUNT = 2
const val DEBT_COLLECTOR_PAYMENT_AMOUNT = 5
val DEVELOPMENT_ACTION_CARDS = arrayOf(ActionType.HOUSE, ActionType.HOTEL)
const val FAKE_CARD_ID = 999
val FAKE_CARD = Card.Action(FAKE_CARD_ID, 0, ActionType.PASS_GO)
val colorToRent = mapOf(
    Color.BLUE      to listOf(3, 8),
    Color.GREEN     to listOf(2, 4, 7),
    Color.BROWN     to listOf(1, 2),
    Color.TURQOUISE to listOf(1, 2, 3),
    Color.ORANGE    to listOf(1, 3, 5),
    Color.MAGENTA   to listOf(1, 2, 4),
    Color.RAILROAD  to listOf(1, 2, 3, 4),
    Color.UTILITY   to listOf(1, 2),
    Color.YELLOW    to listOf(2, 4, 6),
    Color.RED       to listOf(2, 3, 6))