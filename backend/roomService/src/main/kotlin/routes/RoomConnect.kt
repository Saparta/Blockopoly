package com.roomservice.routes

import com.roomservice.LETTUCE_REDIS_CLIENT_KEY
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond
import io.ktor.server.sse.ServerSSESession
import io.ktor.server.sse.heartbeat
import io.ktor.sse.ServerSentEvent
import io.lettuce.core.pubsub.RedisPubSubAdapter
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.future.await
import kotlin.time.Duration.Companion.seconds

suspend fun roomConnect(call: ApplicationCall, session: ServerSSESession) {
    session.heartbeat { period = 15.seconds }
    val roomCode = call.parameters["roomCode"] ?: return call.respond(HttpStatusCode.BadRequest)
    val connection = call.application.attributes[LETTUCE_REDIS_CLIENT_KEY].connectPubSub()

    val incoming = Channel<String>(Channel.UNLIMITED)
    val listener = object : RedisPubSubAdapter<String, String>() {
        override fun message(ch: String?, msg: String?) {
            call.application.environment.log.info("→ Received message on channel: $ch, msg: $msg")
            if (ch == roomCode && msg != null) {
                incoming.trySend(msg)
            }
        }
    }

    connection.addListener(listener)
    val redis = connection.async()
    redis.subscribe(roomCode).await()

    try {
        for (msg in incoming) {
            call.application.environment.log.info("Sending $msg")
            session.send(ServerSentEvent(msg))
        }
    } finally {
        call.application.environment.log.info("Closing redis pubSub")
        redis.unsubscribe(roomCode)
        connection.closeAsync()
    }

}