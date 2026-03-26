package com.ProductClientService.ProductClientService.DTO;

import java.util.Set;
import java.util.UUID;

import com.ProductClientService.ProductClientService.Model.User;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserSummaryDTO {
    private UUID id;
    private String name;
    private String image; // only one image (e.g. first)

    public static UserSummaryDTO fromEntity(User user) {
        String image = user.getAvatarUrl();

        return new UserSummaryDTO(user.getId(), user.getName(), image);
    }
}
