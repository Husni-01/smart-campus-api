# Smart Campus API

> A JAX-RS RESTful service for campus Room and Sensor management, built with **Jersey 2.x** and **Embedded Apache Tomcat 9**.

---

## Overview

This API provides a robust, scalable interface for campus facilities managers and automated building systems to interact with campus infrastructure data. It manages two primary resource hierarchies:

- **Rooms** — physical spaces on campus (`/api/v1/rooms`)
- **Sensors** — hardware devices deployed within rooms (`/api/v1/sensors`)
- **Sensor Readings** — historical measurement logs per sensor (`/api/v1/sensors/{id}/readings`)

### Technology Stack

| Component          | Technology                       |
|--------------------|----------------------------------|
| Language           | Java 11                          |
| JAX-RS Framework   | Jersey 2.40 (Reference Impl.)    |
| Servlet Container  | Embedded Apache Tomcat 9.0.76    |
| JSON Serialization | Jackson (via Jersey Media)       |
| Build Tool         | Maven 3.x                        |
| Data Storage       | In-Memory (`ConcurrentHashMap`)  |

---

## How to Build and Run

### Prerequisites
- **Java 11+** installed and on your PATH
- **Maven 3.6+** installed and on your PATH

### Step 1 — Clone the Repository
```bash
git clone https://github.com/<your-username>/smart-campus-api.git
cd smart-campus-api
```

### Step 2 — Build the Project
```bash
mvn clean compile
```

### Step 3 — Start the Server
```bash
mvn exec:java
```

The server will start on **http://localhost:8080/api/v1**. You will see the following output:
```
Smart Campus API started successfully!
Base URL : http://localhost:8080/api/v1
Rooms    : http://localhost:8080/api/v1/rooms
Sensors  : http://localhost:8080/api/v1/sensors
```

### Step 4 — Test with Postman
1. Open **Postman** and create a new request.
2. Set the URL to `http://localhost:8080/api/v1/<endpoint>` (see API Reference below).
3. For **POST** requests, go to the **Headers** tab and set `Content-Type: application/json`.
4. For **POST** requests, go to the **Body** tab, select **raw**, and choose **JSON** from the dropdown.
5. Paste the JSON body as shown in the examples below and click **Send**.

### Optional — Build a Fat JAR
```bash
mvn clean package
java -jar target/smart-campus-api-1.0.0.jar
```

---

## API Reference & Sample curl Commands

### 1. Discovery Endpoint
```bash
curl -X GET http://localhost:8080/api/v1/
```

### 2. Get All Rooms
```bash
curl -X GET http://localhost:8080/api/v1/rooms
```

### 3. Create a New Room
```bash
curl -X POST http://localhost:8080/api/v1/rooms \
  -H "Content-Type: application/json" \
  -d '{"id":"ENG-201","name":"Engineering Lab","capacity":40}'
```

### 4. Get a Specific Room
```bash
curl -X GET http://localhost:8080/api/v1/rooms/LIB-301
```

### 5. Delete a Room (empty room — succeeds)
```bash
curl -X DELETE http://localhost:8080/api/v1/rooms/HALL-001
```

### 6. Delete a Room with Sensors (triggers 409 Conflict)
```bash
curl -X DELETE http://localhost:8080/api/v1/rooms/LIB-301
```

### 7. Get All Sensors
```bash
curl -X GET http://localhost:8080/api/v1/sensors
```

### 8. Get Sensors Filtered by Type
```bash
curl -X GET "http://localhost:8080/api/v1/sensors?type=CO2"
```

### 9. Create a New Sensor (with a valid roomId)
```bash
curl -X POST http://localhost:8080/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"id":"LIGHT-001","type":"Lighting","status":"ACTIVE","currentValue":75.0,"roomId":"LIB-301"}'
```

### 10. Create a Sensor with an Invalid roomId (triggers 422)
```bash
curl -X POST http://localhost:8080/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"id":"TEMP-999","type":"Temperature","status":"ACTIVE","currentValue":20.0,"roomId":"FAKE-999"}'
```

### 11. Add a Reading to an Active Sensor
```bash
curl -X POST http://localhost:8080/api/v1/sensors/TEMP-001/readings \
  -H "Content-Type: application/json" \
  -d '{"value":24.7}'
```

### 12. Add a Reading to a MAINTENANCE Sensor (triggers 403)
```bash
curl -X POST http://localhost:8080/api/v1/sensors/OCC-001/readings \
  -H "Content-Type: application/json" \
  -d '{"value":15.0}'
```

### 13. Get All Readings for a Sensor
```bash
curl -X GET http://localhost:8080/api/v1/sensors/TEMP-001/readings
```

