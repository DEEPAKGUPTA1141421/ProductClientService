package com.ProductClientService.ProductClientService.Model.listener.callback;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.ProductClientService.ProductClientService.Model.Product;
import com.ProductClientService.ProductClientService.Model.StandardProduct;
import com.ProductClientService.ProductClientService.Model.listener.event.ProductUpdatedEvent;
import com.ProductClientService.ProductClientService.Repository.ProductRepository;
import com.ProductClientService.ProductClientService.Repository.StandardProductRepository;

@Component
public class ProductUpdatedListener {

    private final StandardProductRepository standardRepo;
    private final ProductRepository productRepo;

    public ProductUpdatedListener(StandardProductRepository standardRepo,
            ProductRepository productRepo) {
        this.standardRepo = standardRepo;
        this.productRepo = productRepo;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleProductUpdate(ProductUpdatedEvent event) {
        System.out.println("✅ Handling ProductUpdatedEvent");

        Product product = event.getProduct();

        if (Boolean.TRUE.equals(product.getIsStandard()) && product.getStep() == Product.Step.LIVE) {
            try {
                System.out.println("product value" + product.getName() + " " + product.getDescription() + " " + " "
                        + product.getCategory() + " " + product.getBrand());
                StandardProduct standardProduct = new StandardProduct();
                standardProduct.setName(product.getName());
                standardProduct.setDescription(product.getDescription());

                standardProduct.setCategory(product.getCategory());
                standardProduct.setBrandEntity(product.getBrand());

                StandardProduct saved = standardRepo.save(standardProduct);
                System.out.println("✅ StandardProduct saved: " + saved);

            } catch (Exception e) {
                System.err.println("❌ Failed to save StandardProduct: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.out.println("⚠️ Product not standard/live, skipping StandardProduct creation.");
        }
    }
}

// hj njjjnnhm mkkmk kkkkkkbjhbgggggg gfgb