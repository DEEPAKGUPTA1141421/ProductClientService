package com.ProductClientService.ProductClientService.Service.session;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

/**
 * Per-session ring buffer of the last N product clicks, held in Redis.
 * Feeds the real-time reranker: the serving layer reads this list and
 * computes a session embedding on every /reco/for-you call so that a
 * freshly-clicked product influences results within seconds.
 *
 * Key:  sess:clicks:{sessionId}   (LIST)
 * TTL:  30 minutes, slid on every write (matches typical session length).
 * Cap:  50 entries — older entries trimmed; anything beyond adds no signal.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SessionClickBufferService {

    private static final String KEY_PREFIX = "sess:clicks:";
    private static final long   MAX_ENTRIES = 50;
    private static final Duration TTL = Duration.ofMinutes(30);

    private final StringRedisTemplate redis;

    public void pushClick(UUID sessionId, UUID productId) {
        if (sessionId == null || productId == null) return;
        String key = KEY_PREFIX + sessionId;
        try {
            redis.opsForList().leftPush(key, productId.toString());
            redis.opsForList().trim(key, 0, MAX_ENTRIES - 1);
            redis.expire(key, TTL);
        } catch (Exception e) {
            log.warn("Session click buffer write failed for session={}: {}", sessionId, e.getMessage());
        }
    }

    public List<String> recentClicks(UUID sessionId, int limit) {
        if (sessionId == null) return List.of();
        String key = KEY_PREFIX + sessionId;
        try {
            List<String> vals = redis.opsForList().range(key, 0, Math.max(0, limit - 1));
            return vals != null ? vals : List.of();
        } catch (Exception e) {
            log.warn("Session click buffer read failed for session={}: {}", sessionId, e.getMessage());
            return List.of();
        }
    }
}
