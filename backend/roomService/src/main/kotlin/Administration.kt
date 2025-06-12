package com.roomservice

import io.ktor.server.plugins.ratelimit.*
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.util.AttributeKey
import kotlin.time.Duration.Companion.seconds
import io.lettuce.core.RedisClient
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.async.RedisAsyncCommands
import io.github.cdimascio.dotenv.dotenv
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.ApplicationStopping
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.origin

private val env = dotenv {
    directory = "."
}
val LettuceRedisClientKey = AttributeKey<RedisClient>(env["REDIS_CLIENT"])
val LettuceRedisConnectionKey = AttributeKey<StatefulRedisConnection<String, String>>(env["REDIS_CONNECTION_KEY"])
val LettuceRedisAsyncCommandsKey = AttributeKey<RedisAsyncCommands<String, String>>(env["REDIS_ASYNC_COMMANDS_KEY"])


fun Application.configureAdministration() {
    install(ContentNegotiation) {
        json()
    }

    install(RateLimit) {
        global {
            rateLimiter(limit = 2, refillPeriod = 10.seconds)
            requestKey { applicationCall -> applicationCall.request.origin.remoteHost }
        }
    }
    val redisHost = environment.config.propertyOrNull("ktor.redis.host")?.getString() ?: "localhost"
    val redisPort = environment.config.propertyOrNull("ktor.redis.port")?.getString()?.toIntOrNull() ?: 6379
    val redisUri = "redis://$redisHost:$redisPort"

    val redisClient = RedisClient.create(redisUri)
    val connection: StatefulRedisConnection<String, String> = redisClient.connect()
    val asyncCommands: RedisAsyncCommands<String, String> = connection.async()

    attributes.put(LettuceRedisClientKey, redisClient)
    attributes.put(LettuceRedisConnectionKey, connection)
    attributes.put(LettuceRedisAsyncCommandsKey, asyncCommands)

    monitor.subscribe(ApplicationStopping) {
        connection.close()
        redisClient.shutdown()
        monitor.unsubscribe(ApplicationStopping) {}
    }
}
