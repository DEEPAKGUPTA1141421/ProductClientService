package com.ProductClientService.ProductClientService.Utils;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class RateLimiter {
   private final StringRedisTemplate redisTemplate;
    private static final int MAX_ATTEMPTS = 5;
    private static final Duration WINDOW = Duration.ofHours(1); // 1 hour window

    public RateLimiter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean allow(String phone) {
        String key = "otp:" + phone;

        // Increment atomically
        Long count = redisTemplate.opsForValue().increment(key);
        if (count == 1) {
            // first request, set expiration
            redisTemplate.expire(key, WINDOW);
        }

        return count <= MAX_ATTEMPTS;
    } 
}