---

## Conceptual Report

### Part 1 — Service Architecture & Setup

**Q: What is the default lifecycle of a JAX-RS Resource class? How does this impact in-memory data management?**

By default, JAX-RS creates a **new instance of a resource class for every incoming HTTP request** (per-request lifecycle). This means that any instance variables declared directly on the resource class are destroyed after the request completes — they cannot safely hold shared state. If data were stored as instance fields, two concurrent requests would each see a fresh, empty object, causing all persisted data to be lost.

To safely manage shared in-memory state across all requests, a **Singleton pattern** is required. In this project, `InMemoryDataStore` is implemented as a static Singleton, meaning only one instance exists for the entire application's lifetime. All resource classes retrieve this single instance via `InMemoryDataStore.getInstance()`. Furthermore, the data maps use `ConcurrentHashMap` instead of a plain `HashMap`. `ConcurrentHashMap` is designed for concurrent access by multiple threads, providing thread-safe read and write operations without the need for explicit `synchronized` blocks on most operations, thereby preventing race conditions and data corruption when multiple requests arrive simultaneously.

---

**Q: Why is HATEOAS considered a hallmark of advanced RESTful design? How does it benefit client developers?**

HATEOAS (Hypermedia as the Engine of Application State) is a key REST constraint where the server embeds navigational links within its responses, guiding clients to the next available actions. Instead of clients needing to memorise or hardcode all API URLs, the server acts like a map — each response tells the client where it can go next.

This benefits client developers in several ways: First, it makes the API **self-discoverable** — a developer can start at the root endpoint and explore the entire API by following links, without reading extensive external documentation. Second, it **decouples clients from URL structures** — if the server changes a path (e.g., `/api/v1/rooms` becomes `/api/v2/rooms`), clients that follow links dynamically do not break. Third, it enables **stateless navigation** — the client does not need to store or remember API paths, as the server provides them on demand.

---

### Part 2 — Room Management

**Q: What are the implications of returning only IDs versus full room objects in a list response?**

Returning only **IDs** reduces the response payload size significantly, saving network bandwidth and making the list response fast. However, it forces the client to make a separate GET request for each ID to retrieve the full details — this is known as the **N+1 request problem**, which increases latency and server load under heavy traffic.

Returning **full room objects** increases the response size but eliminates the need for follow-up requests, making it ideal for clients that need to display or process all room data at once (e.g., a dashboard). The optimal approach, used in this implementation, is to return the full objects in the list but ensure they contain only the most relevant fields (a "summary" view), reserving the detailed endpoint (`GET /rooms/{id}`) for the complete representation, including sub-resource links.

---

**Q: Is the DELETE operation idempotent in your implementation? Justify with multiple calls.**

Yes, DELETE is **idempotent** in this implementation. Idempotency means that making the same request multiple times produces the same application state as making it once.

- **1st DELETE `/api/v1/rooms/HALL-001`**: The room exists and has no sensors, so it is removed from the data store. The response is `204 No Content`.
- **2nd DELETE `/api/v1/rooms/HALL-001`**: The room no longer exists. The response is `404 Not Found`.

Both the 1st and 2nd calls result in the same **state** — the room is absent from the system. The HTTP response codes differ (204 vs 404), but the server's state is identical. This aligns with the REST specification, which defines idempotency in terms of the server state, not the response code.

---

### Part 3 — Sensor Operations & Filtering

**Q: Explain the consequences if a client sends data in a format other than `application/json` to a `@Consumes(MediaType.APPLICATION_JSON)` endpoint.**

The `@Consumes(MediaType.APPLICATION_JSON)` annotation instructs the JAX-RS runtime to match this method only when the incoming request's `Content-Type` header is `application/json`. If a client sends a request with `Content-Type: text/plain` or `Content-Type: application/xml`, the JAX-RS runtime (Jersey) will be unable to find a matching resource method, and will automatically return **HTTP 415 Unsupported Media Type** before the method body is ever executed. No custom error handling code is required for this scenario — the framework enforces it declaratively. This provides a clean contract between the client and the API.

---

**Q: Why is `@QueryParam` preferred over path-based filtering (e.g., `/sensors/type/CO2`)?**

Using `@QueryParam` for filtering (e.g., `GET /sensors?type=CO2`) is superior for several reasons:

