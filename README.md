# Event Ledger API

A Spring Boot REST API for recording and querying financial transaction events.

\---

## Prerequisites

* Java 17
* Maven 3.8+



Verify your versions:

```bash
java -version
mvn -version
```

\---

## Install Dependencies

```bash
mvn clean install -DskipTests
```

\---

## Start the Application

```bash
mvn spring-boot:run
```

The application starts at **http://localhost:8080**

\---

## Run the Tests

```bash
mvn test
```

## API Endpoints

|Method|Endpoint|Description|
|-|-|-|
|POST|`/events`|Submit a transaction event|
|GET|`/events/{id}`|Get a single event by ID|
|GET|`/events?account={accountId}`|List all events for an account|
|GET|`/accounts/{accountId}/balance`|Get account balance|

### Sample Request — POST /events

```bash
curl --location 'http://localhost:8080/events' \\

\--header 'Content-Type: application/json' \\

\--data '{

&#x20;   "eventId": "evt-001",

&#x20;   "accountId": "acct-221",

&#x20;   "type": "CREDIT",

&#x20;   "amount": 0.00,

&#x20;   "currency": "INR",

&#x20;   "eventTimestamp": "2026-05-24T14:46:11Z",

&#x20;   "metadata": {

&#x20;       "source": "mainframe-batch",

&#x20;       "batchId": "B-9042"

&#x20;   }

}''```

\---

## H2 Console (in-browser database viewer)

Available at **http://localhost:8080/h2-console**

|Field|Value|
|-|-|
|JDBC URL|`jdbc:h2:mem:EVENT\_LEDGER`|
|Username|`sa`|
|Password|*(leave blank)*|



