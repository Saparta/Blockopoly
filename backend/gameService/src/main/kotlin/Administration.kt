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
import io.lettuce.core.pubsub.RedisPubSubAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.ktor.ext.inject
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger
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
    install(Koin) {
        slf4jLogger()
        modules(redisModule(environment))
    }

    val redisClient : RedisClient by inject()

    monitor.subscribe(ApplicationStopping) {
        redisClient.shutdown()
        monitor.unsubscribe(ApplicationStopping) {}
    }

    CoroutineScope(Dispatchers.Default + SupervisorJob()).launch {
        val pubSub = redisClient.connectPubSub()
        pubSub.addListener(object : RedisPubSubAdapter<String, String>() {
            override fun message(channel: String?, message: String?) {
                if (channel == ROOM_START_CHANNEL) {
                    val msg = message?.split(ROOM_BROADCAST_MSG_DELIMITER) ?: return
                    val roomId = msg.first()
                    val players = msg.subList(1, msg.size)
                    ServerManager.addRoom(DealGame(roomId, players))
                }
            }
        })
        pubSub.async().subscribe(ROOM_START_CHANNEL)
    }
}