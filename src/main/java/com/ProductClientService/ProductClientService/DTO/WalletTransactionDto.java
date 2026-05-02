package com.ProductClientService.ProductClientService.DTO;

import com.ProductClientService.ProductClientService.Model.WalletTransaction;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class WalletTransactionDto {

    private UUID id;
    private long amountPaise;
    private String amountRupees;
    private String type;
    private String typeLabel;
    private String source;
    private String sourceLabel;
    private String referenceId;
    private String description;
    private String createdAt;

    public static WalletTransactionDto fromEntity(WalletTransaction tx) {
        return WalletTransactionDto.builder()
                .id(tx.getId())
                .amountPaise(tx.getAmountPaise())
                .amountRupees(String.format("%.2f", tx.getAmountPaise() / 100.0))
                .type(tx.getType().name())
                .typeLabel(tx.getType().label())
                .source(tx.getSource().name())
                .sourceLabel(tx.getSource().label())
                .referenceId(tx.getReferenceId())
                .description(tx.getDescription())
                .createdAt(tx.getCreatedAt() != null ? tx.getCreatedAt().toString() : null)
                .build();
    }
}
