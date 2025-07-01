package com.roomservice.models

import com.roomservice.RedisPubSubManager
import io.lettuce.core.api.async.RedisAsyncCommands

data class RedisConnections(val pubSubManager: RedisPubSubManager, val redis: RedisAsyncCommands<String, String>)