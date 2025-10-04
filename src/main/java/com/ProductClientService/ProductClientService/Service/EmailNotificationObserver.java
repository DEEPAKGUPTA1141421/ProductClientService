package com.ProductClientService.ProductClientService.Service;

import java.util.List;

import org.springframework.stereotype.Component;

import com.ProductClientService.ProductClientService.Model.ProductVariant;

@Component
public class EmailNotificationObserver implements StockObserver {

    @Override
    public void update(ProductVariant variant, List<String> emails) {
        // You can integrate with Mail Service here
        for (String email : emails) {
            System.out.println("Sending email to: " + email +
                    " -> Product " + variant.getSku() + " is back in stock!");
        }
    }
}
