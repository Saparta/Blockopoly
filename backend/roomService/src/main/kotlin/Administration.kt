package com.roomservice

import io.github.flaxoos.ktor.server.plugins.ratelimiter.RateLimiting
import io.github.flaxoos.ktor.server.plugins.ratelimiter.implementations.TokenBucket
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.util.AttributeKey
import kotlin.time.Duration.Companion.seconds
import io.lettuce.core.RedisClient
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.async.RedisAsyncCommands
import io.github.cdimascio.dotenv.dotenv
import io.ktor.server.application.ApplicationStopping

private val env = dotenv {
    directory = "."
}
val LettuceRedisClientKey = AttributeKey<RedisClient>(env["REDIS_CLIENT"])
val LettuceRedisConnectionKey = AttributeKey<StatefulRedisConnection<String, String>>(env["REDIS_CONNECTION_KEY"])
val LettuceRedisAsyncCommandsKey = AttributeKey<RedisAsyncCommands<String, String>>(env["REDIS_ASYNC_COMMANDS_KEY"])


fun Application.configureAdministration() {
    install(RateLimiting) {
        rateLimiter {
            type = TokenBucket::class
            capacity = 2
            rate = 10.seconds}
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
