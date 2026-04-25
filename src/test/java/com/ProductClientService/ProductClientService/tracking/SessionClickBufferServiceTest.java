package com.ProductClientService.ProductClientService.tracking;

import com.ProductClientService.ProductClientService.Service.session.SessionClickBufferService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class SessionClickBufferServiceTest {

    private StringRedisTemplate redis;
    @SuppressWarnings("unchecked")
    private ListOperations<String, String> listOps;
    private SessionClickBufferService svc;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redis = mock(StringRedisTemplate.class);
        listOps = mock(ListOperations.class);
        when(redis.opsForList()).thenReturn(listOps);
        svc = new SessionClickBufferService(redis);
    }

    @Test
    void pushTrimsToCapAndSetsTtl() {
        UUID session = UUID.randomUUID();
        UUID product = UUID.randomUUID();

        svc.pushClick(session, product);

        String expectedKey = "sess:clicks:" + session;
        verify(listOps).leftPush(expectedKey, product.toString());

        ArgumentCaptor<Long> end = ArgumentCaptor.forClass(Long.class);
        verify(listOps).trim(eq(expectedKey), eq(0L), end.capture());
        assertTrue(end.getValue() == 49L,
                "List must be trimmed to MAX_ENTRIES=50 (0..49 inclusive)");

        verify(redis).expire(eq(expectedKey), eq(Duration.ofMinutes(30)));
    }

    @Test
    void nullInputsAreNoOps() {
        svc.pushClick(null, UUID.randomUUID());
        svc.pushClick(UUID.randomUUID(), null);
        verify(listOps, never()).leftPush(anyString(), anyString());
    }
}
