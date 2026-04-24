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

---

### Part 1: Service Architecture & Setup

**1.1:  JAX-RS Resource Lifecycle and In-Memory Data Management**

By default, JAX-RS creates a completely new instance of a resource class every single time a request hits the server. So if ten users call `/api/v1/rooms` at the same time, the runtime spins up ten separate `RoomResource` objects, handles each request, and then discards them. This is called per-request scoping, and it is the default behaviour defined in the JAX-RS specification.

At first glance this seems fine, but it creates a real problem when you need to store data. If I kept my room and sensor data as instance variables inside `RoomResource`, each request would start with a fresh, empty object and any data saved by a previous request would simply be gone. The API would forget everything on every call, which is obviously useless.

The solution I used was the Singleton pattern. I created a single class called `InMemoryDataStore` that holds one shared instance for the entire lifetime of the application. Every resource class — `RoomResource`, `SensorResource`, and `SensorReadingResource` — goes through `InMemoryDataStore.getInstance()` to read or write data, rather than storing anything locally.

But there is a second issue. When multiple requests arrive at the same millisecond, they are handled by different threads running in parallel. If two threads try to write to the same `HashMap` at the same moment, you can get corrupted data or lost updates — this is a classic race condition. To prevent this, I used `ConcurrentHashMap` instead of a plain `HashMap`. `ConcurrentHashMap` was specifically built for multi-threaded environments. It uses internal locking at the bucket level so that multiple threads can read and write safely without blocking each other unnecessarily, keeping the API both correct and fast under load.

---

**1.2:  Why HATEOAS Is a Hallmark of Advanced REST Design**

HATEOAS stands for Hypermedia as the Engine of Application State. The idea is simple: instead of the client needing to already know every URL in your API, the server embeds navigational links directly inside its responses. The client just starts at a known root endpoint and follows the links from there.

Think of it like a website. When you land on a homepage, you do not need to memorise every URL on the site. You just click the links provided. HATEOAS brings this same idea to APIs.

Compared to static documentation, this approach has some real advantages. First, the API becomes self-describing. A developer exploring the API for the first time can send a single GET to `/api/v1` and immediately discover that rooms live at `/api/v1/rooms` and sensors at `/api/v1/sensors`, without reading a single page of docs. Second, it makes clients more resilient to change. If the server team decides to restructure paths in a future version, clients that follow links dynamically will adapt automatically, while clients that hardcoded URLs will break. Third, it reduces tight coupling between the client and server, which is one of the core goals of the REST architectural style.

In my discovery endpoint, I embedded a `links` object in the response containing `self`, `rooms`, and `sensors` URIs. This is a basic but deliberate implementation of this principle.

---

### Part 2:  Room Management

**2.1:  Returning Only IDs vs Full Room Objects**

When designing a list endpoint like `GET /api/v1/rooms`, you have a choice: return just the IDs, or return the full objects. Both approaches have trade-offs worth thinking through carefully.

Returning only IDs keeps the response payload very small. This is great for bandwidth, especially when the collection is large. But the client then has to fire off a separate GET request for every single ID to get the actual data it needs. If there are 200 rooms, that is 200 extra HTTP round trips — a problem known as the N+1 request problem. Each of those round trips adds latency, consumes server resources, and makes the client logic more complicated.

Returning full objects solves the N+1 problem because the client gets everything it needs in one single call. The downside is that the payload is significantly larger, which matters on slow networks or mobile connections. You are also sending data that many clients may not even need, like the `sensorIds` list, which wastes both bandwidth and client-side memory.

In my implementation, I return full room objects in the list response. For a campus management system where an admin dashboard likely needs to display room names, capacities, and sensor counts all at once, this is the more practical choice. For very large datasets in a production system, you would typically add pagination to keep the response size manageable, but for this coursework scope, full objects provide the cleanest developer experience.

---

**2.2:  Is DELETE Idempotent in This Implementation?**

Yes, the DELETE operation is idempotent in my implementation, though it is worth being precise about what idempotent actually means here, because there is a common misconception.

Idempotency means that sending the same request multiple times produces the same server state as sending it just once. It does not mean the response code has to be identical every time.

