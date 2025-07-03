package com.gameservice

import com.gameservice.models.ActionType
import com.gameservice.models.Card
import com.gameservice.models.Color
import com.gameservice.models.createCardMapping
import io.github.cdimascio.dotenv.dotenv
import io.ktor.util.AttributeKey
import io.lettuce.core.RedisClient
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.async.RedisAsyncCommands

private val env = dotenv {
    directory = "."
}
val REDIS_CLIENT_KEY = AttributeKey<RedisClient>(env["REDIS_CLIENT"])
val REDIS_CONNECTION_KEY = AttributeKey<StatefulRedisConnection<String, String>>(env["REDIS_CONNECTION_KEY"])
val REDIS_COMMANDS_KEY = AttributeKey<RedisAsyncCommands<String, String>>(env["REDIS_ASYNC_COMMANDS_KEY"])
const val SECONDS_IN_DAY = 86400
const val PLAYER_TO_NAME_PREFIX = "v1:p2n:"
const val PLAYER_TO_ROOM_PREFIX = "v1:p2r:"
const val ROOM_TO_PLAYERS_PREFIX = "v1:r2p:"
const val ROOM_START_STATUS_PREFIX = "v1:rss:"
enum class ErrorType {
    BAD_REQUEST,
    SERVICE_UNAVAILABLE,
    INTERNAL_SERVER_ERROR,
}
val cardMapping = createCardMapping()
val deck = cardMapping.values
const val MAX_CARDS_PER_TURN = 3
const val INITIAL_DRAW_COUNT = 5
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