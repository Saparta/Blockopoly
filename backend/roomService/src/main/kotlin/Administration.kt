package com.roomservice

import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopping
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.origin
import io.ktor.server.plugins.ratelimit.RateLimit
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respondText
import io.ktor.server.sse.SSE
import io.lettuce.core.RedisClient
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.async.RedisAsyncCommands
import kotlin.time.Duration.Companion.seconds

fun Application.configureAdministration() {
    install(ContentNegotiation) {
        json()
    }

    install(SSE) {}

    install(RateLimit) {
        global {
            rateLimiter(limit = 5, refillPeriod = 10.seconds)
            requestKey { applicationCall -> applicationCall.request.origin.remoteAddress }
        }
    }

    install(StatusPages) {
        status(HttpStatusCode.TooManyRequests) { call, status ->
            val retryAfter = call.response.headers["Retry-After"]
            call.respondText(text = "429: Too many requests. Wait for $retryAfter seconds.", status = status)
        }
    }

    val redisHost = environment.config.propertyOrNull("ktor.redis.host")?.getString() ?: "localhost"
    val redisPort = environment.config.propertyOrNull("ktor.redis.port")?.getString()?.toIntOrNull() ?: 6379
    val redisUri = "redis://$redisHost:$redisPort"

    val redisClient = RedisClient.create(redisUri)
    val connection: StatefulRedisConnection<String, String> = redisClient.connect()
    val asyncCommands: RedisAsyncCommands<String, String> = connection.async()

    attributes.put(LETTUCE_REDIS_CLIENT_KEY, redisClient)
    attributes.put(LETTUCE_REDIS_CONNECTION_KEY, connection)
    attributes.put(LETTUCE_REDIS_COMMANDS_KEY, asyncCommands)
    attributes.put(PUBSUB_MANAGER_KEY, RedisPubSubManager(redisClient))

    monitor.subscribe(ApplicationStopping) {
        connection.close()
        redisClient.shutdown()
        monitor.unsubscribe(ApplicationStopping) {}
    }
}
