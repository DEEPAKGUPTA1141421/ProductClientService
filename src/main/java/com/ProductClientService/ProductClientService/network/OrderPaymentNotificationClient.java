package com.ProductClientService.ProductClientService.network;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.ProductClientService.ProductClientService.DTO.NotificationRequest;

@FeignClient(name = "notification", url = "${feign.client.orderpaymentnotification.url}")
public interface OrderPaymentNotificationClient {

    @PostMapping("/api/v1/kafka/send")
    ResponseEntity<?> createRider(@RequestBody NotificationRequest request);
}
