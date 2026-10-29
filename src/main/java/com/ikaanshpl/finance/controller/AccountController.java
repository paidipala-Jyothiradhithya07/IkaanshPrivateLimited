package com.ikaanshpl.finance.controller;

import java.util.List;


import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ikaanshpl.finance.dto.Dtos.AccountResponse;
import com.ikaanshpl.finance.dto.Dtos.CreateAccountRequest;
import com.ikaanshpl.finance.security.AuthUser;
import com.ikaanshpl.finance.service.AccountService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {
	private final AccountService accounts;

    public AccountController(AccountService accounts) {
		super();
		this.accounts = accounts;
	}

	@PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AccountResponse create(@AuthenticationPrincipal AuthUser u, @Valid @RequestBody CreateAccountRequest r) {
        return accounts.create(u.id(), r);
    }

    @GetMapping
    public List<AccountResponse> list(@AuthenticationPrincipal AuthUser u) {
        return accounts.list(u.id());
    }

    @GetMapping("/{id}")
    public AccountResponse get(@AuthenticationPrincipal AuthUser u, @PathVariable Long id) {
        return accounts.get(u.id(), id);
    }
}
