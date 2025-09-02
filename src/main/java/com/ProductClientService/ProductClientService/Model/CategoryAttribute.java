package com.ProductClientService.ProductClientService.Model;
import jakarta.persistence.*;
import lombok.Data;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
@Entity
@Table(name = "category_attributes")
@Data
public class CategoryAttribute {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch=FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false, unique = true)
    private Category category;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "category_attribute_mapping",   // 👈 join table
        joinColumns = @JoinColumn(name = "category_attribute_id"),
        inverseJoinColumns = @JoinColumn(name = "attribute_id")
    )
    private Set<Attribute> attributes=new HashSet<>();
}
