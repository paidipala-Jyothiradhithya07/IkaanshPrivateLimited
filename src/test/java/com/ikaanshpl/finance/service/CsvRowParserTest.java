package com.ikaanshpl.finance.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ikaansh.finance.domain.Category;
import com.ikaansh.finance.domain.TxType;
import com.ikaansh.finance.service.CsvRowParser;

public class CsvRowParserTest {
	private Map<String, Integer> idx;
    private Map<String, Category> cats;

    @BeforeEach
    void setUp() {
        idx = CsvRowParser.headerIndex(new String[]{"\uFEFFDate", "TYPE", " amount ", "Category", "Description"});
        cats = Map.of("food", new Category(1L, "Food"));
    }

    private String[] row(String date, String type, String amount, String category, String desc) {
        return new String[]{date, type, amount, category, desc};
    }

    @Test
    void parsesValidRow_caseInsensitiveHeadersAndValues() {
        var p = CsvRowParser.parse(row("2026-10-05", "expense", "12.5", "FOOD", " lunch "), idx, cats);
        assertThat(p.date()).isEqualTo(LocalDate.of(2026, 10, 5));
        assertThat(p.type()).isEqualTo(TxType.EXPENSE);
        assertThat(p.amount()).isEqualByComparingTo("12.50");
        assertThat(p.amount().scale()).isEqualTo(2);
        assertThat(p.description()).isEqualTo("lunch");
    }

    @Test
    void missingRequiredColumn_isRejected() {
        assertThatThrownBy(() -> CsvRowParser.headerIndex(new String[]{"date", "type", "amount"}))
                .hasMessageContaining("category");
    }

    @Test
    void invalidRows_giveClearMessages() {
        assertThatThrownBy(() -> CsvRowParser.parse(row("05/10/2026", "EXPENSE", "5", "Food", ""), idx, cats))
                .hasMessageContaining("yyyy-MM-dd");
        assertThatThrownBy(() -> CsvRowParser.parse(row("2026-10-05", "SPEND", "5", "Food", ""), idx, cats))
                .hasMessageContaining("INCOME or EXPENSE");
        assertThatThrownBy(() -> CsvRowParser.parse(row("2026-10-05", "EXPENSE", "abc", "Food", ""), idx, cats))
                .hasMessageContaining("not a number");
        assertThatThrownBy(() -> CsvRowParser.parse(row("2026-10-05", "EXPENSE", "-1", "Food", ""), idx, cats))
                .hasMessageContaining("greater than 0");
        assertThatThrownBy(() -> CsvRowParser.parse(row("2026-10-05", "EXPENSE", "1.234", "Food", ""), idx, cats))
                .hasMessageContaining("2 decimal");
        assertThatThrownBy(() -> CsvRowParser.parse(row("2026-10-05", "EXPENSE", "5", "Gadgets", ""), idx, cats))
                .hasMessageContaining("unknown category");
    }

    @Test
    void trailingZerosBeyondTwoDecimalsAreAccepted() {
        var p = CsvRowParser.parse(row("2026-10-05", "INCOME", "10.500", "Food", ""), idx, cats);
        assertThat(p.amount()).isEqualByComparingTo(new BigDecimal("10.50"));
        assertThat(p.description()).isNull();
    }

    @Test
    void blankRowDetection() {
        assertThat(CsvRowParser.isBlank(new String[]{"", " ", null})).isTrue();
        assertThat(CsvRowParser.isBlank(new String[]{"", "x"})).isFalse();
    }
}
