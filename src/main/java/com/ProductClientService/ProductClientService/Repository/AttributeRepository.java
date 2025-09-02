package com.ProductClientService.ProductClientService.Repository;


import com.ProductClientService.ProductClientService.Model.Attribute;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AttributeRepository extends JpaRepository<Attribute, UUID> {
    boolean existsByName(String name);
}
