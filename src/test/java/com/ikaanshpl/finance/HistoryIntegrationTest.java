package com.ikaanshpl.finance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;


public class HistoryIntegrationTest extends AbstractionIntegrationTest {
    private String token;
    private long acc;
    private long foodId;

    @BeforeEach
    void seed() throws Exception {
        token = signup(uniqueEmail()).get("accessToken").asText();
        acc = createAccount(token, "Main", "0.00");
        foodId = categoryId(token, "Food");
        importCsv(token, acc, "seed.csv", """
                date,type,amount,category,description
                2026-08-01,INCOME,1000,Salary,aug pay
                2026-08-10,EXPENSE,100,Food,aug food
                2026-09-01,INCOME,1000,Salary,sep pay
                2026-09-05,EXPENSE,200,Food,sep food
                2026-09-06,EXPENSE,50,Transport,sep metro
                2026-10-02,EXPENSE,70,Food,oct food
                """).andExpect(status().isOk());
    }

    @Test
    void pagination_returnsRequestedSlice_newestFirst() throws Exception {
        JsonNode p0 = body(get("/api/transactions?size=4&page=0", token).andReturn());
        JsonNode p1 = body(get("/api/transactions?size=4&page=1", token).andReturn());
        assertThat(p0.get("totalElements").asLong()).isEqualTo(6);
        assertThat(p0.get("totalPages").asInt()).isEqualTo(2);
        assertThat(p0.get("content")).hasSize(4);
        assertThat(p1.get("content")).hasSize(2);
        assertThat(p0.get("content").get(0).get("txDate").asText()).isEqualTo("2026-10-02");
    }

    @Test
    void dateRangeFilter_isInclusive() throws Exception {
        JsonNode r = body(get("/api/transactions?from=2026-09-01&to=2026-09-05", token).andReturn());
        assertThat(r.get("totalElements").asLong()).isEqualTo(2);
    }

    @Test
    void categoryScope_filtersByCategory() throws Exception {
        JsonNode r = body(get("/api/transactions?categoryId=" + foodId, token).andReturn());
        assertThat(r.get("totalElements").asLong()).isEqualTo(3);
        r.get("content").forEach(t -> assertThat(t.get("categoryName").asText()).isEqualTo("Food"));
    }

    @Test
    void invalidInputs_return400() throws Exception {
        get("/api/transactions?from=2026-10-01&to=2026-01-01", token).andExpect(status().isBadRequest());
        get("/api/transactions?sort=account.userId,asc", token).andExpect(status().isBadRequest());
        get("/api/transactions?from=not-a-date", token).andExpect(status().isBadRequest());
    }

    @Test
    void monthlySummary_aggregatesIncomeExpenseAndNet() throws Exception {
        JsonNode s = body(get("/api/transactions/summary/monthly", token).andExpect(status().isOk()).andReturn());
        assertThat(s).hasSize(3);
        assertThat(s.get(0).get("month").asText()).isEqualTo("2026-08");
        assertThat(s.get(0).get("net").decimalValue()).isEqualByComparingTo("900");
        assertThat(s.get(1).get("month").asText()).isEqualTo("2026-09");
        assertThat(s.get(1).get("income").decimalValue()).isEqualByComparingTo("1000");
        assertThat(s.get(1).get("expense").decimalValue()).isEqualByComparingTo("250");
        assertThat(s.get(1).get("count").asLong()).isEqualTo(3);
    }

    @Test
    void monthlySummary_respectsCategoryAndDateScope() throws Exception {
        JsonNode s = body(get("/api/transactions/summary/monthly?categoryId=" + foodId + "&from=2026-09-01", token).andReturn());
        assertThat(s).hasSize(2); // Sep + Oct only
        assertThat(s.get(0).get("expense").decimalValue()).isEqualByComparingTo("200");
        assertThat(s.get(1).get("expense").decimalValue()).isEqualByComparingTo("70");
    }
}
