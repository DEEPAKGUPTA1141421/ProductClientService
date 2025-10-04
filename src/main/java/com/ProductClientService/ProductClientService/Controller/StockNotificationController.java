package com.ProductClientService.ProductClientService.Controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import com.ProductClientService.ProductClientService.Service.EmailNotificationObserver;
import com.ProductClientService.ProductClientService.Service.StockNotificationService;

import jakarta.annotation.PostConstruct;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/stock")
public class StockNotificationController {

    private final StockNotificationService stockNotificationService;
    private final EmailNotificationObserver emailObserver;

    public StockNotificationController(StockNotificationService stockNotificationService,
            EmailNotificationObserver emailObserver) {
        this.stockNotificationService = stockNotificationService;
        this.emailObserver = emailObserver;
    }

    @PostConstruct
    public void init() {
        stockNotificationService.addObserver(emailObserver);
    }

    @PostMapping("/subscribe/{variantId}")
    public String subscribe(@PathVariable UUID variantId, @RequestParam String email) {
        stockNotificationService.subscribeForStock(variantId, email);
        return "Subscribed " + email + " for stock alerts.";
    }

    @PostMapping("/updateStock/{variantId}")
    public String updateStock(@PathVariable UUID variantId, @RequestParam int stock) {
        stockNotificationService.updateStock(variantId, stock);
        return "Stock updated!";
    }
}

// khiyiydekjyiuydf hhihud hiuhuihiu hiui hu9iiyuyygggcb byu7d