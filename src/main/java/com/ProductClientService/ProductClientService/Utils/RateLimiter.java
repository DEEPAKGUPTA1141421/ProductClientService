package com.ProductClientService.ProductClientService.Utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class RateLimiter {

    private static final Logger log = LoggerFactory.getLogger(RateLimiter.class);

    private final StringRedisTemplate redisTemplate;
    private static final int MAX_ATTEMPTS = 5;
    private static final Duration WINDOW = Duration.ofHours(1); // 1 hour window

    public RateLimiter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean allow(String phone) {
        String key = "otp:" + phone;
        try {
            log.info("Checking rate limit for phone: {}", phone);

            // Increment atomically
            Long count = redisTemplate.opsForValue().increment(key);
            log.info("Current count for {}: {}", key, count);

            if (count == 1) {
                // first request, set expiration
                boolean expireSet = redisTemplate.expire(key, WINDOW);
                log.info("Expiration set for {}: {}", key, expireSet);
            }

            boolean allowed = count <= MAX_ATTEMPTS;
            log.info("Allow OTP for {}: {}", phone, allowed);
            return allowed;

        } catch (Exception e) {
            log.error("Error accessing Redis for key {}: {}", key, e.getMessage(), e);
            // fail-safe: allow the request if Redis is down, or return false depending on
            // your policy
            return false;
        }
    }
}
