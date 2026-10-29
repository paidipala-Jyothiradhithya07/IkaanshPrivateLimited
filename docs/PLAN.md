
 0.  Run `docker compose up --build`, hit `/actuator/health/liveness`, register + login  App runs  0.5 h 
 1. Read the code top-down (see order below); run `mvn test`       3 h 
 2. Manual test with the curl walkthrough + `samples/*.csv`; open `/ws-demo.html`   1 h 
 3. Push to GitHub, invite `iks-hiring`, verify a fresh clone runs from the README | Submission  0.5 h
 4. Interview prep: practise the Q&A sheet in `assignment-plan.xlsx`   2 h 

Reading order: `TransactionService` → `CsvRowParser` → `CsvImportService` → `TransactionQueryService` →
`RefreshTokenService` → `BearerTokenAuthenticator` → `AuditAspect` → `BalanceNotifier` → tests.