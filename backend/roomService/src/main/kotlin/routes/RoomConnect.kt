package com.roomservice.routes

import com.roomservice.LETTUCE_REDIS_CLIENT_KEY
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond
import io.ktor.server.sse.ServerSSESession
import io.ktor.server.sse.heartbeat
import io.lettuce.core.pubsub.RedisPubSubAdapter
import kotlinx.coroutines.channels.Channel
import kotlin.time.Duration.Companion.seconds

suspend fun roomConnect(call: ApplicationCall, session: ServerSSESession) {
    session.heartbeat { period = 15.seconds }
    val roomId = call.parameters["roomId"] ?: return call.respond(HttpStatusCode.BadRequest)
    val pubSub = call.application.attributes[LETTUCE_REDIS_CLIENT_KEY].connectPubSub()

    val incoming = Channel<String>(Channel.UNLIMITED)
    pubSub.addListener(object : RedisPubSubAdapter<String, String>() {
        override fun message(channel: String, message: String) {
            incoming.trySend(message)
        }
    })

    val redis = pubSub.async()
    redis.subscribe(roomId)

    try {
        for (msg in incoming) {
            session.send(msg)
        }
    } finally {
        redis.unsubscribe(roomId)
        pubSub.closeAsync()
    }

}