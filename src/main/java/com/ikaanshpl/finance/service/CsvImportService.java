package com.ikaanshpl.finance.service;

import java.io.IOException;
import com.ikaanshpl.finance.entity.TxType;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import com.opencsv.exceptions.CsvValidationException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.ikaanshpl.finance.audit.Audited;
import com.ikaanshpl.finance.controller.ApiException;
import com.ikaanshpl.finance.dto.Dtos.ImportResult;
import com.ikaanshpl.finance.dto.Dtos.RowError;
import com.ikaanshpl.finance.entity.Account;
import com.ikaanshpl.finance.entity.Category;
import com.ikaanshpl.finance.entity.Transaction;
import com.ikaanshpl.finance.repo.AccountRepository;
import com.ikaanshpl.finance.repo.CategoryRepository;
import com.ikaanshpl.finance.repo.TransactionRepository;
import com.ikaanshpl.finance.ws.BalanceChangedEvent;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;

@Service
public class CsvImportService {
	 public static final int MAX_ROWS = 10_000;

	    private final AccountRepository accounts;
	    private final CategoryRepository categories;
	    private final TransactionRepository transactions;
	    private final ApplicationEventPublisher events;
	    
	    

	    public CsvImportService(AccountRepository accounts, CategoryRepository categories,
				TransactionRepository transactions, ApplicationEventPublisher events) {
			super();
			this.accounts = accounts;
			this.categories = categories;
			this.transactions = transactions;
			this.events = events;
		}



		@Audited(entity = "TRANSACTION_IMPORT", action = "IMPORT")
	    @Transactional
	    public ImportResult importCsv(Long userId, Long accountId, MultipartFile file) {
	        if (file == null || file.isEmpty()) throw new ApiException(HttpStatus.BAD_REQUEST, "CSV file is required");
	        String filename = file.getOriginalFilename();
	        if (filename == null || !filename.toLowerCase(Locale.ROOT).endsWith(".csv")) {
	            throw new ApiException(HttpStatus.BAD_REQUEST, "Only .csv files are supported");
	        }

	        // Validate against the existing Accounts and Categories - both scoped to the caller.
	        Account account = accounts.findForUpdate(accountId, userId)
	                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Account not found"));
	        Map<String, Category> categoriesByName = categories.findByUserId(userId).stream()
	                .collect(Collectors.toMap(c -> c.getName().toLowerCase(Locale.ROOT), c -> c, (a, b) -> a));

	        List<Transaction> valid = new ArrayList<>();
	        List<RowError> errors = new ArrayList<>();
	        int total = 0;

	        try (CSVReader reader = new CSVReaderBuilder(
	                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8)).build()) {

	            Map<String, Integer> idx;
	            try {
	                idx = CsvRowParser.headerIndex(reader.readNext());
	            } catch (IllegalArgumentException e) {
	                throw new ApiException(HttpStatus.BAD_REQUEST, e.getMessage());
	            }

	            int rowNumber = 1; // the header is row 1, so the first data row is row 2 (what Excel shows)
	            String[] row;
	            while ((row = reader.readNext()) != null) {
	                rowNumber++;
	                if (CsvRowParser.isBlank(row)) continue;
	                if (++total > MAX_ROWS) {
	                    throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE, "Too many rows (max " + MAX_ROWS + ")");
	                }
	                try {
	                    CsvRowParser.ParsedRow p = CsvRowParser.parse(row, idx, categoriesByName);
	                    valid.add(Transaction.of(userId, account, p.category(),
	                            p.type(), p.amount(), p.date(), p.description()));
	                } catch (IllegalArgumentException e) {
	                    errors.add(new RowError(rowNumber, e.getMessage()));
	                }
	            }
	        } catch (IOException | CsvValidationException e) {
	            throw new ApiException(HttpStatus.BAD_REQUEST, "Could not read CSV: " + e.getMessage());
	        }

	        transactions.saveAll(valid);
	        valid.forEach(t -> account.apply(t.getType(), t.getAmount()));
	        if (!valid.isEmpty()) {
	            events.publishEvent(new BalanceChangedEvent(userId, account.getId(), account.getBalance()));
	        }
	        return new ImportResult(account.getId(), total, valid.size(), errors.size(), account.getBalance(), errors);
	    }
}
