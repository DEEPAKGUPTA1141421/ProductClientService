package com.ProductClientService.ProductClientService.Repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.ProductClientService.ProductClientService.Model.ProductAttribute;

import org.springframework.data.repository.query.Param;

import org.springframework.stereotype.Repository;

@Repository
public interface ProductAttributeRepository extends JpaRepository<ProductAttribute, UUID> {

  @Query("""
          SELECT pa
          FROM ProductAttribute pa
          JOIN pa.categoryAttribute ca
          WHERE pa.product.id = :productId
            AND ca.isImageAttribute = true
      """)
  List<ProductAttribute> findImageAttributesByProductId(@Param("productId") UUID productId);
}

// khgky jht7u6t khguygiu jhghh nkhui8uhi gyujtyutggygyuu
// jju hyhyuhbhuiyugyuyhbjyuy7uvgy uyyugyuyu tu yuyuyjuyhu
// huiuonjuioui nuiu8iuiui huyu hyiuyu8i iuy7iyiuuihyuiyui iuui
// huyuiuhyui gyuyu yuyi huyui huihui gyuyhuuhjgyjugyhhy