1. **Semantics**: A path like `/sensors/type/CO2` implies that `type/CO2` is a distinct sub-resource (a new noun in the hierarchy), which it is not — it is a filter applied to the `/sensors` collection.
2. **Composability**: Multiple query parameters can be combined naturally (e.g., `?type=CO2&status=ACTIVE&roomId=LAB-105`). Achieving the same with path parameters would require a complex and unreadable URL structure.
3. **Optionality**: Query parameters are inherently optional. `GET /sensors` returns all sensors, while `GET /sensors?type=CO2` returns a filtered subset. With path parameters, you would need two separate method signatures.
4. **Caching and RESTful convention**: The base collection URI `/sensors` remains stable and cacheable, while the filter is a transient query modifier.

---

### Part 4 — Deep Nesting with Sub-Resources

**Q: What are the architectural benefits of the Sub-Resource Locator pattern?**

The Sub-Resource Locator pattern, used for `/sensors/{sensorId}/readings`, delegates logic to a dedicated `SensorReadingResource` class. This provides several key benefits:

1. **Separation of Concerns**: `SensorResource` handles sensor management; `SensorReadingResource` handles historical data. Each class has a single, clear responsibility, aligning with the **Single Responsibility Principle**.
2. **Reduced Complexity**: Without the pattern, `SensorResource` would contain methods for both sensors AND readings, growing into a "god class" that is difficult to understand and maintain. The locator keeps each class focused.
3. **Testability**: Each sub-resource class can be unit-tested in isolation by constructing it directly with a specific `sensorId`, without needing to simulate the full HTTP lifecycle.
4. **Reusability**: The `SensorReadingResource` class could, in principle, be reused as a sub-resource from multiple parent locators, promoting code reuse.
5. **Scalability**: In large APIs with dozens of nested resources, the locator pattern prevents a single file from growing to thousands of lines, making the codebase navigable for large teams.

---

### Part 5 — Error Handling & Exception Mapping

**Q: Why is HTTP 422 more semantically accurate than 404 when a POST payload references a missing resource?**

- **404 Not Found** means the **endpoint URL** that the client requested does not exist on the server. It signals a routing problem.
- **422 Unprocessable Entity** means the **endpoint was found and the request was syntactically valid (well-formed JSON)**, but the semantic content inside the payload is logically incorrect.

When a client POSTs a sensor with `"roomId": "FAKE-999"`, the URL `/api/v1/sensors` is perfectly valid and found. The JSON is syntactically correct. The problem is that the *data value* `"FAKE-999"` refers to a room that does not exist — a **referential integrity violation**. Using 404 would mislead the client into thinking they requested a wrong URL. Using 422 precisely communicates: "Your request reached the right place, but the data inside it is semantically invalid." This helps clients debug faster and is the industry-standard response for validation failures.

---

**Q: From a cybersecurity standpoint, what are the risks of exposing Java stack traces to external API consumers?**

Exposing stack traces to external clients represents a significant security vulnerability known as **Information Disclosure**. A stack trace can reveal:

1. **Internal package and class names** (e.g., `com.smartcampus.repository.InMemoryDataStore`) — exposing the application's architecture and design patterns.
2. **Third-party library names and versions** (e.g., `org.glassfish.jersey 2.40`) — allowing attackers to look up known CVEs (Common Vulnerabilities and Exposures) for those specific versions.
3. **Server file paths** (e.g., `/home/ubuntu/app/smart-campus-api/...`) — revealing the server's directory structure for potential path traversal attacks.
4. **Internal logic and control flow** — the exact sequence of method calls can reveal business logic, which attackers can exploit to craft targeted malicious payloads.

The `GlobalExceptionMapper` addresses this by catching all unhandled exceptions, logging the full trace securely on the server, and returning only a sanitised, generic `500 Internal Server Error` message to the client — following the principle of **minimal information disclosure**.

---

**Q: Why is it advantageous to use JAX-RS filters for cross-cutting concerns like logging?**

Using a `ContainerRequestFilter` / `ContainerResponseFilter` for logging has significant advantages over manually inserting `Logger.info()` calls in every resource method:

1. **DRY Principle (Don't Repeat Yourself)**: Logging logic is written once in the filter and automatically applied to every request and response, across all current and future endpoints.
2. **Guaranteed Coverage**: A developer adding a new resource method cannot accidentally forget to add logging — it is enforced globally by the filter infrastructure.
3. **Separation of Concerns**: Resource classes contain only business logic. Logging, authentication, CORS headers, and compression are cross-cutting concerns that belong in filters, not intermingled with domain code.
4. **Consistency**: All logs are formatted identically, making server log analysis predictable and easier to parse with log aggregation tools.
5. **Maintainability**: Changing the log format, adding a request ID, or switching logging libraries requires editing only one file (`LoggingFilter.java`), not dozens of resource files.
