package com.ikaanshpl.finance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;


public class TransactionIntegrationTest extends AbstractionIntegrationTest{
	 private Map<String, Object> tx(long accountId, long categoryId, String type, String amount) {
	        return Map.of("accountId", accountId, "categoryId", categoryId, "type", type,
	                "amount", amount, "txDate", LocalDate.of(2026, 10, 1).toString(), "description", "test");
	    }

	    @Test
	    void createTransactions_updatesBalance() throws Exception {
	        String token = signup(uniqueEmail()).get("accessToken").asText();
	        long acc = createAccount(token, "Main", "1000.00");
	        long food = categoryId(token, "Food");
	        long salary = categoryId(token, "Salary");

	        postJson("/api/transactions", token, tx(acc, food, "EXPENSE", "250.75"))
	                .andExpect(status().isCreated())
	                .andExpect(jsonPath("$.accountBalance").value(749.25));
	        postJson("/api/transactions", token, tx(acc, salary, "INCOME", "500.00"))
	                .andExpect(status().isCreated())
	                .andExpect(jsonPath("$.accountBalance").value(1249.25));

	        get("/api/accounts/" + acc, token).andExpect(jsonPath("$.balance").value(1249.25));
	    }

	    @Test
	    void deleteTransaction_restoresBalance() throws Exception {
	        String token = signup(uniqueEmail()).get("accessToken").asText();
	        long acc = createAccount(token, "Main", "100.00");
	        long food = categoryId(token, "Food");
	        long id = body(postJson("/api/transactions", token, tx(acc, food, "EXPENSE", "40.00"))
	                .andExpect(status().isCreated()).andReturn()).get("id").asLong();

	        mvc.perform(delete("/api/transactions/" + id).header("Authorization", "Bearer " + token))
	                .andExpect(status().isOk());
	        get("/api/accounts/" + acc, token).andExpect(jsonPath("$.balance").value(100.00));
	    }

	    @Test
	    void validationFailures_areRejected() throws Exception {
	        String token = signup(uniqueEmail()).get("accessToken").asText();
	        long acc = createAccount(token, "Main", "100.00");
	        long food = categoryId(token, "Food");

	        postJson("/api/transactions", token, tx(acc, food, "EXPENSE", "-5")).andExpect(status().isBadRequest());
	        postJson("/api/transactions", token, tx(acc, food, "EXPENSE", "1.999")).andExpect(status().isBadRequest());
	        postJson("/api/transactions", token, tx(acc, 999_999L, "EXPENSE", "5")).andExpect(status().isNotFound());
	        postJson("/api/transactions", token, tx(999_999L, food, "EXPENSE", "5")).andExpect(status().isNotFound());
	        postJson("/api/transactions", token, Map.of("accountId", acc)).andExpect(status().isBadRequest());

	        get("/api/accounts/" + acc, token).andExpect(jsonPath("$.balance").value(100.00)); // untouched
	    }

	    @Test
	    void userCannotTouchAnotherUsersAccount() throws Exception {
	        String alice = signup(uniqueEmail()).get("accessToken").asText();
	        String bob = signup(uniqueEmail()).get("accessToken").asText();
	        long aliceAcc = createAccount(alice, "Alice main", "100.00");
	        long bobFood = categoryId(bob, "Food");

	        postJson("/api/transactions", bob, tx(aliceAcc, bobFood, "EXPENSE", "10")).andExpect(status().isNotFound());
	        get("/api/accounts/" + aliceAcc, bob).andExpect(status().isNotFound());
	    }

	    @Test
	    void unauthenticatedRequest_returns401() throws Exception {
	        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/accounts"))
	                .andExpect(status().isUnauthorized());
	    }

	    /** 20 threads hit ONE account at the same time: the pessimistic row lock must prevent lost updates. */
	    @Test
	    void concurrentExpenses_keepBalanceConsistent() throws Exception {
	        String token = signup(uniqueEmail()).get("accessToken").asText();
	        long acc = createAccount(token, "Shared", "1000.00");
	        long food = categoryId(token, "Food");

	        int threads = 20;
	        ExecutorService pool = Executors.newFixedThreadPool(threads);
	        CountDownLatch start = new CountDownLatch(1);
	        List<Future<Integer>> results = new ArrayList<>();
	        for (int i = 0; i < threads; i++) {
	            results.add(pool.submit(() -> {
	                start.await();
	                return postJson("/api/transactions", token, tx(acc, food, "EXPENSE", "1.00"))
	                        .andReturn().getResponse().getStatus();
	            }));
	        }
	        start.countDown();
	        for (Future<Integer> f : results) assertThat(f.get(30, TimeUnit.SECONDS)).isEqualTo(201);
	        pool.shutdown();

	        JsonNode account = body(get("/api/accounts/" + acc, token).andReturn());
	        assertThat(account.get("balance").decimalValue()).isEqualByComparingTo(new BigDecimal("980.00"));
	        JsonNode page = body(get("/api/transactions?accountId=" + acc + "&size=100", token).andReturn());
	        assertThat(page.get("totalElements").asLong()).isEqualTo(20);
	    }

	    @Test
	    void auditLog_recordsBalanceAndTransactionChanges() throws Exception {
	        String token = signup(uniqueEmail()).get("accessToken").asText();
	        long acc = createAccount(token, "Main", "10.00");
	        long food = categoryId(token, "Food");
	        postJson("/api/transactions", token, tx(acc, food, "EXPENSE", "3.00")).andExpect(status().isCreated());

	        JsonNode audit = body(get("/api/audit", token).andExpect(status().isOk()).andReturn());
	        Set<String> kinds = new HashSet<>();
	        audit.get("content").forEach(e -> kinds.add(e.get("entityType").asText() + ":" + e.get("action").asText()));
	        assertThat(kinds).contains("ACCOUNT:CREATE", "TRANSACTION:CREATE");
	    }
}
