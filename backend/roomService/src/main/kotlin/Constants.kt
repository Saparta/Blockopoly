package com.roomservice

import io.github.cdimascio.dotenv.dotenv
import io.ktor.http.HttpStatusCode
import io.ktor.util.AttributeKey
import io.lettuce.core.RedisClient
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.async.RedisAsyncCommands

private val env = dotenv {
    directory = "."
}
val LETTUCE_REDIS_CLIENT_KEY = AttributeKey<RedisClient>(env["REDIS_CLIENT"])
val LETTUCE_REDIS_CONNECTION_KEY = AttributeKey<StatefulRedisConnection<String, String>>(env["REDIS_CONNECTION_KEY"])
val LETTUCE_REDIS_COMMANDS_KEY = AttributeKey<RedisAsyncCommands<String, String>>(env["REDIS_ASYNC_COMMANDS_KEY"])
val ROOM_FULL_STATUS = HttpStatusCode(420, "Room full")
object Constants {
    const val JOIN_CODE_ALPHABET = "23456789ABCDEFGHJKMNPQRSTUVWXYZabcdefghjkmnopqrstuvwxyz"
    const val PLAYER_TO_ROOM_PREFIX = "v1:p2r:"
    const val ROOM_TO_PLAYERS_PREFIX = "v1:r2p:"
    const val JOIN_CODE_TO_ROOM_PREFIX = "v1:j2r:"
    const val ROOM_TO_JOIN_CODE_PREFIX = "v1:r2j:"
    const val MAX_PLAYERS = 5
}