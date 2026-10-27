package com.cafe.backend.security;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class RateLimitService {

    private final StringRedisTemplate redisTemplate;

    public RateLimitService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean isAllowed(String key, int maxRequests, Duration window) {
        try {
            Long count = redisTemplate.opsForValue().increment(key);

            // Set the window on first hit, and repair keys that somehow lost their expiry
            Long ttl = redisTemplate.getExpire(key);
            if ((count != null && count == 1) || ttl == null || ttl < 0) {
                redisTemplate.expire(key, window);
            }

            return count != null && count <= maxRequests;
        } catch (Exception e) {
            // Redis down: don't stop the cafe from taking orders / logging in
            System.out.println("RATE LIMIT SKIPPED (Redis unavailable): " + e.getMessage());
            return true;
        }
    }
}