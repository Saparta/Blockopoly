package com.gameservice

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
object Constants {
    const val SECONDS_IN_DAY = 86400
    const val PLAYER_TO_NAME_PREFIX = "v1:p2n:"
    const val PLAYER_TO_ROOM_PREFIX = "v1:p2r:"
    const val ROOM_TO_PLAYERS_PREFIX = "v1:r2p:"
    const val JOIN_CODE_TO_ROOM_PREFIX = "v1:j2r:"
    const val ROOM_TO_JOIN_CODE_PREFIX = "v1:r2j:"
    const val ROOM_START_STATUS_PREFIX = "v1:rss:"
    enum class ErrorType {
        BAD_REQUEST,
        SERVICE_UNAVAILABLE,
        INTERNAL_SERVER_ERROR,
    }
}