package com.ProductClientService.ProductClientService.DTO.Settings;

import jakarta.validation.constraints.NotBlank;

public record SecurityQuestionsDto(
        @NotBlank String question1,
        @NotBlank String answer1,
        @NotBlank String question2,
        @NotBlank String answer2) {
}