Here is what happens in practice. Say a client sends `DELETE /api/v1/rooms/HALL-001`. If the room exists and has no sensors, it gets removed from the data store and the server responds with `204 No Content`. Now if the same client sends the exact same DELETE request again by mistake, the room is already gone. My implementation checks whether the room exists and throws a `NotFoundException`, which maps to a `404 Not Found` response.

The response code changed from `204` to `404`, but the state of the system is identical in both cases: `HALL-001` does not exist. That is the definition of idempotency — the outcome on the server is the same regardless of how many times you repeat the call. This is one of the properties that makes DELETE safe to retry in unreliable network conditions, because a client can resend a failed DELETE without worrying about accidentally deleting something twice.

There is also the safety constraint to consider. If the room has sensors, my implementation returns `409 Conflict` and leaves the room untouched. This is consistent — calling it multiple times always returns the same `409` until the sensors are removed.

---

### Part 3 : Sensor Operations & Linking

**3.1: Technical Consequences of Sending the Wrong Content-Type**

The `@Consumes(MediaType.APPLICATION_JSON)` annotation on a POST method tells the JAX-RS runtime to only match that method when the incoming request carries a `Content-Type: application/json` header. It is a declarative contract between the API and its clients.

If a client sends a request with `Content-Type: text/plain` or `Content-Type: application/xml`, Jersey compares that value against the `@Consumes` annotation before the method body is ever executed. Finding no match, it immediately rejects the request and returns `HTTP 415 Unsupported Media Type`. The developer does not need to write any validation code for this — the framework enforces it automatically.

This matters for a few reasons. It prevents invalid data from leaking into business logic. Without this check, a malformed plain-text body might cause a silent failure or a confusing `NullPointerException` deep inside the application instead of a clean, descriptive error at the boundary. It also makes the API self-documenting in a sense, because the `415` response tells the client developer exactly what went wrong and what they need to fix.

---

**3.2:  @QueryParam vs Path-Based Type Filtering**

Using `@QueryParam` for filtering — as in `GET /api/v1/sensors?type=CO2` — is generally the better design compared to embedding the filter in the path like `/api/v1/sensors/type/CO2`, and the reasons come down to semantics and flexibility.

A URL path is meant to identify a specific resource or collection. The path `/api/v1/sensors` already uniquely identifies the sensors collection. Appending `/type/CO2` to the path implies that `type/CO2` is a distinct sub-resource or a different addressable entity, which is architecturally misleading. It is not a resource — it is a filter being applied to a collection.

Query parameters, on the other hand, are designed exactly for this purpose. They are optional modifiers that adjust the response without changing the resource being addressed. This means `GET /api/v1/sensors` and `GET /api/v1/sensors?type=CO2` both refer to the sensors collection — one just returns a filtered view of it.

There is also a practical advantage. Query parameters compose naturally. A future requirement to filter by both type and status can be handled cleanly with `?type=CO2&status=ACTIVE`. Achieving the same thing with path parameters would require something ugly like `/sensors/type/CO2/status/ACTIVE`, which would also need its own dedicated resource method. Query parameters scale with requirements in a way that path-based filtering simply cannot.

---

### Part 4: Deep Nesting with Sub-Resources

**4.1: Architectural Benefits of the Sub-Resource Locator Pattern**

The Sub-Resource Locator pattern is a JAX-RS feature where a resource method returns an instance of another class rather than a response directly. JAX-RS then continues dispatching the request to that returned object. In my implementation, `SensorResource` has a method annotated with `@Path("/{sensorId}/readings")` that returns a `SensorReadingResource` instance, passing the sensor context along through the constructor.

The main benefit is separation of concerns. If I had defined every single reading endpoint directly inside `SensorResource`, that class would end up handling sensor creation, sensor retrieval, type filtering, reading history, reading creation, and the update side-effect on `currentValue`. That is six distinct responsibilities in one class. It becomes very difficult to read, maintain, or test.

By splitting this into two classes, `SensorResource` handles sensor-level concerns and `SensorReadingResource` handles reading-level concerns. Each class has a clear, singular purpose. This aligns with the Single Responsibility Principle, which is one of the foundational principles of good software design.

