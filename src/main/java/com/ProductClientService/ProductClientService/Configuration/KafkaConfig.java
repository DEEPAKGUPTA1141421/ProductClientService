package com.ProductClientService.ProductClientService.Configuration;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.*;

import jakarta.annotation.PostConstruct;

@Configuration
@EnableKafka
public class KafkaConfig {

    @Value("${kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${kafka.sasl.username}")
    private String saslUsername;

    @Value("${kafka.sasl.password}")
    private String saslPassword;

    @Value("${kafka.jaas.template}")
    private String jaasTemplate;

    @Value("${kafka.truststore.password}")
    private String truststorePassword;

    public String getBootstrapServers() {
        return bootstrapServers;
    }

    public String getSaslUsername() {
        return saslUsername;
    }

    public String getSaslPassword() {
        return saslPassword;
    }

    public String getTruststorePassword() {
        return truststorePassword;
    }

    public String getJaasConfig() {
        return String.format(jaasTemplate, saslUsername, saslPassword);
    }

    @PostConstruct
    public void checkConfig() {
        System.out.println("Bootstrap servers: " + bootstrapServers);
    }

    // ✅ Producer config
    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_SSL");
        props.put(SaslConfigs.SASL_MECHANISM, "SCRAM-SHA-256");
        props.put(SaslConfigs.SASL_JAAS_CONFIG, getJaasConfig());

        props.put("ssl.endpoint.identification.algorithm", "");
        props.put("ssl.truststore.type", "jks");
        props.put("ssl.truststore.location", "client.truststore.jks");
        props.put("ssl.truststore.password", truststorePassword);

        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
}
// lniuhiu hjbgyjuyhgy njmk mk jkkop