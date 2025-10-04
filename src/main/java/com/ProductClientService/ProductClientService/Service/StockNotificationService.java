package com.ProductClientService.ProductClientService.Service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ProductClientService.ProductClientService.Model.ProductVariant;
import com.ProductClientService.ProductClientService.Model.StockNotification;
import com.ProductClientService.ProductClientService.Repository.ProductVariantRepository;
import com.ProductClientService.ProductClientService.Repository.StockNotificationRepository;
import java.util.*;

@Service
@RequiredArgsConstructor
public class StockNotificationService implements StockObservable {

    private final StockNotificationRepository stockNotificationRepository;
    private final ProductVariantRepository productVariantRepository;

    private final List<StockObserver> observers = new ArrayList<>();

    @Override
    public void addObserver(StockObserver observer) {
        observers.add(observer);
    }

    @Override
    public void removeObserver(StockObserver observer) {
        observers.remove(observer);
    }

    @Override
    public void notifyObservers(ProductVariant variant, List<String> emails) {
        for (StockObserver observer : observers) {
            observer.update(variant, emails);
        }
    }

    /**
     * User subscribes for notifications
     */
    public void subscribeForStock(UUID variantId, String email) {
        ProductVariant variant = productVariantRepository.findById(variantId)
                .orElseThrow(() -> new RuntimeException("Variant not found"));

        StockNotification notification = new StockNotification();
        notification.setEmail(email);
        notification.setProductVariant(variant);
        stockNotificationRepository.save(notification);
    }

    /**
     * When stock is updated, notify waiting users
     */
    @Transactional
    public void updateStock(UUID variantId, int newStock) {
        ProductVariant variant = productVariantRepository.findById(variantId)
                .orElseThrow(() -> new RuntimeException("Variant not found"));

        variant.setStock(newStock);
        productVariantRepository.save(variant);

        if (newStock > 0) {
            List<StockNotification> notifications = stockNotificationRepository
                    .findByProductVariantAndNotifiedFalse(variant);

            List<String> emails = notifications.stream().map(StockNotification::getEmail).toList();

            // Notify observers (like Email service)
            notifyObservers(variant, emails);

            // Mark as notified
            notifications.forEach(n -> n.setNotified(true));
            stockNotificationRepository.saveAll(notifications);
        }
    }
}

// nkhiuhiuef uhihiuf hhuihf hihiufdbbhbjfbhjkjujiuu huihuihuihui huhui