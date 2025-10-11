package com.ProductClientService.ProductClientService.DTO;

import java.util.Set;
import java.util.UUID;

import com.ProductClientService.ProductClientService.Model.User;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserSummaryDTO {
    private UUID id;
    private String name;
    private String image; // only one image (e.g. first)

    public static UserSummaryDTO fromEntity(User user) {
        String firstImage = null;
        Set<String> images = user.getImages();
        if (images != null && !images.isEmpty()) {
            firstImage = images.iterator().next(); // pick the first image
        }
        return new UserSummaryDTO(user.getId(), user.getName(), firstImage);
    }
}
