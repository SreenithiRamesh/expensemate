# ExpenseMate

A full-stack personal and shared expense management platform built with **Java, Spring Boot, and PostgreSQL**, featuring JWT authentication, budget analytics, Splitwise-style expense splitting, settlements, deterministic debt simplification, and Gemini-powered financial assistance.

> Backend: complete through Milestone 18 (132 passing tests) · Frontend: in progress (React)

<br/>

![Java](https://img.shields.io/badge/Java%2021-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
![Spring Security](https://img.shields.io/badge/Spring%20Security-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-4169E1?style=for-the-badge&logo=postgresql&logoColor=white)
![Hibernate](https://img.shields.io/badge/Hibernate-59666C?style=for-the-badge&logo=hibernate&logoColor=white)
![Flyway](https://img.shields.io/badge/Flyway-CC0200?style=for-the-badge&logo=flyway&logoColor=white)
![JWT](https://img.shields.io/badge/JWT-000000?style=for-the-badge&logo=jsonwebtokens&logoColor=white)
![Swagger](https://img.shields.io/badge/Swagger-85EA2D?style=for-the-badge&logo=swagger&logoColor=black)
![Maven](https://img.shields.io/badge/Maven-C71A36?style=for-the-badge&logo=apachemaven&logoColor=white)
![JUnit5](https://img.shields.io/badge/JUnit5-25A162?style=for-the-badge&logo=junit5&logoColor=white)
![Gemini](https://img.shields.io/badge/Gemini%20API-8E75B2?style=for-the-badge&logo=googlegemini&logoColor=white)
![React](https://img.shields.io/badge/React-20232A?style=for-the-badge&logo=react&logoColor=61DAFB)
![Vite](https://img.shields.io/badge/Vite-646CFF?style=for-the-badge&logo=vite&logoColor=white)
![TailwindCSS](https://img.shields.io/badge/Tailwind%20CSS-38B2AC?style=for-the-badge&logo=tailwind-css&logoColor=white)

<br/>

## Overview

ExpenseMate solves two everyday money problems in one app:

- **Personal expense tracking** — log spending, set monthly budgets, manage recurring bills, and see where money actually goes.
- **Shared expense settlement** — split group expenses (equal, percentage, or custom amounts), automatically calculate who owes whom, and simplify group debts down to the fewest possible payments.

The project was built to demonstrate production-style Java backend engineering — not just CRUD, but real business logic: financial correctness under rounding, idempotent transactions, deterministic algorithms, and a disciplined boundary between deterministic computation and generative AI.

---

## Highlights

- JWT authentication with BCrypt password hashing and per-user data isolation
- Personal expense, budget, and recurring-expense management
- Shared groups with equal, percentage, and custom split strategies
- Deterministic monetary rounding — smallest-unit remainders are distributed correctly instead of leaking to floating-point error
- Dynamic balance engine — balances are derived from expenses, splits, and settlements rather than stored as mutable state
- Debt simplification — a deterministic greedy algorithm reduces group debts to a minimal settlement plan
- Full and partial settlements, idempotent by design (safe against duplicate/network-retry requests)
- Append-only group activity/audit history
- Dashboard aggregation — monthly spending, category breakdown, six-month trend, budget status
- Gemini-powered expense categorization — AI suggests, Java validates and owns the final record
- Gemini-powered monthly insights — Java computes the statistics, Gemini only narrates them in plain language
- OpenAPI / Swagger documentation with JWT bearer auth support
- Hardened CORS, global exception handling, and request validation
- 132 passing JUnit 5 + Mockito tests covering services, security, split logic, rounding, balances, debt simplification, settlements, and AI service boundaries

---

## Architecture

```
React Frontend                 [in progress]
        │
        │  REST · JWT Bearer
        ▼
Spring Boot REST API
        │
        ├── Authentication & Security   (Spring Security, JWT, BCrypt)
        ├── Expense & Budget Services
        ├── Recurring Expense Service
        ├── Group & Split Services      (Equal / Percentage / Custom)
        ├── Balance Engine              (derived, not stored)
        ├── Settlement Engine           (full / partial, idempotent)
        ├── Debt Simplification         (deterministic greedy algorithm)
        ├── Activity / Audit Timeline
        ├── Dashboard Aggregation
        └── AI Services ──────────────► Gemini API
        │
        ▼
PostgreSQL
        │
        └── Flyway migrations (version-controlled schema)
```

### The core design rule

> **Java owns financial correctness. AI assists, but is never the source of truth for money.**

This shows up in two places:

- **Expense categorization**: Gemini suggests a category from raw text (e.g. `"Swiggy 320"`), but the suggestion is validated against a fixed category enum before it's ever accepted — the AI cannot write an arbitrary value to a financial record.
- **Monthly insights**: Java pre-calculates all statistics (totals, category breakdowns, month-over-month deltas) and sends only the *aggregated summary* to Gemini. Gemini's job is strictly to turn verified numbers into a readable sentence — it never performs the arithmetic itself.

---

## Tech stack

**Backend** — Java 21 · Spring Boot 4.1.1 · Spring Security · JJWT · Spring Data JPA · Hibernate · PostgreSQL (H2 for tests) · Flyway · Springdoc OpenAPI · Google GenAI Java SDK (Gemini) · Maven · JUnit 5 · Mockito

**Frontend** *(in progress)* — React · Vite · Tailwind CSS · Axios · React Router · Recharts

**Planned deployment** — Vercel (frontend) · Render (backend) · Neon (PostgreSQL)

---

## Backend package structure

```
backend/src/main/java/com/expensemate/
├── config          # app + OpenAPI configuration
├── controller       # REST endpoints
├── service          # business logic
├── repository        # Spring Data JPA repositories
├── entity            # JPA entities
├── dto                # request/response DTOs
├── security          # JWT filter, auth config
├── exception          # global exception handling
└── ai                 # Gemini integration (categorizer, insights)
```

---

## Core domain design

### Split strategies

Shared expenses support three split types, each validated server-side:

| Split type | Rule |
|---|---|
| **Equal** | Divides evenly; remainder (in smallest currency unit) is distributed deterministically so totals always reconcile exactly |
| **Percentage** | Rejected unless individual percentages sum to exactly 100% |
| **Custom / Exact** | Rejected unless individual amounts sum to exactly the expense total |

### Balance engine

There is no mutable "balance" table. A user's net position in a group is always calculated from source-of-truth events:

```
net balance = (amount paid on behalf of the group)
            − (own share of all expenses)
            + (settlements paid)
            − (settlements received)
```

This avoids the class of bugs where a cached balance drifts out of sync with the underlying transactions.

### Debt simplification

Rather than showing every pairwise "who paid what for whom," a deterministic greedy algorithm matches debtors against creditors to minimize the number of actual payments needed to settle a group. It reduces unnecessary transfers efficiently — it is not claimed to guarantee the mathematically absolute minimum in every possible case, but the reduction is significant and consistent.

### Settlements

Settlements are idempotent by design: duplicate requests (from a network retry or a double-click) do not create duplicate financial records. Both full and partial settlements are supported, with validation preventing self-payment and confirming group membership for both parties.

---

## AI integration

| Feature | How it works |
|---|---|
| **Expense categorization** | User enters free text (e.g. `"Uber airport 560"`) → Gemini extracts amount/merchant/category → Java validates the category against a fixed enum → user confirms before it's saved |
| **Monthly insights** | Java computes exact statistics (totals, category %, month-over-month change) → only the aggregated summary is sent to Gemini → Gemini returns a natural-language explanation, which is cached and invalidated only when the underlying data changes |

Both features include daily usage limits and safe failure handling so the app degrades gracefully if the AI service is slow or unavailable.

---

## Getting started

### Prerequisites
- JDK 21
- Maven (or use the included Maven Wrapper)
- PostgreSQL (or rely on the default local/dev configuration)
- A Gemini API key (for AI features)

### Setup

```bash
git clone https://github.com/SreenithiRamesh/expensemate.git
cd expensemate/backend

# configure environment variables (see below)
./mvnw spring-boot:run
```

### Environment variables

```
DB_URL=jdbc:postgresql://localhost:5432/expensemate
DB_USERNAME=your_db_username
DB_PASSWORD=your_db_password
JWT_SECRET=your_jwt_signing_secret
GEMINI_API_KEY=your_gemini_api_key
CORS_ALLOWED_ORIGINS=http://localhost:5173
```

### API documentation

Once running, Swagger UI is available at:

```
http://localhost:8080/swagger-ui.html
```

---

## Testing

```bash
./mvnw test
```

132 tests currently cover: authentication, JWT filtering, budgets, personal expenses, split strategy logic and rounding, group authorization, balance calculation, debt simplification, settlements (including idempotency), activity logging, dashboard aggregation, and AI service boundary/failure handling.

---

## Roadmap

- [ ] React frontend (dashboard, groups, settlements, quick-add AI entry)
- [ ] GitHub Actions CI (automated test runs on push/PR)
- [ ] Refresh token rotation
- [ ] Rate limiting on auth endpoints
- [ ] Spring Boot Actuator health/metrics endpoints
- [ ] Deployment to Vercel + Render + Neon
- [ ] Screenshots and live demo link

---

## Author

**Sreenithi R.** — 2026 Computer Science graduate, building ExpenseMate as a Java full-stack portfolio project.
