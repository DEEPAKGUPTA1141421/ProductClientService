package com.ProductClientService.ProductClientService.Model;

import jakarta.persistence.*;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import com.fasterxml.jackson.annotation.JsonManagedReference;

@Entity
@Table(name = "category_attributes")
@Getter
@Setter
public class CategoryAttribute {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    @JsonManagedReference
    private Category category;

    @Column(name = "name", nullable = false)
    private String name;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "category_attribute_mapping", // 👈 join table
            joinColumns = @JoinColumn(name = "category_attribute_id"), inverseJoinColumns = @JoinColumn(name = "attribute_id"))
    private Set<Attribute> attributes = new HashSet<>();

    Boolean is_Required = false;
    Boolean isImageAttribute = false;
    Boolean isVariantAttribute = false;
    Boolean isAdditionalAttribute = false;
}
// jjijjmjj kjkjkkhjjidiojjfrjijjijin