There is also a meaningful benefit to testability. Because `SensorReadingResource` is a plain Java class that accepts its context through its constructor, it can be instantiated directly in a unit test with a specific `sensorId` — no need to spin up an HTTP server or simulate a full request cycle. Testing the reading logic becomes simple and isolated.

In large APIs with dozens of nested resource hierarchies, this pattern is essentially what keeps the codebase navigable. Instead of one enormous controller file, you get a well-organised tree of focused classes that mirrors the resource hierarchy of the API itself.

---

### Part 5:  Error Handling, Exception Mapping & Logging

**5.2:  Why HTTP 422 Is More Semantically Accurate Than 404**

This distinction is subtle but important. HTTP `404 Not Found` means the URL the client requested does not exist on the server. It signals a routing problem — the client asked for something the server has no knowledge of at that address.

When a client POSTs a new sensor with `"roomId": "FAKE-999"`, the URL they sent the request to — `/api/v1/sensors` — is absolutely valid and exists. The server found the endpoint just fine. The request body is also syntactically valid JSON. The problem is purely semantic: the value `"FAKE-999"` inside that valid payload refers to a room that does not exist in the system.

Returning `404` in this situation would be actively misleading. The client would likely think they typed the wrong URL, when in reality their endpoint is correct but their data is wrong. `422 Unprocessable Entity` was defined specifically for this scenario. It communicates: "I understood your request, I found your endpoint, your JSON is syntactically correct — but the data inside it makes no logical sense given the current state of the system." That is a fundamentally different kind of error from a missing route, and using the right status code helps client developers debug their integrations much faster.

---

**5.4: Security Risks of Exposing Java Stack Traces**

Exposing raw Java stack traces to external API consumers is a significant security vulnerability, specifically classified as Information Disclosure.

A stack trace is essentially a detailed map of what happened inside your application at the moment of failure. To a developer it is useful for debugging. To an attacker, it is a reconnaissance tool. A single stack trace can reveal the internal package structure and class names of your application (e.g., `com.smartcampus.repository.InMemoryDataStore`), exposing the design and architecture. It shows the exact versions of third-party libraries being used (e.g., `jersey-server-2.40`), allowing attackers to cross-reference those versions against public databases of known CVEs and target specific vulnerabilities. It can reveal server file paths, which support path traversal attack planning. It exposes the exact sequence of method calls that led to the error, handing over business logic details that might reveal exploitable assumptions in the code.

The principle here is minimal information disclosure — external clients should be told only what they need to know to fix their mistake. In my implementation, the `GlobalExceptionMapper<Throwable>` catches any unhandled exception, logs the full stack trace safely on the server using `java.util.logging.Logger`, and returns only a generic `500 Internal Server Error` JSON response to the client. The attacker sees nothing useful. The developer monitoring the server sees everything they need.

---

**5.5:  Why JAX-RS Filters Are Better Than Manual Logging**

Placing `Logger.info()` calls inside every resource method is a valid approach, but it has a number of practical problems that filters solve completely.

The most obvious issue is coverage. Every time a new endpoint is added, the developer must remember to add logging manually. It is easy to forget, especially under time pressure. A filter registered with `@Provider` is automatically applied to every single request and response across the entire API, including endpoints that do not exist yet. You cannot forget to apply it.

Beyond that, putting logging inside resource methods mixes two fundamentally different concerns. A resource method should be focused entirely on its business logic — validating input, performing operations, constructing responses. Logging is a cross-cutting concern, meaning it cuts across all parts of the application regardless of what each part does. JAX-RS filters were designed specifically for this category of concern, alongside things like authentication, CORS headers, and request compression. Keeping them separate makes each resource method shorter, cleaner, and much easier to read.

Maintenance is another factor. If the log format needs to change — say, to add a request ID or a timestamp — with the filter approach you edit one file. With the manual approach, you edit every single resource method across the entire codebase, and you risk inconsistencies creeping in.

In my implementation, the `LoggingFilter` class implements both `ContainerRequestFilter` and `ContainerResponseFilter`. Every incoming request is logged with its HTTP method and URI, and every outgoing response is logged with its final status code — all from a single, maintainable class.
