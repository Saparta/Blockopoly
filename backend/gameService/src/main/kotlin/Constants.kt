package com.gameservice

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
enum class MessageType {
    LEAVE,
    STATE,
}

val cardMapping = createCardMapping()
val deck = cardMapping.values
const val HOUSE_ADDITIONAL_RENT = 3
const val HOTEL_ADDITIONAL_RENT = 4
const val INITIAL_DRAW_COUNT = 5
const val FAKE_CARD_ID = 999
val colorToRent = mapOf(
    Pair(Color.BLUE, listOf(3, 8)),
    Pair(Color.GREEN, listOf(2, 4, 7)),
    Pair(Color.BROWN, listOf(1, 2)),
    Pair(Color.TURQOUISE, listOf(1, 2, 3)),
    Pair(Color.ORANGE, listOf(1, 3, 5)),
    Pair(Color.MAGENTA, listOf(1, 2, 4)),
    Pair(Color.RAILROAD, listOf(1, 2, 3, 4)),
    Pair(Color.UTILITY, listOf(1, 2)),
    Pair(Color.YELLOW, listOf(2, 4, 6)),
    Pair(Color.RED, listOf(2, 3, 6)))