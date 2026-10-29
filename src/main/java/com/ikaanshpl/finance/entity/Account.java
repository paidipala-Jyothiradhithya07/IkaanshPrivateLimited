package com.ikaanshpl.finance.entity;

import java.math.BigDecimal;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.*;


@Entity
@Table(name = "accounts",
uniqueConstraints = @UniqueConstraint(name = "uk_account_user_name", columnNames = {"user_id", "name"}))
public class Account {
	@Id 
	@GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;

    public void apply(TxType type, BigDecimal amount) {
        if (type == TxType.INCOME) {
            balance = balance.add(amount);
        } else if (type == TxType.EXPENSE) {
            balance = balance.subtract(amount);
        }
    }

    public void reverse(TxType type, BigDecimal amount) {
        if (type == TxType.INCOME) {
            balance = balance.subtract(amount);
        } else if (type == TxType.EXPENSE) {
            balance = balance.add(amount);
        }
    }
    
    

	public Account(Long id, String name, BigDecimal balance, Long userId) {
		super();
		this.id = id;
		this.name = name;
		this.balance = balance;
		this.userId = userId;
	}

	public Account() {
		super();
		// TODO Auto-generated constructor stub
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

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public BigDecimal getBalance() {
		return balance;
	}

	public void setBalance(BigDecimal balance) {
		this.balance = balance;
	}
}
