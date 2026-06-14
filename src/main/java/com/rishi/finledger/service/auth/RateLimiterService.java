package com.rishi.finledger.service.auth;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import com.rishi.finledger.exception.RateLimitExceededException;

import org.springframework.beans.factory.annotation.Value; // ✅ CORRECT

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class RateLimiterService {

    private static final Logger log = LoggerFactory.getLogger(RateLimiterService.class);

    @Value("${rate.limit.max}")
    private int maxRequests;

    @Value("${rate.limit.window}")
    private long timeWindow;

    @Value("${rate.limit.global.max}")
    private int globalMaxRequests;

    private final Map<Long, Deque<Long>> userRequests = new ConcurrentHashMap<>();
    private final Deque<Long> globalTimestamps = new ArrayDeque<>();

    public void checkRateLimit(Long userId) {

        long now = System.currentTimeMillis();

        // GLOBAL LIMIT
        synchronized (globalTimestamps) {

            while (!globalTimestamps.isEmpty() &&
                    now - globalTimestamps.peekFirst() > timeWindow) {
                globalTimestamps.pollFirst();
            }

            if (globalTimestamps.size() >= globalMaxRequests) {
                log.warn("GLOBAL rate limit exceeded | userId={}", userId);
                throw new RateLimitExceededException("System busy. Try again later.");
            }
        }

        // USER LIMIT
        Deque<Long> timestamps = userRequests.computeIfAbsent(userId, k -> new ArrayDeque<>());

        synchronized (timestamps) {

            while (!timestamps.isEmpty() &&
                    now - timestamps.peekFirst() > timeWindow) {
                timestamps.pollFirst();
            }

            if (timestamps.size() >= maxRequests) {

                Long first = timestamps.peekFirst();
                long retryAfterSec = (first == null) ? 1 : (timeWindow - (now - first)) / 1000;

                log.warn("Rate limit exceeded | userId={} | retryAfter={}s",
                        userId, retryAfterSec);

                throw new RateLimitExceededException(
                        "Too many requests. Try again after " + retryAfterSec + " seconds");
            }
            // record ONLY after passing checks
            timestamps.addLast(now);
        }
        // ✅ GLOBAL ADD (only after success)
        synchronized (globalTimestamps) {
            globalTimestamps.addLast(now);
        }
    }
}