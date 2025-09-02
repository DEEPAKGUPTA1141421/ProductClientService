package com.ProductClientService.ProductClientService.Model.listener;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import com.ProductClientService.ProductClientService.Model.Product;
import com.ProductClientService.ProductClientService.Model.StandardProduct;
import com.ProductClientService.ProductClientService.Model.listener.event.ProductUpdatedEvent;

import jakarta.persistence.PostUpdate;

@Component
public class ProductEntityListener {

    private static ApplicationEventPublisher publisher;

    @Autowired
    public void setPublisher(ApplicationEventPublisher publisher) {
        ProductEntityListener.publisher = publisher;
    }

    @PostUpdate
    public void onPostUpdate(Product product) {
        // Don’t save anything here (still inside flush)
        // Just raise an event
        System.out.println("✅ Handling ProductUpdatedEvent");

        if (Boolean.TRUE.equals(product.getIsStandard()) && product.getStep() == Product.Step.LIVE) {
            try {
                System.out.println("product value" + product.getName() + " " + product.getDescription() + " " +
                        product.getProductImages() + " " + product.getCategory() + " " + product.getBrand());
                StandardProduct standardProduct = new StandardProduct();
                standardProduct.setName(product.getName());
                standardProduct.setDescription(product.getDescription());
                standardProduct.setProductImages(product.getProductImages());
                standardProduct.setCategory(product.getCategory());
                standardProduct.setBrandEntity(product.getBrand());

                System.out.println("✅ StandardProduct saved: ");

            } catch (Exception e) {
                System.err.println("❌ Failed to save StandardProduct: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.out.println("⚠️ Product not standard/live, skipping StandardProduct creation.");
        }
    }
}
