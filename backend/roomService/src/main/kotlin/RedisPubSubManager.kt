package com.roomservice

import io.lettuce.core.RedisClient
import io.lettuce.core.pubsub.RedisPubSubAdapter
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection
import kotlinx.coroutines.channels.Channel
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

class RedisPubSubManager(private val redisClient: RedisClient) {
    private val connection : StatefulRedisPubSubConnection<String, String> = redisClient.connectPubSub()
    private val listeners = ConcurrentHashMap<String, CopyOnWriteArrayList<Channel<String>>>()

    init {
        connection.addListener(object : RedisPubSubAdapter<String, String>() {
            override fun message(channel: String, message: String) {
                listeners[channel]?.forEach { ch ->
                    ch.trySend(message).isSuccess
                }
            }
        })
    }

    fun subscribe(channelName: String): Channel<String> {
        val newChannel = Channel<String>(Channel.UNLIMITED)

        val subs = listeners.computeIfAbsent(channelName) { CopyOnWriteArrayList() }
        subs.add(newChannel)

        if (subs.size == 1) {
            connection.async().subscribe(channelName)
        }
        return newChannel
    }

    fun unsubscribe(channelName: String, subscriberChannel: Channel<String>) {
        val subs = listeners[channelName] ?: return
        subs.remove(subscriberChannel)
        subscriberChannel.close()

        if (subs.isEmpty()) {
            listeners.remove(channelName)
            connection.async().unsubscribe(channelName)
        }
    }
}