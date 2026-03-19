package com.ProductClientService.ProductClientService.DTO.Settings;

import jakarta.validation.constraints.NotBlank;

public record BankDetailsDto(
                @NotBlank(message = "Account holder name is required") String accountHolderName,

                @NotBlank(message = "Account number is required") String accountNumber,
                @NotBlank(message = "Bank name is required") String bankName,
                @NotBlank(message = "IFSC code is required") String ifscCode) {
}
// huuihuiuhuuhhuhuhuhuhukkuh huh huhuhkhuis