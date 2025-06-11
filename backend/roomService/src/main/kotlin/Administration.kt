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

val dotenv = dotenv()
val LettuceRedisClientKey = AttributeKey<RedisClient>(dotenv["LettuceRedisClient"])
val LettuceRedisConnectionKey = AttributeKey<StatefulRedisConnection<String, String>>(dotenv["LettuceRedisConnection"])
val LettuceRedisAsyncCommandsKey = AttributeKey<RedisAsyncCommands<String, String>>(dotenv["LettuceRedisAsyncCommands"])

fun Application.configureAdministration() {
    install(RateLimiting) {
        rateLimiter {
            type = TokenBucket::class
            capacity = 2
            rate = 10.seconds}
    }
    val redisHost = dotenv["REDIS_HOST"] ?: "localhost"
    val redisPort = dotenv["REDIS_PORT"] ?: 6379
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
