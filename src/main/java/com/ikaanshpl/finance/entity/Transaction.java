package com.ikaanshpl.finance.entity;

import java.math.BigDecimal;


import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "transactions", indexes = {
        @Index(name = "idx_tx_user_date", columnList = "user_id, tx_date"),
        @Index(name = "idx_tx_user_ym", columnList = "user_id, ym"),
        @Index(name = "idx_tx_account", columnList = "account_id")
})
public class Transaction {
	 @Id
	    @GeneratedValue(strategy = GenerationType.IDENTITY)
	    private Long id;

	    /** Denormalised owner id: every query filters on it, so a user can never see someone else's data. */
	    @Column(name = "user_id", nullable = false)
	    private Long userId;

	    @ManyToOne(fetch = FetchType.LAZY, optional = false)
	    @JoinColumn(name = "account_id", nullable = false)
	    private Account account;

	    @ManyToOne(fetch = FetchType.LAZY, optional = false)
	    @JoinColumn(name = "category_id", nullable = false)
	    private Category category;

	    @Enumerated(EnumType.STRING)
	    @Column(nullable = false, length = 10)
	    private TxType type;

	    @Column(nullable = false, precision = 19, scale = 2)
	    private BigDecimal amount;

	    @Column(name = "tx_date", nullable = false)
	    private LocalDate txDate;

	    /** Pre-computed "YYYY-MM" bucket so monthly summaries are a plain indexed GROUP BY. */
	    @Column(nullable = false, length = 7)
	    private String ym;

	    @Column(length = 255)
	    private String description;

	    @Column(name = "created_at", nullable = false)
	    private Instant createdAt;

	    public static Transaction of(Long userId, Account account, Category category, TxType type,
	                                 BigDecimal amount, LocalDate txDate, String description) {
	        Transaction t = new Transaction();
	        t.userId = userId;
	        t.account = account;
	        t.category = category;
	        t.type = type;
	        t.amount = amount;
	        t.txDate = txDate;
	        t.description = description;
	        return t;
	    }

	    @PrePersist
	    void onCreate() {
	        ym = YearMonth.from(txDate).toString();
	        createdAt = Instant.now();
	    }

	    
		public Transaction() {
			super();
			// TODO Auto-generated constructor stub
		}

		public Transaction(Long id, Long userId, Account account, Category category, TxType type, BigDecimal amount,
				LocalDate txDate, String ym, String description, Instant createdAt) {
			super();
			this.id = id;
			this.userId = userId;
			this.account = account;
			this.category = category;
			this.type = type;
			this.amount = amount;
			this.txDate = txDate;
			this.ym = ym;
			this.description = description;
			this.createdAt = createdAt;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Long getUserId() {
			return userId;
		}

		public void setUserId(Long userId) {
			this.userId = userId;
		}

		public Account getAccount() {
			return account;
		}

		public void setAccount(Account account) {
			this.account = account;
		}
		
		public Category getCategory() {
			return category;
		}

		public void setCategory(Category category) {
			this.category = category;
		}

		public TxType getType() {
			return type;
		}

		public void setType(TxType type) {
			this.type = type;
		}

		public BigDecimal getAmount() {
			return amount;
		}

		public void setAmount(BigDecimal amount) {
			this.amount = amount;
		}

		public LocalDate getTxDate() {
			return txDate;
		}

		public void setTxDate(LocalDate txDate) {
			this.txDate = txDate;
		}

		public String getYm() {
			return ym;
		}

		public void setYm(String ym) {
			this.ym = ym;
		}

		public String getDescription() {
			return description;
		}

		public void setDescription(String description) {
			this.description = description;
		}

		public Instant getCreatedAt() {
			return createdAt;
		}

		public void setCreatedAt(Instant createdAt) {
			this.createdAt = createdAt;
		}
}
