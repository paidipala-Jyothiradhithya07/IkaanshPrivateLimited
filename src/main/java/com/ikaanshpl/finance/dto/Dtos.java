package com.ikaanshpl.finance.dto;

import java.math.BigDecimal;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.ikaanshpl.finance.audit.Auditable;
import com.ikaanshpl.finance.entity.Account;
import com.ikaanshpl.finance.entity.AuditEvent;
import com.ikaanshpl.finance.entity.Category;
import com.ikaanshpl.finance.entity.Transaction;
import com.ikaanshpl.finance.entity.TxType;
import com.ikaanshpl.finance.entity.User;

import com.ikaansh.finance.domain.*;
import jakarta.validation.constraints.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;


public class Dtos {
	private Dtos() {}

    // ---------- auth ----------
    public record RegisterRequest(@NotBlank @Size(max = 100) String name,
                                  @NotBlank @Email @Size(max = 200) String email,
                                  @NotBlank @Size(min = 8, max = 72) String password) {}

    public record LoginRequest(@NotBlank String email, @NotBlank String password) {}

    public record RefreshRequest(@NotBlank String refreshToken) {}

    public record TokenResponse(String accessToken, String refreshToken, String tokenType, long expiresIn) {}

    public record UserResponse(Long id, String name, String email) {
        public static UserResponse from(User u) { return new UserResponse(u.getId(), u.getName(), u.getEmail()); }
    }

    // ---------- accounts & categories ----------
    public record CreateAccountRequest(@NotBlank @Size(max = 100) String name,
                                       @DecimalMin("0.00") @Digits(integer = 15, fraction = 2) BigDecimal openingBalance) {}

    public record AccountResponse(Long id, String name, BigDecimal balance) implements Auditable {
        public static AccountResponse from(Account a) { return new AccountResponse(a.getId(), a.getName(), a.getBalance()); }
        @Override public Long auditEntityId() { return id; }
    }

    public record CreateCategoryRequest(@NotBlank @Size(max = 100) String name) {}

    public record CategoryResponse(Long id, String name) {
        public static CategoryResponse from(Category c) { return new CategoryResponse(c.getId(), c.getName()); }
    }

    // ---------- transactions ----------
    public record CreateTransactionRequest(@NotNull Long accountId,
                                           @NotNull Long categoryId,
                                           @NotNull TxType type,
                                           @NotNull @DecimalMin("0.01") @Digits(integer = 15, fraction = 2) BigDecimal amount,
                                           @NotNull LocalDate txDate,
                                           @Size(max = 255) String description) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record TransactionResponse(Long id, Long accountId, Long categoryId, String categoryName, TxType type,
                                      BigDecimal amount, LocalDate txDate, String description,
                                      BigDecimal accountBalance) implements Auditable {
        public static TransactionResponse from(Transaction t, BigDecimal accountBalance) {
            return new TransactionResponse(t.getId(), t.getAccount().getId(), t.getCategory().getId(),
                    t.getCategory().getName(), t.getType(), t.getAmount(), t.getTxDate(), t.getDescription(),
                    accountBalance);
            
        }
        @Override public Long auditEntityId() { return id; }
    }

    public record RowError(int row, String message) {}

    public record ImportResult(Long accountId, int totalRows, int imported, int rejected,
                               BigDecimal accountBalance, List<RowError> errors) implements Auditable {
        @Override public Long auditEntityId() { return accountId; }
        /** Audit log keeps counts only - not the (possibly 10k) error rows. */
        @Override public String toString() {
            return "ImportResult[accountId=" + accountId + ", totalRows=" + totalRows + ", imported=" + imported
                    + ", rejected=" + rejected + ", accountBalance=" + accountBalance + "]";
        }
    }

    public record TransactionFilter(Long accountId, Long categoryId, TxType type, LocalDate from, LocalDate to) {}

    public record MonthlySummary(String month, BigDecimal income, BigDecimal expense, BigDecimal net, long count) {}

    public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {
        public static <T> PageResponse<T> of(org.springframework.data.domain.Page<T> p) {
            return new PageResponse<>(p.getContent(), p.getNumber(), p.getSize(), p.getTotalElements(), p.getTotalPages());
        }
    }

    // ---------- audit & websocket ----------
    public record AuditEventResponse(Long id, String entityType, Long entityId, String action, String details,
                                     Instant createdAt) {
        public static AuditEventResponse from(AuditEvent e) {
            return new AuditEventResponse(e.getId(), e.getEntityType(), e.getEntityId(), e.getAction(),
                    e.getDetails(), e.getCreatedAt());
        }
    }

    public record BalanceUpdate(Long accountId, BigDecimal balance, Instant at) {}
}
