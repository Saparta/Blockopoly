package com.roomservice

import io.github.cdimascio.dotenv.dotenv
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
val PUBSUB_MANAGER_KEY = AttributeKey<RedisPubSubManager>("PUBSUB_MANAGER_KEY")
const val JOIN_CODE_ALPHABET = "23456789ABCDEFGHJKMNPQRSTUVWXYZabcdefghjkmnopqrstuvwxyz"
const val PLAYER_TO_NAME_PREFIX = "v1:p2n:"
const val PLAYER_TO_ROOM_PREFIX = "v1:p2r:"
const val ROOM_TO_PLAYERS_PREFIX = "v1:r2p:"
const val JOIN_CODE_TO_ROOM_PREFIX = "v1:j2r:"
const val ROOM_TO_JOIN_CODE_PREFIX = "v1:r2j:"
const val ROOM_START_STATUS_PREFIX = "v1:rss:"
const val JOIN_CODE_SIZE = 6
const val MAX_PLAYERS = 5L
enum class RoomBroadcastType {
    INITIAL,
    JOIN,
    LEAVE,
    CLOSED,
    HOST,
    RECONNECT,
    ERROR,
}
enum class ErrorType {
    BAD_REQUEST,
    SERVICE_UNAVAILABLE,
    INTERNAL_SERVER_ERROR,
    ROOM_NOT_FOUND,
    ROOM_FULL,
    ROOM_ALREADY_STARTED
}
const val ROOM_BROADCAST_TYPE_DELIMITER = "#"
const val ROOM_BROADCAST_MSG_DELIMITER = ":"