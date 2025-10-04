package com.ProductClientService.ProductClientService.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ProductClientService.ProductClientService.Model.ProductVariant;
import com.ProductClientService.ProductClientService.Model.StockNotification;

import java.util.List;
import java.util.UUID;

@Repository
public interface StockNotificationRepository extends JpaRepository<StockNotification, UUID> {
    List<StockNotification> findByProductVariantAndNotifiedFalse(ProductVariant variant);
}
// huiyid huiyuiyd hguyhuidhgyuhid jghuyjgdgj