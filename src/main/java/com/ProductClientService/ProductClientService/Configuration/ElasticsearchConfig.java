package com.ProductClientService.ProductClientService.Configuration;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ElasticsearchConfig {

        @Value("${elasticsearch.url}")
        private String elasticsearchUrl;

        @Value("${elasticsearch.apiKey}")
        private String apiKey;

        public ElasticsearchConfig() {
                System.out.println("Api Craetion Failed");
        }

        @Bean

        public ElasticsearchClient elasticsearchClient() {
                RestClientBuilder builder = RestClient.builder(HttpHost.create(elasticsearchUrl))
                                .setHttpClientConfigCallback(new RestClientBuilder.HttpClientConfigCallback() {
                                        @Override
                                        public HttpAsyncClientBuilder customizeHttpClient(
                                                        HttpAsyncClientBuilder httpClientBuilder) {
                                                return httpClientBuilder.addInterceptorFirst(
                                                                (HttpRequestInterceptor) (request, context) -> {
                                                                        request.addHeader("Authorization",
                                                                                        "ApiKey " + apiKey);
                                                                });
                                        }
                                });

                RestClient restClient = builder.build();

                RestClientTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper());

                return new ElasticsearchClient(transport);
        }
}

// we arethe best and we willjhggh bhjb