# Expense Splitter App

A Splitwise-style expense splitting service built with Java 17, Spring Boot 3, Spring Security, JWT authentication, and MySQL. Designed to run straight from IntelliJ with no Lombok involved, so there's no annotation-processor/JDK version trap to hit.

## Stack

- **Java 17** / **Spring Boot 3.3** (Web, Data JPA, Security, Validation)
- **Spring Security + JWT** (`jjwt` 0.12.x) — stateless authentication, no sessions
- **MySQL 8** — persistent storage
- **No Lombok** — every entity/DTO has plain getters/setters, so there's nothing for IntelliJ's annotation processor to choke on

## Features

- User registration & login, returning a JWT on success
- Create expense groups and add members **by name** — if the name matches a registered username it's linked to that account, otherwise it's added as a guest member with no account required (like adding a friend on Splitwise before they've signed up)
- A monthly spending report per logged-in user: how much they paid, how much their own share came to, and the net, both overall and broken down per group
- Add expenses with three split strategies:
  - **EQUAL** — split evenly across participants (defaults to all group members)
  - **PERCENTAGE** — split by custom percentages that must sum to 100
  - **CUSTOM** — split by exact amounts that must sum to the total
- Per-group balances (who's owed money, who owes money)
- A simplified settlement plan (minimum set of "who pays whom how much" transactions) computed with a greedy debt-simplification algorithm
- Global JSON error handling (400/401/403/404/409/500)
- All endpoints except `/api/auth/**` and `/api/health` require a valid JWT

## Project layout

```
src/main/java/com/example/expensesplitter/
├── ExpenseSplitterApplication.java
├── config/SecurityConfig.java            # filter chain, CORS, password encoder, auth manager
├── security/
│   ├── JwtService.java                   # token generation/validation
│   ├── JwtAuthenticationFilter.java      # reads Authorization header on every request
│   └── UserDetailsServiceImpl.java
├── controller/                           # REST endpoints
├── dto/                                  # request/response payloads
├── entity/                               # User, ExpenseGroup, GroupMember, Expense, ExpenseShare, SplitType
├── exception/                            # custom exceptions + @RestControllerAdvice
├── repository/                           # Spring Data JPA repositories
└── service/
    ├── AuthService.java
    ├── GroupService.java
    ├── ExpenseService.java               # split calculation logic
    └── SettlementService.java            # balances + debt simplification
src/main/resources/application.yml
docker-compose.yml                        # MySQL only — the app itself runs from IntelliJ
```

## Running it in IntelliJ

**1. Start MySQL** (pick one):

```bash
docker compose up -d
```

...or install MySQL locally and create a database/user matching `application.yml`'s defaults (`expense_splitter` / `expense_user` / `expense_pass`), or override via environment variables.

**2. Open the project in IntelliJ**

- `File → Open` and select the project folder (the one containing `pom.xml`).
- Let IntelliJ import the Maven project (it'll download dependencies automatically).
- Make sure **Project SDK is Java 17** (`File → Project Structure → Project`).

**3. Run it**

- Open `ExpenseSplitterApplication.java` and click the green run arrow, or
- Use the Maven tool window → `Lifecycle → spring-boot:run`.

The app starts on **port 8081** (so it won't collide with the URL shortener project on 8080, if you're running both).

No Lombok is involved anywhere in this project, so there's nothing for IntelliJ's annotation processor to fight with — if it still fails to start, it'll be a MySQL connectivity issue (check the console for a `Communications link failure` or access-denied error) rather than the `TypeTag :: UNKNOWN` class of problem.

### Running without IntelliJ

```bash
mvn clean package
java -jar target/expense-splitter.jar
```

## Configuration

All overridable via environment variables (see `application.yml`):

| Variable | Default | Purpose |
|---|---|---|
| `SERVER_PORT` | `8081` | App port |
| `DB_HOST` / `DB_PORT` / `DB_NAME` | `localhost` / `3306` / `expense_splitter` | MySQL connection |
| `DB_USERNAME` / `DB_PASSWORD` | `expense_user` / `expense_pass` | MySQL credentials |
| `JWT_SECRET` | a placeholder base64 string | **Change this in any real deployment** — must be a base64-encoded key of at least 256 bits for HS256 |
| `JWT_EXPIRATION_MS` | `86400000` (24h) | Token lifetime |

## Web UI

There's a small standalone web page at `webapp/index.html` that gives you register/login, group creation, adding expenses (all three split types), balances, and the settlement plan — all with buttons instead of curl commands.

It's a plain HTML/JS file with no build step, so just open it directly:

```bash
open webapp/index.html        # macOS
```

or double-click it in Finder/Explorer. On first load it defaults to `http://localhost:8081` as the API base — change that field in the top bar if you're running the app on a different port.

It talks to your locally running Spring Boot app over `fetch()`, so the backend must already be running (see "Running it in IntelliJ" above) before you open the page. The app's CORS config already allows this.

## Guest members (no account required)

When you create a group or add a member, you give a **name**, not necessarily a username:

- If that name matches an existing registered username, the new member is linked to that real account (they can log in and see the group themselves).
- If it doesn't match anyone, a **guest member** is created — just a display name, no login, no password. You can still mark them as the payer of an expense, include them in a split, and see them in balances/settlements. They simply can't log in until/unless they register with that same username and get re-added.

This means you (the one logged-in user) can create a whole group and log every expense for friends who've never touched the app.

## Monthly spending report

`GET /api/reports/monthly?month=2026-10` (the `month` param is optional — it defaults to the current month) returns, for the logged-in user, across every group they belong to:

- `totalPaid` — the total they fronted/paid for that month
- `totalShare` — the total they're personally responsible for (their share of every expense), that month
- `net` — `totalPaid - totalShare` (positive means they're owed money net for the month; negative means they owe)
- `groups` — the same breakdown per group

This is based on each expense's creation date, not a separate "transaction date" field.

## API walkthrough

### 1. Register

```bash
curl -X POST http://localhost:8081/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"alice","email":"alice@example.com","password":"password123"}'
```

Returns a JWT. Register a second user too (e.g. `bob`) so you have someone to split expenses with.

### 2. Login

```bash
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"alice","password":"password123"}'
```

```json
{ "token": "eyJhbGciOi...", "username": "alice", "email": "alice@example.com" }
```

Use this token as `Authorization: Bearer <token>` on every call below.

### 3. Create a group

```bash
curl -X POST http://localhost:8081/api/groups \
  -H "Authorization: Bearer <alice-token>" \
  -H "Content-Type: application/json" \
  -d '{"name":"Goa Trip","members":["bob"]}'
```

Alice (the creator) is added automatically; `bob` is added because he's listed.

### 4. Add an expense (equal split)

```bash
curl -X POST http://localhost:8081/api/groups/1/expenses \
  -H "Authorization: Bearer <alice-token>" \
  -H "Content-Type: application/json" \
  -d '{
    "description": "Hotel",
    "amount": 100.00,
    "splitType": "EQUAL"
  }'
```

With no `participants` listed, it splits across all group members (alice, bob → 50.00 each). Alice paid, so she's owed 50.00.

### 5. Add an expense (percentage split)

```bash
curl -X POST http://localhost:8081/api/groups/1/expenses \
  -H "Authorization: Bearer <bob-token>" \
  -H "Content-Type: application/json" \
  -d '{
    "description": "Dinner",
    "amount": 60.00,
    "splitType": "PERCENTAGE",
    "percentages": {"alice": 40, "bob": 60}
  }'
```

### 6. Add an expense (custom split)

```bash
curl -X POST http://localhost:8081/api/groups/1/expenses \
  -H "Authorization: Bearer <alice-token>" \
  -H "Content-Type: application/json" \
  -d '{
    "description": "Taxi",
    "amount": 25.00,
    "splitType": "CUSTOM",
    "customShares": {"alice": 10.00, "bob": 15.00}
  }'
```

### 7. Check balances

```bash
curl http://localhost:8081/api/groups/1/balances -H "Authorization: Bearer <alice-token>"
```

```json
[
  {"name": "alice", "netBalance": 39.00},
  {"name": "bob", "netBalance": -39.00}
]
```

### 8. Get the settlement plan

```bash
curl http://localhost:8081/api/groups/1/settlements -H "Authorization: Bearer <alice-token>"
```

```json
[{"fromName": "bob", "toName": "alice", "amount": 39.00}]
```

## Notes / things to harden for production

- Add role-based authorization if you need admin-only operations (e.g. deleting a group).
- Add refresh tokens — right now a token is just valid until it expires (24h by default).
- Add a `DELETE`/edit endpoint for expenses, with recalculation implications considered.
- Move `JWT_SECRET` and DB credentials to a secrets manager; never commit real secrets.
- Add Flyway/Liquibase migrations instead of `ddl-auto: update` once the schema stabilizes.
- Add pagination to `GET /api/groups/{id}/expenses` for large groups.
