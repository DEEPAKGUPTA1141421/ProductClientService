package com.ProductClientService.ProductClientService.Service;

import com.ProductClientService.ProductClientService.Service.messaging.EventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class KafkaProducerService {

    private final EventPublisher eventPublisher;

    public void sendMessage(String topic, Object obj) {
        eventPublisher.publish(topic, String.valueOf(obj));
    }
}
