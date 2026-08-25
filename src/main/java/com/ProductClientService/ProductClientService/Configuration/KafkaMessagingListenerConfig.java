package com.ProductClientService.ProductClientService.Configuration;

import com.ProductClientService.ProductClientService.Service.kafka.InteractionEventConsumer;
import com.ProductClientService.ProductClientService.Service.kafka.ProductIndexerConsumer;
import com.ProductClientService.ProductClientService.Service.kafka.ProductMetricsConsumer;
import com.ProductClientService.ProductClientService.Service.kafka.ReviewEventConsumer;
import com.ProductClientService.ProductClientService.Service.kafka.SearchIntentIndexerConsumer;
import com.ProductClientService.ProductClientService.Service.kafka.ShopIndexerConsumer;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.AcknowledgingMessageListener;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

/**
 * Wires each topic to its handler(s) as a raw (non-@KafkaListener) container,
 * so no consumer starts — and no connection to a broker is attempted — unless
 * app.messaging.provider=kafka. Mirrors RedisMessagingListenerConfig's topic
 * table; keep the two in sync when adding a new event.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "app.messaging", name = "provider", havingValue = "kafka", matchIfMissing = true)
public class KafkaMessagingListenerConfig {

    private final ConsumerFactory<String, String> consumerFactory;
    private final ProductMetricsConsumer metricsConsumer;
    private final ShopIndexerConsumer shopIndexerConsumer;
    private final ProductIndexerConsumer productIndexerConsumer;
    private final SearchIntentIndexerConsumer searchIntentIndexerConsumer;
    private final ReviewEventConsumer reviewEventConsumer;
    private final InteractionEventConsumer interactionEventConsumer;

    @PostConstruct
    public void startListeners() {
        listen("product.viewed", "product-metrics-group", metricsConsumer::handleProductViewed);
        listen("product.cart_added", "product-metrics-group", metricsConsumer::handleCartAdded);
        listen("product.wishlisted", "product-metrics-group", metricsConsumer::handleWishlisted);
        listen("order.completed", "product-metrics-group", metricsConsumer::handleOrderCompleted);
        listen("order.returned", "product-metrics-group", metricsConsumer::handleOrderReturned);
        listen("seller.live", "shop-es-indexer-group", shopIndexerConsumer::handleSellerLive);
        listen("product.live", "product-es-indexer-group", productIndexerConsumer::handleProductLive);
        listen("product.live", "search-intent-indexer-group", searchIntentIndexerConsumer::handleProductLive);
        listen("review.submit.requested", "review-events-group", reviewEventConsumer::handleReviewSubmitRequested);
        listen("review.helpful", "review-events-group", reviewEventConsumer::handleReviewHelpful);
        listen("user.interaction", "interaction-persist-group", interactionEventConsumer::handleInteraction);
        log.info("Kafka messaging listeners started (app.messaging.provider=kafka)");
    }

    private void listen(String topic, String groupId, Consumer<String> handler) {
        ContainerProperties containerProps = new ContainerProperties(topic);
        containerProps.setGroupId(groupId);
        containerProps.setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        containerProps.setMessageListener((AcknowledgingMessageListener<String, String>) (record, ack) -> {
            try {
                handler.accept(record.value());
            } finally {
                if (ack != null) ack.acknowledge();
            }
        });

        ConcurrentMessageListenerContainer<String, String> container =
                new ConcurrentMessageListenerContainer<>(consumerFactory, containerProps);
        container.setBeanName(topic + "." + groupId);
        container.setConcurrency(1);
        container.start();
    }
}
