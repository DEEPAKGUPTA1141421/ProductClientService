package com.ProductClientService.ProductClientService.Service;

/** Thrown by {@link ShopSearchService#nearby} when Elasticsearch cannot be reached or errors out. */
public class ElasticsearchUnavailableException extends RuntimeException {
    public ElasticsearchUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
