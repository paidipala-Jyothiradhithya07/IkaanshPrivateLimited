# Smart Financial Management System - Expense Tracking API

This is my submission for the Ikaansh Edutech Java Development assignment:
"Expense Tracking API Enhancement with CSV Import and Audit Logging".

The backend is built with Java 17, Spring Boot 3 and PostgreSQL.

## What I implemented

Main goals
- CSV import of transactions using OpenCSV. Each row is checked against the existing accounts and categories.
- Transaction history API with pagination, date range filter, category filter and monthly spending summary.
- JWT login with refresh token rotation and safe token invalidation (logout and logout from all devices).
- Integration tests using JUnit 5 and Testcontainers.

Bonus goals
- Multi-stage Dockerfile and a basic Kubernetes Deployment file.
- Audit logging using Spring AOP, saved in a separate table with timestamps.
- Real-time balance updates using WebSocket and STOMP.

## What you need installed

- JDK 17
- Maven (or the Maven that comes with STS)
- PostgreSQL 14 or higher
- Docker Desktop (only needed for the Docker option and for running the tests)

## Database setup

Create the database and the user in PostgreSQL:

```sql
CREATE USER finance WITH PASSWORD 'finance';
CREATE DATABASE finance OWNER finance;
```

The tables are created automatically when the application starts for the first time.

If you want to create them yourself, run the schema file and start the app with `DDL_AUTO=validate`:

```
psql -U finance -d finance -f db/schema.sql
```

The tables are users, accounts, categories, transactions, refresh_tokens and audit_events.

## How to run the backend

Option 1: Docker (easiest, no Java or PostgreSQL needed)

```
docker compose up --build
```

The API starts on http://localhost:8080 and PostgreSQL on port 5432.

Option 2: Maven

Make sure PostgreSQL is running and the database above exists, then:

```
mvn spring-boot:run
```

Option 3: STS or Eclipse

Right click FinanceApplication, then Run As, then Spring Boot App.

To check that it started, open http://localhost:8080/actuator/health in the browser. It should show UP.

## Configuration

The default values work for local setup. To change them, set these environment variables.

