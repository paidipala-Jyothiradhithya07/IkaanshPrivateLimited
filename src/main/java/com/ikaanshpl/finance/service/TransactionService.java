package com.ikaanshpl.finance.service;

import org.springframework.context.ApplicationEventPublisher;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ikaanshpl.finance.audit.Audited;
import com.ikaanshpl.finance.controller.ApiException;
import com.ikaanshpl.finance.dto.Dtos.CreateTransactionRequest;
import com.ikaanshpl.finance.dto.Dtos.TransactionResponse;
import com.ikaanshpl.finance.entity.Account;
import com.ikaanshpl.finance.entity.Category;
import com.ikaanshpl.finance.entity.Transaction;
import com.ikaanshpl.finance.repo.AccountRepository;
import com.ikaanshpl.finance.repo.CategoryRepository;
import com.ikaanshpl.finance.repo.TransactionRepository;
import com.ikaanshpl.finance.ws.BalanceChangedEvent;

@Service
public class TransactionService {
	  private final AccountRepository accounts;
	    private final CategoryRepository categories;
	    private final TransactionRepository transactions;
	    private final ApplicationEventPublisher events;
	    
	    

	    public TransactionService(AccountRepository accounts, CategoryRepository categories,
				TransactionRepository transactions, ApplicationEventPublisher events) {
			super();
			this.accounts = accounts;
			this.categories = categories;
			this.transactions = transactions;
			this.events = events;
		}

		@Audited(entity = "TRANSACTION", action = "CREATE")
	    @Transactional
	    public TransactionResponse create(Long userId, CreateTransactionRequest r) {
	        // Lock FIRST: two concurrent requests on one account queue up here, so no lost balance update.
	        Account account = accounts.findForUpdate(r.accountId(), userId)
	                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Account not found"));
	        Category category = categories.findByIdAndUserId(r.categoryId(), userId)
	                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Category not found"));

	        Transaction t = Transaction.of(userId, account, category, r.type(), r.amount(), r.txDate(),
	                r.description() == null ? null : r.description().trim());
	        transactions.save(t);
	        account.apply(r.type(), r.amount());

	        events.publishEvent(new BalanceChangedEvent(userId, account.getId(), account.getBalance()));
	        return TransactionResponse.from(t, account.getBalance());
	    }

	    @Audited(entity = "TRANSACTION", action = "DELETE")
	    @Transactional
	    public TransactionResponse delete(Long userId, Long id) {
	        Transaction t = transactions.findByIdAndUserId(id, userId)
	                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Transaction not found"));
	        Account account = accounts.findForUpdate(t.getAccount().getId(), userId)
	                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Account not found"));

	        account.reverse(t.getType(), t.getAmount());
	        TransactionResponse response = TransactionResponse.from(t, account.getBalance());
	        transactions.delete(t);

	        events.publishEvent(new BalanceChangedEvent(userId, account.getId(), account.getBalance()));
	        return response;
	    }

}
