package com.ikaanshpl.finance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;

public class AuthIntegrationTest 
	extends AbstractionIntegrationTest {

	    private JsonNode refresh(String refreshToken, int expectedStatus) throws Exception {
	        var r = postJson("/api/auth/refresh", "ignored", Map.of("refreshToken", refreshToken))
	                .andExpect(status().is(expectedStatus)).andReturn();
	        return body(r);
	    }

	    @Test
	    void wrongPassword_returns401() throws Exception {
	        String email = uniqueEmail();
	        signup(email);
	        postJson("/api/auth/login", "x", Map.of("email", email, "password", "wrong-password"))
	                .andExpect(status().isUnauthorized());
	    }

	    @Test
	    void duplicateEmail_returns409() throws Exception {
	        String email = uniqueEmail();
	        signup(email);
	        postJson("/api/auth/register", "x", Map.of("name", "T", "email", email, "password", "Passw0rd!"))
	                .andExpect(status().isConflict());
	    }

	    @Test
	    void refresh_rotatesToken_andNewAccessTokenWorks() throws Exception {
	        JsonNode first = signup(uniqueEmail());
	        JsonNode second = refresh(first.get("refreshToken").asText(), 200);

	        assertThat(second.get("refreshToken").asText()).isNotEqualTo(first.get("refreshToken").asText());
	        get("/api/accounts", second.get("accessToken").asText()).andExpect(status().isOk());
	    }

	    @Test
	    void reusingAnOldRefreshToken_revokesTheWholeSession() throws Exception {
	        JsonNode first = signup(uniqueEmail());
	        JsonNode second = refresh(first.get("refreshToken").asText(), 200);   // first is now burned

	        refresh(first.get("refreshToken").asText(), 401);                      // replay -> attack detected

	        // the legitimate newer tokens of that session are dead too
	        refresh(second.get("refreshToken").asText(), 401);
	        get("/api/accounts", second.get("accessToken").asText()).andExpect(status().isUnauthorized());
	    }

	    @Test
	    void logout_invalidatesThatSessionImmediately_butNotOtherSessions() throws Exception {
	        String email = uniqueEmail();
	        JsonNode s1 = signup(email);
	        JsonNode s2 = login(email);

	        postJson("/api/auth/logout", "x", Map.of("refreshToken", s1.get("refreshToken").asText()))
	                .andExpect(status().isNoContent());

	        get("/api/accounts", s1.get("accessToken").asText()).andExpect(status().isUnauthorized());
	        refresh(s1.get("refreshToken").asText(), 401);
	        get("/api/accounts", s2.get("accessToken").asText()).andExpect(status().isOk());   // other device unaffected
	    }

	    @Test
	    void logoutAll_killsEverySession() throws Exception {
	        String email = uniqueEmail();
	        JsonNode s1 = signup(email);
	        JsonNode s2 = login(email);

	        mvc.perform(post("/api/auth/logout-all").header("Authorization", "Bearer " + s1.get("accessToken").asText()))
	                .andExpect(status().isNoContent());

	        get("/api/accounts", s1.get("accessToken").asText()).andExpect(status().isUnauthorized());
	        get("/api/accounts", s2.get("accessToken").asText()).andExpect(status().isUnauthorized());
	        refresh(s2.get("refreshToken").asText(), 401);
	    }

	    /** Concurrent session safety: the same refresh token used by 5 threads at once must succeed exactly once. */
	    @Test
	    void concurrentRefreshWithSameToken_succeedsExactlyOnce() throws Exception {
	        JsonNode s = signup(uniqueEmail());
	        String rt = s.get("refreshToken").asText();

	        int threads = 5;
	        ExecutorService pool = Executors.newFixedThreadPool(threads);
	        CountDownLatch start = new CountDownLatch(1);
	        List<Future<Integer>> futures = new ArrayList<>();
	        for (int i = 0; i < threads; i++) {
	            futures.add(pool.submit(() -> {
	                start.await();
	                return postJson("/api/auth/refresh", "x", Map.of("refreshToken", rt))
	                        .andReturn().getResponse().getStatus();
	            }));
	        }
	        start.countDown();
	        int ok = 0, unauthorized = 0;
	        for (Future<Integer> f : futures) {
	            int code = f.get(30, TimeUnit.SECONDS);
	            if (code == 200) ok++; else if (code == 401) unauthorized++;
	        }
	        pool.shutdown();

	        assertThat(ok).isEqualTo(1);
	        assertThat(unauthorized).isEqualTo(threads - 1);
	    }

	    @Test
	    void tamperedAccessToken_returns401() throws Exception {
	        String token = signup(uniqueEmail()).get("accessToken").asText();
	        get("/api/accounts", token + "x").andExpect(status().isUnauthorized());
	    }
}
