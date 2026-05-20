package com.ProductClientService.ProductClientService.network;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DeliveryInventoryFeignConfig {

    @Value("${delivery.client.id}")
    private String clientId;

    @Value("${delivery.client.password}")
    private String clientPassword;

    @Bean
    public RequestInterceptor deliveryClientCredentialInterceptor() {
        return template -> {
            template.header("X-Client-Id", clientId);
            template.header("X-Client-Password", clientPassword);
        };
    }
}
