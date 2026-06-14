# FinLedger - Digital Banking & FinTech System

A production-style Digital Banking and FinTech backend built with Spring Boot, PostgreSQL, JWT Authentication, Double-Entry Ledger Accounting, Refund Processing, Fraud Detection, Rate Limiting, and Event-Driven Architecture.

---

## Features

### Authentication & Authorization

* User Registration
* User Login
* JWT Access Token
* Refresh Token Support
* Logout with Token Revocation
* Role-Based Access Control (RBAC)
* BCrypt Password Encryption

### Wallet Management

* Automatic Wallet Creation
* Wallet Balance Tracking
* Deposit Operations
* Withdraw Operations
* System Wallet for Fee Collection

### Money Transfers

* Wallet-to-Wallet Transfers
* Dynamic Fee Calculation
* Transaction Validation
* Double-Spend Protection
* Atomic Transactions
* Pessimistic Database Locking

### Ledger System

* Double-Entry Accounting
* Debit Entries
* Credit Entries
* Ledger Validation
* Immutable Financial Records

### Idempotency

* Reference-ID Based Idempotency
* Duplicate Request Protection
* Race Condition Handling
* Final Transaction State Resolver

### Refund Engine

* Full Refunds
* Partial Refunds
* Refund Authorization
* Ledger Reversal
* Audit Trail Preservation

### Fraud Detection

* High Value Transaction Detection
* Rapid Transaction Detection
* Refund Abuse Detection
* Fraud Flagging

### Rate Limiting

* Per User Rate Limiting
* Global System Rate Limiting

### Event-Driven Architecture

* Transfer Completion Events
* Refund Completion Events
* Notification Listeners
* Audit Event Listeners
* Fraud Analytics Listeners
* Asynchronous Event Processing

### Security

* JWT Authentication Filter
* Spring Security Integration
* Role-Based Access Control
* Refresh Token Revocation
* Password Encryption using BCrypt

### API Documentation

* OpenAPI 3
* Swagger UI Documentation

### Testing

* Unit Tests
* Integration Tests
* Concurrency Tests
* Idempotency Tests
* Service Layer Tests
* JWT Tests

---

## Technology Stack

* Java 21
* Spring Boot 3
* Spring Security
* Spring Data JPA
* Hibernate
* PostgreSQL
* JWT (JSON Web Tokens)
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

## Architecture

```text
Controller
     ↓
Service Layer
     ↓
Repository Layer
     ↓
PostgreSQL Database
```

### Cross-Cutting Concerns

* Security
* Event Handling
* Fraud Detection
* Rate Limiting
* Global Exception Handling
* Logging

---

## Key Financial Guarantees

### Double-Entry Ledger

Every transaction creates balanced ledger entries.

Example:

```text
Sender Wallet   → DEBIT  ₹102
Receiver Wallet → CREDIT ₹100
System Wallet   → CREDIT ₹2
```

The ledger must always balance.

---

### Idempotency

Duplicate transfer requests with the same `referenceId` return the original transaction instead of creating a new one.

---

### Concurrency Safety

Pessimistic database locking prevents:

* Double Spending
* Race Conditions
* Balance Corruption

---

## API Documentation

After starting the application:

Swagger UI:

```text
http://localhost:8080/swagger-ui/index.html
```

OpenAPI JSON:

```text
http://localhost:8080/v3/api-docs
```

---

## Running the Project

### Clone Repository

```bash
git clone https://github.com/Rishi-U/finledger.git
cd finledger
```

### Configure Database

Update `application.properties`:

```properties
spring.datasource.url=
spring.datasource.username=
spring.datasource.password=
jwt.secret=
```

### Build Project

```bash
mvn clean install
```

### Run Application

```bash
mvn spring-boot:run
```

---

## Running Tests

```bash
mvn test
```

---

## Future Improvements

* Docker Support
* Redis Caching
* Kafka Integration
* Email Notifications
* Multi-Currency Wallets
* Admin Dashboard
* Kubernetes Deployment
* CI/CD Pipeline

---

### Swagger UI

*Add Swagger screenshots here.*

---

## Author

**Rishi U**

GitHub: https://github.com/Rishi-U

---

## License

This project is licensed under the MIT License.
