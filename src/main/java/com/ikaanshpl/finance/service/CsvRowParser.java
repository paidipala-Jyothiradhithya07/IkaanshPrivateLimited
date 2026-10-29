package com.ikaanshpl.finance.service;


import java.math.BigDecimal;


import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;


import com.ikaanshpl.finance.entity.TxType;
import org.springframework.stereotype.Service;

import com.ikaanshpl.finance.entity.Category;



@Service
public class CsvRowParser {
	  private CsvRowParser() {}

	    public static final List<String> REQUIRED_COLUMNS = List.of("date", "type", "amount", "category");
	    private static final BigDecimal MAX_AMOUNT = new BigDecimal("9999999999999.99");

	    public record ParsedRow(LocalDate date, TxType type, BigDecimal amount, Category category, String description) {}

	    public static Map<String, Integer> headerIndex(String[] header) {
	        if (header == null) throw new IllegalArgumentException("CSV file is empty");
	        Map<String, Integer> idx = new HashMap<>();
	        for (int i = 0; i < header.length; i++) {
	            String h = header[i] == null ? "" : header[i].replace("\uFEFF", "").trim().toLowerCase(Locale.ROOT);
	            idx.putIfAbsent(h, i);
	        }
	        List<String> missing = REQUIRED_COLUMNS.stream().filter(c -> !idx.containsKey(c)).toList();
	        if (!missing.isEmpty()) {
	            throw new IllegalArgumentException("Missing required column(s): " + String.join(", ", missing));
	        }
	        return idx;
	    }

	    public static boolean isBlank(String[] row) {
	        return Arrays.stream(row).allMatch(c -> c == null || c.isBlank());
	    }

	    /**
	     * @param categoriesByLowerName the CALLER's categories only - so a row can never reference another user's category
	     * @throws IllegalArgumentException with a human-readable message when the row is invalid
	     */
	    public static ParsedRow parse(String[] row, Map<String, Integer> idx, Map<String, Category> categoriesByLowerName) {
	        String dateStr = cell(row, idx, "date");
	        String typeStr = cell(row, idx, "type");
	        String amountStr = cell(row, idx, "amount");
	        String categoryStr = cell(row, idx, "category");
	        String description = cell(row, idx, "description");

	        if (dateStr.isEmpty()) throw new IllegalArgumentException("date is required");
	        LocalDate date;
	        try {
	            date = LocalDate.parse(dateStr);
	        } catch (DateTimeParseException e) {
	            throw new IllegalArgumentException("date '" + dateStr + "' must be in yyyy-MM-dd format");
	        }

	        if (typeStr.isEmpty()) throw new IllegalArgumentException("type is required");
	        TxType type;
	        try {
	            type = TxType.valueOf(typeStr.toUpperCase(Locale.ROOT));
	        } catch (IllegalArgumentException e) {
	            throw new IllegalArgumentException("type '" + typeStr + "' must be INCOME or EXPENSE");
	        }

	        if (amountStr.isEmpty()) throw new IllegalArgumentException("amount is required");
	        BigDecimal amount;
	        try {
	            amount = new BigDecimal(amountStr);
	        } catch (NumberFormatException e) {
	            throw new IllegalArgumentException("amount '" + amountStr + "' is not a number");
	        }
	        if (amount.signum() <= 0) throw new IllegalArgumentException("amount must be greater than 0");
	        if (amount.stripTrailingZeros().scale() > 2) throw new IllegalArgumentException("amount allows at most 2 decimal places");
	        if (amount.compareTo(MAX_AMOUNT) > 0) throw new IllegalArgumentException("amount is too large");

	        if (categoryStr.isEmpty()) throw new IllegalArgumentException("category is required");
	        Category category = categoriesByLowerName.get(categoryStr.toLowerCase(Locale.ROOT));
	        if (category == null) throw new IllegalArgumentException("unknown category '" + categoryStr + "'");

	        if (description.length() > 255) throw new IllegalArgumentException("description is longer than 255 characters");

	        return new ParsedRow(date, type, amount.setScale(2), category, description.isEmpty() ? null : description);
	    }

	    private static String cell(String[] row, Map<String, Integer> idx, String column) {
	        Integer i = idx.get(column);
	        if (i == null || i >= row.length || row[i] == null) return "";
	        return row[i].trim();
	    }
}

