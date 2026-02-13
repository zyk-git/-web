package com.example.giftbook.util;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitStore {
    private final Map<String, Deque<Long>> ipHits = new ConcurrentHashMap<>();

    // 同一 IP 1 分钟最多 3 次
    public boolean allow(String ip) {
        long now = Instant.now().toEpochMilli();
        long window = 60_000;
        Deque<Long> queue = ipHits.computeIfAbsent(ip, k -> new ArrayDeque<>());
        synchronized (queue) {
            while (!queue.isEmpty() && now - queue.peekFirst() > window) {
                queue.pollFirst();
            }
            if (queue.size() >= 3) {
                return false;
            }
            queue.addLast(now);
            return true;
        }
    }
}
