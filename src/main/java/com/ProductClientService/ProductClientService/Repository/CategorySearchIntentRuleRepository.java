package com.ProductClientService.ProductClientService.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.ProductClientService.ProductClientService.Model.CategorySearchIntentRule;
import java.util.UUID;
import java.util.List;

@Repository
public interface CategorySearchIntentRuleRepository extends JpaRepository<CategorySearchIntentRule, UUID> {
    List<CategorySearchIntentRule> findByCategoryId(UUID categoryId);

}
