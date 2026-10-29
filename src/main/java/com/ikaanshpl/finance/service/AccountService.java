package com.ikaanshpl.finance.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ikaanshpl.finance.audit.Audited;
import com.ikaanshpl.finance.controller.ApiException;
import com.ikaanshpl.finance.dto.Dtos.AccountResponse;
import com.ikaanshpl.finance.dto.Dtos.CreateAccountRequest;
import com.ikaanshpl.finance.entity.Account;
import com.ikaanshpl.finance.repo.AccountRepository;
import com.ikaanshpl.finance.ws.BalanceChangedEvent;


@Service
public class AccountService {
	 private final AccountRepository accounts;
	    private final ApplicationEventPublisher events;
	    

	    public AccountService(AccountRepository accounts, ApplicationEventPublisher events) {
			super();
			this.accounts = accounts;
			this.events = events;
		}

		@Audited(entity = "ACCOUNT", action = "CREATE")
	    @Transactional
	    public AccountResponse create(Long userId, CreateAccountRequest r) {
	        String name = r.name().trim();
	        if (accounts.existsByUserIdAndNameIgnoreCase(userId, name)) {
	            throw new ApiException(HttpStatus.CONFLICT, "Account name already exists");
	        }
	        Account a = new Account();
	        a.setUserId(userId);
	        a.setName(name);
	        a.setBalance(r.openingBalance() == null ? BigDecimal.ZERO : r.openingBalance());
	        accounts.save(a);
	        events.publishEvent(new BalanceChangedEvent(userId, a.getId(), a.getBalance()));
	        return AccountResponse.from(a);
	    }

	    @Transactional(readOnly = true)
	    public List<AccountResponse> list(Long userId) {
	        return accounts.findByUserIdOrderById(userId).stream().map(AccountResponse::from).toList();
	    }

	    @Transactional(readOnly = true)
	    public AccountResponse get(Long userId, Long id) {
	        return accounts.findByIdAndUserId(id, userId).map(AccountResponse::from)
	                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Account not found"));
	    }
}
