package com.roomservice

import io.github.flaxoos.ktor.server.plugins.ratelimiter.RateLimiting
import io.github.flaxoos.ktor.server.plugins.ratelimiter.implementations.TokenBucket
import io.ktor.server.application.Application
import io.ktor.server.application.install
import kotlin.time.Duration.Companion.seconds

fun Application.configureAdministration() {
    install(RateLimiting) {
        rateLimiter {
            type = TokenBucket::class
            capacity = 2
            rate = 10.seconds}
    }
}
