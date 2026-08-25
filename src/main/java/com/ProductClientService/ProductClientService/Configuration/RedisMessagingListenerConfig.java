package com.ProductClientService.ProductClientService.Configuration;

import com.ProductClientService.ProductClientService.Service.kafka.InteractionEventConsumer;
import com.ProductClientService.ProductClientService.Service.kafka.ProductIndexerConsumer;
import com.ProductClientService.ProductClientService.Service.kafka.ProductMetricsConsumer;
import com.ProductClientService.ProductClientService.Service.kafka.ReviewEventConsumer;
import com.ProductClientService.ProductClientService.Service.kafka.SearchIntentIndexerConsumer;
import com.ProductClientService.ProductClientService.Service.kafka.ShopIndexerConsumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

/**
 * Subscribes each event handler to a Redis pub/sub channel named after the
 * Kafka topic it replaces. No consumer groups here: every subscriber attached
 * to a channel gets every message, which is exactly how "product.live" having
 * two independent Kafka consumer groups behaved — so nothing changes semantically.
 *
 * Caveats vs. Kafka: no durability/replay (a subscriber that's down misses the
 * message) and no partition-key ordering. Acceptable for single-instance/dev
 * hosting; switch app.messaging.provider back to "kafka" before that matters.
 *
 * Mirrors KafkaMessagingListenerConfig's topic table; keep the two in sync
 * when adding a new event.
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "app.messaging", name = "provider", havingValue = "redis")
public class RedisMessagingListenerConfig {

    private final ProductMetricsConsumer metricsConsumer;
    private final ShopIndexerConsumer shopIndexerConsumer;
    private final ProductIndexerConsumer productIndexerConsumer;
    private final SearchIntentIndexerConsumer searchIntentIndexerConsumer;
    private final ReviewEventConsumer reviewEventConsumer;
    private final InteractionEventConsumer interactionEventConsumer;

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(RedisConnectionFactory connectionFactory) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);

        subscribe(container, "product.viewed", metricsConsumer::handleProductViewed);
        subscribe(container, "product.cart_added", metricsConsumer::handleCartAdded);
        subscribe(container, "product.wishlisted", metricsConsumer::handleWishlisted);
        subscribe(container, "order.completed", metricsConsumer::handleOrderCompleted);
        subscribe(container, "order.returned", metricsConsumer::handleOrderReturned);
        subscribe(container, "seller.live", shopIndexerConsumer::handleSellerLive);
        subscribe(container, "product.live", productIndexerConsumer::handleProductLive);
        subscribe(container, "product.live", searchIntentIndexerConsumer::handleProductLive);
        subscribe(container, "review.submit.requested", reviewEventConsumer::handleReviewSubmitRequested);
        subscribe(container, "review.helpful", reviewEventConsumer::handleReviewHelpful);
        subscribe(container, "user.interaction", interactionEventConsumer::handleInteraction);

        log.info("Redis pub/sub messaging listeners started (app.messaging.provider=redis)");
        return container;
    }

    private void subscribe(RedisMessageListenerContainer container, String channel, Consumer<String> handler) {
        container.addMessageListener(
                (message, pattern) -> handler.accept(new String(message.getBody(), StandardCharsets.UTF_8)),
                new ChannelTopic(channel));
    }
}
