package com.gameservice

import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopping
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.pingPeriod
import io.ktor.server.websocket.timeout
import io.lettuce.core.RedisClient
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.async.RedisAsyncCommands
import kotlin.time.Duration.Companion.seconds

fun Application.configureAdministration() {
    install(ContentNegotiation) {
        json()
    }
    install(WebSockets) {
        pingPeriod = 15.seconds
        timeout = 15.seconds
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }

    val redisHost = environment.config.propertyOrNull("ktor.redis.host")?.getString() ?: "localhost"
    val redisPort = environment.config.propertyOrNull("ktor.redis.port")?.getString()?.toIntOrNull() ?: 6379
    val redisUri = "redis://$redisHost:$redisPort"

    val redisClient = RedisClient.create(redisUri)
    val connection: StatefulRedisConnection<String, String> = redisClient.connect()
    val asyncCommands: RedisAsyncCommands<String, String> = connection.async()

    attributes.put(REDIS_CLIENT_KEY, redisClient)
    attributes.put(REDIS_CONNECTION_KEY, connection)
    attributes.put(REDIS_COMMANDS_KEY, asyncCommands)

    monitor.subscribe(ApplicationStopping) {
        connection.close()
        redisClient.shutdown()
        monitor.unsubscribe(ApplicationStopping) {}
    }
}