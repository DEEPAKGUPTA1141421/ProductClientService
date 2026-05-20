package com.ProductClientService.ProductClientService.DTO;

import com.ProductClientService.ProductClientService.Model.Wallet;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class WalletDto {

    private UUID id;
    private long balancePaise;
    private String balanceRupees;
    private String currency;

    public static WalletDto fromEntity(Wallet w) {
        long paise = w.getBalancePaise() == null ? 0L : w.getBalancePaise();
        return WalletDto.builder()
                .id(w.getId())
                .balancePaise(paise)
                .balanceRupees(String.format("%.2f", paise / 100.0))
                .currency(w.getCurrency())
                .build();
    }
}
