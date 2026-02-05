package io.ngrabner.task_tracker_api.security;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class FixedWindowRateLimiter {

    private static class Bucket {
        volatile long windowStartEpochSec;
        final AtomicInteger count = new AtomicInteger(0);

        Bucket(long windowStartEpochSec) {
            this.windowStartEpochSec = windowStartEpochSec;
        }
    }

    private final long windowSeconds;
    private final int maxRequests;
    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    public FixedWindowRateLimiter(long windowSeconds, int maxRequests) {
        this.windowSeconds = windowSeconds;
        this.maxRequests = maxRequests;
    }

    public boolean allow(String key) {
        long now = Instant.now().getEpochSecond();
        long windowStart = now - (now % windowSeconds);

        Bucket bucket = buckets.computeIfAbsent(key, k -> new Bucket(windowStart));

        // window rotated?
        if (bucket.windowStartEpochSec != windowStart) {
            bucket.windowStartEpochSec = windowStart;
            bucket.count.set(0);
        }

        return bucket.count.incrementAndGet() <= maxRequests;
    }
}
