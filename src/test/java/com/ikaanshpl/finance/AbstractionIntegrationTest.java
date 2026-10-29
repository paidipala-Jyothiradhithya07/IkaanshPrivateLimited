package com.ikaanshpl.finance;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.containers.PostgreSQLContainer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
public class AbstractionIntegrationTest {
	static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        POSTGRES.start();   // singleton container: started once per JVM, stopped by Ryuk
    }

    @DynamicPropertySource
    static void dbProps(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        r.add("spring.datasource.username", POSTGRES::getUsername);
        r.add("spring.datasource.password", POSTGRES::getPassword);
        r.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @Autowired protected MockMvc mvc;
    @Autowired protected ObjectMapper om;

    // ---------------- helpers ----------------
    protected String uniqueEmail() { return "u" + UUID.randomUUID().toString().substring(0, 8) + "@test.com"; }

    protected String json(Object o) throws Exception { return om.writeValueAsString(o); }

    protected JsonNode body(MvcResult r) throws Exception { return om.readTree(r.getResponse().getContentAsString()); }

    /** Registers a fresh user and logs in. Returns {accessToken, refreshToken}. */
    protected JsonNode signup(String email) throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("name", "Test", "email", email, "password", "Passw0rd!"))))
                .andExpect(status().isCreated());
        return login(email);
    }

    protected JsonNode login(String email) throws Exception {
        MvcResult r = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("email", email, "password", "Passw0rd!"))))
                .andExpect(status().isOk()).andReturn();
        return body(r);
    }

    protected ResultActions get(String url, String token) throws Exception {
        return mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(url).header("Authorization", "Bearer " + token));
    }

    protected ResultActions postJson(String url, String token, Object payload) throws Exception {
        return mvc.perform(post(url).header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(json(payload)));
    }

    protected long createAccount(String token, String name, String opening) throws Exception {
        MvcResult r = postJson("/api/accounts", token, Map.of("name", name, "openingBalance", opening))
                .andExpect(status().isCreated()).andReturn();
        return body(r).get("id").asLong();
    }

    protected long categoryId(String token, String name) throws Exception {
        JsonNode list = body(get("/api/categories", token).andExpect(status().isOk()).andReturn());
        for (JsonNode c : list) if (c.get("name").asText().equalsIgnoreCase(name)) return c.get("id").asLong();
        throw new IllegalStateException("category not found: " + name);
    }

    protected ResultActions importCsv(String token, long accountId, String filename, String csv) throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", filename, "text/csv", csv.getBytes(StandardCharsets.UTF_8));
        return mvc.perform(multipart("/api/transactions/import").file(file)
                .param("accountId", String.valueOf(accountId))
                .header("Authorization", "Bearer " + token));
    }
}
