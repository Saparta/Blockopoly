package com.roomservice

import io.ktor.server.application.ApplicationEnvironment
import io.lettuce.core.RedisClient
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.async.RedisAsyncCommands
import org.koin.dsl.module
import org.koin.dsl.onClose

fun redisModule(environment: ApplicationEnvironment) = module {
    single(createdAtStart = true) {
        val redisHost = environment.config.propertyOrNull("ktor.redis.host")?.getString() ?: "localhost"
        val redisPort = environment.config.propertyOrNull("ktor.redis.port")?.getString()?.toIntOrNull() ?: 6379
        val redisUri = "redis://$redisHost:$redisPort"
        RedisClient.create(redisUri)
    } onClose { client ->
        client?.shutdown()
    }

    single(createdAtStart = true) {
        val client: RedisClient = get()
        client.connect()
    } onClose { conn ->
        conn?.close()
    }

    factory<RedisAsyncCommands<String, String>> {
        val conn: StatefulRedisConnection<String, String> = get()
        conn.async()
    }

    single {
        val client: RedisClient = get()
        RedisPubSubManager(client)
    }
}