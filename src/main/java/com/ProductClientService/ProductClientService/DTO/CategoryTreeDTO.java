package com.ProductClientService.ProductClientService.DTO;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.ProductClientService.ProductClientService.Model.Category.Level;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CategoryTreeDTO {

    private UUID id;
    private String name;
    private String imageUrl;
    private Level categoryLevel;
    private List<CategoryTreeDTO> children = new ArrayList<>();

}