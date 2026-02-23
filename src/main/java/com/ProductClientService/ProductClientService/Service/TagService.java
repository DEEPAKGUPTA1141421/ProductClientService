package com.ProductClientService.ProductClientService.Service;

import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.ProductClientService.ProductClientService.DTO.seller.ProductTagRequestDto;
import com.ProductClientService.ProductClientService.Model.Product;
import com.ProductClientService.ProductClientService.Model.SearchLog;
import com.ProductClientService.ProductClientService.Model.Tag;
import com.ProductClientService.ProductClientService.Repository.ProductRepository;
import com.ProductClientService.ProductClientService.Repository.SearchLogRepository;
import com.ProductClientService.ProductClientService.Repository.TagRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TagService {

    private final TagRepository tagRepository;
    private final SearchLogRepository searchLogRepository;

    private final ProductRepository productRepository;

    public List<Tag> search(String keyword) {
        return tagRepository.searchTags(keyword);
    }

    @Transactional
    public void increaseClick(UUID tagId) {
        Tag tag = tagRepository.findById(tagId)
                .orElseThrow();
        tag.setClickCount(tag.getClickCount() + 1);
        tag.setUpdatedAt(ZonedDateTime.now());
    }

    @Transactional
    public void logSearch(String keyword, UUID tagId) {
        SearchLog log = new SearchLog(keyword, tagId);
        searchLogRepository.save(log);
    }

    @Transactional
    public Product AddProductTag(ProductTagRequestDto requestDto) {
        Product product = productRepository.findById(requestDto.product_id())
                .orElseThrow(() -> new RuntimeException("Product not found"));

        Set<Tag> tagSet = new HashSet<>();

        for (String tagName : requestDto.tags()) {
            Tag tag = tagRepository.findByNameIgnoreCase(tagName)
                    .orElseGet(() -> {
                        Tag newTag = new Tag();
                        newTag.setName(tagName.toLowerCase());
                        newTag.setSearchCount(0L);
                        newTag.setClickCount(0L);
                        newTag.setCreatedAt(ZonedDateTime.now());
                        newTag.setUpdatedAt(ZonedDateTime.now());
                        return tagRepository.save(newTag);
                    });

            tagSet.add(tag);
        }

        product.setTags(tagSet);

        return productRepository.save(product);
    }

    @Transactional
    public void removeTagFromProduct(UUID productId, UUID tagId) {

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        Tag tag = tagRepository.findById(tagId)
                .orElseThrow(() -> new RuntimeException("Tag not found"));

        if (!product.getTags().contains(tag)) {
            throw new RuntimeException("Tag is not mapped to this product");
        }

        product.getTags().remove(tag);

        // optional but clean (since bidirectional)
        tag.getProducts().remove(product);

        productRepository.save(product);
    }

}
