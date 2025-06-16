package com.roomservice

import io.lettuce.core.RedisClient
import io.lettuce.core.pubsub.RedisPubSubAdapter
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class RedisPubSubManager(private val redisClient: RedisClient) {
    private val connection : StatefulRedisPubSubConnection<String, String> = redisClient.connectPubSub()
    private val listeners = mutableMapOf<String, MutableList<Channel<String>>>()
    private val mutex = Mutex()

    init {
        connection.addListener(object : RedisPubSubAdapter<String, String>() {
            override fun message(channel: String, message: String) {
                listeners[channel]?.forEach { ch ->
                    ch.trySend(message).isSuccess
                }
            }
        })
    }

    suspend fun subscribe(channelName: String): Channel<String> {
        val newChannel = Channel<String>(Channel.UNLIMITED)

        mutex.withLock {
            val subscribers = listeners.getOrPut(channelName) { mutableListOf() }
            subscribers.add(newChannel)

            if (subscribers.size == 1) {
                // Only subscribe at Redis level if it's the first subscriber
                connection.async().subscribe(channelName)
            }
        }
        return newChannel
    }

    suspend fun unsubscribe(channelName: String, subscriberChannel: Channel<String>) {
        mutex.withLock {
            listeners[channelName]?.remove(subscriberChannel)
            subscriberChannel.close()

            if (listeners[channelName]?.isEmpty() == true) {
                connection.async().unsubscribe(channelName)
                listeners.remove(channelName)
            }
        }
    }
}