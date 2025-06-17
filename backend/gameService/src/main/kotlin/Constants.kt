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