- DB_URL (default jdbc:postgresql://localhost:5432/finance)
- DB_USER and DB_PASSWORD (default finance and finance)
- DDL_AUTO (default update)
- JWT_SECRET (at least 32 characters, please change it for real use)
- JWT_ACCESS_TTL (access token life in seconds, default 900)
- JWT_REFRESH_TTL_DAYS (default 7)
- CORS_ORIGINS (default http://localhost:3000 and http://localhost:5173)

## How to run the frontend

The React frontend is in a separate project. Start it with:

```
npm install
npm start
```

Set the API base URL in the frontend to http://localhost:8080. Every request after login must send the header
`Authorization: Bearer <accessToken>`.

I also added a small test page for the live balance feature at http://localhost:8080/ws-demo.html.
Paste the access token there, click Connect, then create a transaction and the new balance appears.

## How to run the tests

Docker must be running because the tests start a real PostgreSQL container.

```
mvn test
```

The tests cover these things:
- CSV row validation (unit test)
- creating and deleting transactions and checking the balance
- 20 requests at the same time on one account, balance must stay correct
- CSV import with valid and invalid rows, wrong header, wrong file type
- history pagination, date filter, category filter, monthly summary
- login, refresh token rotation, reuse of an old token, logout, logout all
- 5 refresh requests at the same time with the same token, only one must succeed

## API endpoints

Login is not needed for register, login, refresh and logout. All other endpoints need the access token.

```
POST   /api/auth/register
POST   /api/auth/login
POST   /api/auth/refresh
POST   /api/auth/logout
POST   /api/auth/logout-all

POST   /api/accounts
GET    /api/accounts
GET    /api/accounts/{id}

POST   /api/categories
GET    /api/categories

POST   /api/transactions
DELETE /api/transactions/{id}
POST   /api/transactions/import
GET    /api/transactions
GET    /api/transactions/summary/monthly

GET    /api/audit
```

WebSocket: connect to ws://localhost:8080/ws with the access token in the Authorization header of the CONNECT frame,
then subscribe to /user/queue/balance.

Query parameters for GET /api/transactions: accountId, categoryId, type, from, to, page, size, sort.
Dates are in yyyy-MM-dd format. Sorting is allowed only on txDate, amount, createdAt and id.

## Example requests

```
BASE=http://localhost:8080

curl -X POST $BASE/api/auth/register -H "Content-Type: application/json" \
  -d '{"name":"Jyothi","email":"jyothi@example.com","password":"Passw0rd!"}'

curl -X POST $BASE/api/auth/login -H "Content-Type: application/json" \
  -d '{"email":"jyothi@example.com","password":"Passw0rd!"}'
```

Copy the accessToken and refreshToken from the response, then:

```
TOKEN=<accessToken>

curl -X POST $BASE/api/accounts -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" -d '{"name":"Main","openingBalance":1000}'

curl -X POST $BASE/api/transactions/import -H "Authorization: Bearer $TOKEN" \
  -F accountId=1 -F file=@samples/transactions.csv

curl "$BASE/api/transactions?from=2026-09-01&to=2026-10-31&page=0&size=10&sort=amount,desc" \
  -H "Authorization: Bearer $TOKEN"

curl "$BASE/api/transactions/summary/monthly" -H "Authorization: Bearer $TOKEN"

curl $BASE/api/audit -H "Authorization: Bearer $TOKEN"
```

## CSV file format

```
date,type,amount,category,description
2026-08-01,INCOME,50000.00,Salary,August salary
2026-08-03,EXPENSE,1200.50,Food,Groceries
```

Rules:
- The first line must be the header. Column order does not matter.
- date, type, amount and category are required. description is optional.
- date must be yyyy-MM-dd. type must be INCOME or EXPENSE.
- amount must be more than 0 with at most 2 decimal places.
- category must be one of the user's own categories. New users get Salary, Food, Rent, Transport, Utilities,
  Entertainment and Other automatically.
- The account is given with the accountId parameter and must belong to the logged in user.
- Valid rows are saved. Invalid rows are not saved and are returned in the response with the row number
  (the header is row 1).
- Maximum 10,000 rows and 5 MB.

Sample files are in the samples folder. transactions-with-errors.csv shows how errors are reported.

## Project structure

```
src/main/java/com/ikaanshpl/finance
    FinanceApplication
    controller   REST controllers and error handling
    service      business logic and CSV import
    repo         repositories
    entity       JPA entities
    dto          request and response classes
    security     JWT and authentication
    config       security and WebSocket config
    audit        audit annotation and aspect
    ws           WebSocket classes
src/main/resources   application.yml and ws-demo.html
src/test/java        tests
db/schema.sql
Dockerfile
docker-compose.yml
k8s/deployment.yaml
samples
```

## Important design points

Balance and concurrency
Before changing a balance, the account row is locked in the database (SELECT ... FOR UPDATE). So if many requests
come at the same time for one account, none of the updates is lost.

CSV import
Valid rows and the balance update are saved in one transaction. A bad row does not stop the import, it is just reported.
Accounts and categories are always looked up for the logged in user only.

Refresh tokens
The refresh token is a random value and only its hash is stored in the database. A refresh token can be used only once.
If an old token is used again, the whole session is revoked. The access token has a session id, so after logout it
stops working immediately.

History and summary
Both use the same filter code. Monthly totals use a "ym" column like 2026-10 saved with each transaction, so
the monthly group by is simple and fast.

Audit logging
Methods marked with @Audited are logged by an aspect after they finish successfully. The log is saved in the
audit_events table with user, action, entity id and time.

Real-time update
The WebSocket message is sent only after the database commit, and only to the owner of the account.

Money values use BigDecimal in Java and NUMERIC(19,2) in the database.

## Docker and Kubernetes

```
docker build -t finance-api:1.0.0 .
kubectl apply -f k8s/deployment.yaml
```

Change DB_URL and the secret values in the yaml file before using it. The Dockerfile builds the jar in the first
stage and copies only the jar into a small Java runtime image in the second stage.

## Limitations

- Old expired refresh tokens are not deleted automatically.
- There is no login attempt limit.
- Importing the same CSV twice will create the transactions twice.
- The WebSocket broker is in memory, so it works for one application instance only.
