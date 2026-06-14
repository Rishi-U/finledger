# FinLedger

A production-style Digital Wallet & FinTech backend built with Spring Boot.

## Features

### Authentication & Authorization

* User Registration
* User Login
* JWT Access Token
* Refresh Token Support
* Logout with Token Revocation
* Role-Based Access Control (RBAC)

### Wallet Management

* Automatic Wallet Creation
* Wallet Balance Tracking
* Deposit Operations
* Withdraw Operations

### Money Transfers

* Wallet-to-Wallet Transfers
* Dynamic Fee Calculation
* Transaction Validation
* Double-Spend Protection
* Pessimistic Database Locking

### Ledger System

* Double Entry Accounting
* Debit Entries
* Credit Entries
* Ledger Validation
* Immutable Financial Records

### Idempotency

* Reference-ID Based Idempotency
* Duplicate Request Protection
* Race Condition Handling

### Refund Engine

* Full Refunds
* Partial Refunds
* Refund Authorization
* Refund Ledger Reversal

### Fraud Detection

* High Value Transaction Detection
* Rapid Transaction Detection
* Refund Abuse Detection
* Fraud Flagging

### Rate Limiting

* Per User Rate Limiting
* Global System Rate Limiting

### Event Driven Architecture

* Transfer Completion Events
* Refund Completion Events
* Event Listeners

### Security

* JWT Authentication Filter
* Spring Security Integration
* BCrypt Password Encryption
* Role Based Access Control

### API Documentation

* OpenAPI / Swagger UI

### Testing

* Unit Tests
* Integration Tests
* Concurrency Tests
* Idempotency Tests

---

## Technology Stack

* Java 21
* Spring Boot
* Spring Security
* Spring Data JPA
* Hibernate
* MySQL
* JWT
* Maven
* Swagger / OpenAPI
* JUnit 5
* Mockito

---

## Database Tables

* users
* wallets
* transactions
* ledger_entries
* refresh_tokens

---

## Key Financial Guarantees

### Double Entry Ledger

Every transaction creates:

* One Debit Entry
* One Credit Entry

Ledger must always balance.

### Idempotency

Duplicate transfer requests with the same referenceId return the original transaction instead of creating a new one.

### Concurrency Safety

Pessimistic database locking prevents:

* Double spending
* Race conditions
* Balance corruption

---

## Running The Project

### Clone Repository

```bash
git clone <repository-url>
```

### Configure Database

Update:

```properties
spring.datasource.url=
spring.datasource.username=
spring.datasource.password=
```

### Run

```bash
mvn spring-boot:run
```

---

## Swagger

After starting the application:

```text
http://localhost:8080/swagger-ui/index.html
```

---

## Author

Rishi

Digital Banking / FinTech Backend Project
