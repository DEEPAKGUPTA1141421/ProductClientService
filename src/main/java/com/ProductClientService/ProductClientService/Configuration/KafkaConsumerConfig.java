package com.ProductClientService.ProductClientService.Configuration;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * KafkaConsumerConfig
 * ────────────────────
 * Mirrors the SASL/SSL setup from KafkaConfig but for consumer side.
 *
 * Consumer group: "product-metrics-group"
 *   → If this service restarts, it resumes from where it left off.
 *   → Multiple instances of this service each get a partition subset.
 *
 * Concurrency: 3 threads per listener
 *   → Allows parallel processing of events across partitions.
 *
 * Ack mode: MANUAL_IMMEDIATE
 *   → We commit the offset only after MetricsWriterService successfully
 *     persists — prevents lost updates if the DB write fails mid-batch.
 */
@Configuration
@EnableKafka
public class KafkaConsumerConfig {

    private final KafkaConfig kafkaConfig;

    public KafkaConsumerConfig(KafkaConfig kafkaConfig) {
        this.kafkaConfig = kafkaConfig;
    }

    @Bean
    public ConsumerFactory<String, String> consumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaConfig.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "product-metrics-group");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

        // SASL/SSL — same as producer
        props.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_SSL");
        props.put(SaslConfigs.SASL_MECHANISM, "SCRAM-SHA-256");
        props.put(SaslConfigs.SASL_JAAS_CONFIG, kafkaConfig.getJaasConfig());
        props.put("ssl.endpoint.identification.algorithm", "");
        props.put("ssl.truststore.type", "jks");
        props.put("ssl.truststore.location", "client.truststore.jks");
        props.put("ssl.truststore.password", kafkaConfig.getTruststorePassword());

        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.setConcurrency(3);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        return factory;
    }
}
