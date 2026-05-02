package com.ProductClientService.ProductClientService.Service;

import com.ProductClientService.ProductClientService.DTO.ApiResponse;
import com.ProductClientService.ProductClientService.DTO.WalletDto;
import com.ProductClientService.ProductClientService.DTO.WalletTransactionDto;
import com.ProductClientService.ProductClientService.Model.Wallet;
import com.ProductClientService.ProductClientService.Model.WalletTransaction;
import com.ProductClientService.ProductClientService.Repository.WalletRepository;
import com.ProductClientService.ProductClientService.Repository.WalletTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class WalletService extends BaseService {

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository txRepository;

    // ── Internals ─────────────────────────────────────────────────────────────

    private Wallet getOrCreate(UUID userId) {
        return walletRepository.findByUserId(userId)
                .orElseGet(() -> walletRepository.save(
                        Wallet.builder().userId(userId).build()));
    }

    // ── Public: credit (called internally by ReturnService on REFUNDED) ───────

    @Transactional
    public void credit(UUID userId, long amountPaise,
                       WalletTransaction.TransactionSource source,
                       String referenceId, String description) {
        if (amountPaise <= 0) return;

        Wallet wallet = walletRepository.findByUserIdForUpdate(userId)
                .orElseGet(() -> walletRepository.save(
                        Wallet.builder().userId(userId).build()));

        wallet.setBalancePaise(wallet.getBalancePaise() + amountPaise);
        walletRepository.save(wallet);

        txRepository.save(WalletTransaction.builder()
                .userId(userId)
                .walletId(wallet.getId())
                .amountPaise(amountPaise)
                .type(WalletTransaction.TransactionType.CREDIT)
                .source(source)
                .referenceId(referenceId)
                .description(description)
                .build());

        log.info("Wallet credited userId={} amount={}p source={}", userId, amountPaise, source);
    }

    // ── Public API: get balance ───────────────────────────────────────────────

    public ApiResponse<Object> getBalance() {
        UUID userId = getUserId();
        Wallet wallet = getOrCreate(userId);
        return new ApiResponse<>(true, "Wallet fetched", WalletDto.fromEntity(wallet), 200);
    }

    // ── Public API: get transaction history ───────────────────────────────────

    public ApiResponse<Object> getTransactions(int page, int size) {
        UUID userId = getUserId();
        Page<WalletTransaction> pageResult = txRepository
                .findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, Math.min(size, 20)));

        Map<String, Object> data = new HashMap<>();
        data.put("transactions", pageResult.getContent().stream()
                .map(WalletTransactionDto::fromEntity).toList());
        data.put("totalElements", pageResult.getTotalElements());
        data.put("hasMore", pageResult.hasNext());
        data.put("page", page);

        return new ApiResponse<>(true, "Transactions fetched", data, 200);
    }

    // ── Public API: pay with wallet balance ───────────────────────────────────

    @Transactional
    public ApiResponse<Object> payWithWallet(String bookingId, long amountPaise) {
        UUID userId = getUserId();
        if (amountPaise <= 0) {
            return new ApiResponse<>(false, "Invalid amount", null, 400);
        }

        Wallet wallet = walletRepository.findByUserIdForUpdate(userId).orElse(null);
        if (wallet == null || wallet.getBalancePaise() < amountPaise) {
            return new ApiResponse<>(false, "Insufficient wallet balance", null, 422);
        }

        wallet.setBalancePaise(wallet.getBalancePaise() - amountPaise);
        walletRepository.save(wallet);

        txRepository.save(WalletTransaction.builder()
                .userId(userId)
                .walletId(wallet.getId())
                .amountPaise(amountPaise)
                .type(WalletTransaction.TransactionType.DEBIT)
                .source(WalletTransaction.TransactionSource.PURCHASE)
                .referenceId(bookingId)
                .description("Payment for order #" + bookingId)
                .build());

        log.info("Wallet payment userId={} bookingId={} amount={}p", userId, bookingId, amountPaise);

        return new ApiResponse<>(true, "Payment successful",
                WalletDto.fromEntity(wallet), 200);
    }

    // ── Admin: manual credit/debit ────────────────────────────────────────────

    @Transactional
    public ApiResponse<Object> adminCredit(UUID targetUserId, long amountPaise, String note) {
        credit(targetUserId, amountPaise,
                WalletTransaction.TransactionSource.ADMIN_CREDIT, null,
                note != null ? note : "Manual credit by support");
        Wallet wallet = walletRepository.findByUserId(targetUserId).orElseThrow();
        return new ApiResponse<>(true, "Credited", WalletDto.fromEntity(wallet), 200);
    }

    @Transactional
    public ApiResponse<Object> adminDebit(UUID targetUserId, long amountPaise, String note) {
        Wallet wallet = walletRepository.findByUserIdForUpdate(targetUserId).orElse(null);
        if (wallet == null || wallet.getBalancePaise() < amountPaise) {
            return new ApiResponse<>(false, "Insufficient balance for debit", null, 422);
        }
        wallet.setBalancePaise(wallet.getBalancePaise() - amountPaise);
        walletRepository.save(wallet);

        txRepository.save(WalletTransaction.builder()
                .userId(targetUserId)
                .walletId(wallet.getId())
                .amountPaise(amountPaise)
                .type(WalletTransaction.TransactionType.DEBIT)
                .source(WalletTransaction.TransactionSource.ADMIN_DEBIT)
                .description(note != null ? note : "Manual debit by support")
                .build());

        return new ApiResponse<>(true, "Debited", WalletDto.fromEntity(wallet), 200);
    }
}
