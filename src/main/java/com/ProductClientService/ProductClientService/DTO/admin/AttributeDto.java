package com.ProductClientService.ProductClientService.DTO.admin;

import java.util.List;

import com.ProductClientService.ProductClientService.Model.Attribute.FEILDTYPE;


public record AttributeDto(
     String name,
     FEILDTYPE fieldType,
     Boolean isRequired,
     List<String> options
) {}