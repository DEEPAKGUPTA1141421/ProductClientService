package com.ProductClientService.ProductClientService.Model;

import java.util.List;
import java.util.UUID;

import jakarta.persistence.*;
import lombok.Data;


@Entity
@Table(name = "attributes")
@Data
public class Attribute {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String name;

    @Enumerated(EnumType.STRING)  // ✅ Save enum as String in DB
    private FEILDTYPE field_type;

    private Boolean is_required = true;

    private List<String> options;

    public enum FEILDTYPE {
        STRING,
        NUMBER,
        ENUMERATION,   // ✅ renamed, not "enum"
        BOOLEAN_TYPE,  // ✅ renamed, not "boolean"
        DATE
    }
}