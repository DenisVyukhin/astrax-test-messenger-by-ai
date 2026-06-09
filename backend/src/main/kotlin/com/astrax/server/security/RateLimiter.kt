package com.astrax.server.security

import java.util.concurrent.ConcurrentHashMap

class RateLimiter(
    private val maxRequests: Int,
    private val windowMillis: Long
) {
    private data class Bucket(var count: Int, var resetAt: Long)

    private val buckets = ConcurrentHashMap<String, Bucket>()

    fun allow(key: String): Boolean {
        val now = System.currentTimeMillis()
        val bucket = buckets.compute(key) { _, current ->
            when {
                current == null -> Bucket(1, now + windowMillis)
                now > current.resetAt -> Bucket(1, now + windowMillis)
                else -> current.apply { count += 1 }
            }
        }
        return bucket != null && bucket.count <= maxRequests
    }
}
