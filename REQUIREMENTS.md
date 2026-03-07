# ProductClientService Requirements and Overview

## 🔧 Prerequisites

Before building or running the service ensure the following tools are installed:

1. **Java Development Kit (JDK)**
   - Version 17 (matches `<java.version>` in `pom.xml`).
   - Set `JAVA_HOME` accordingly and add to `PATH`.

2. **Apache Maven**
   - Version 3.6+ (any recent release).
   - Used to compile, package and run tests (`mvn` / `mvnw` wrappers included).

3. **Database**
   - PostgreSQL (or any JDBC-compatible DB; `org.postgresql:postgresql` dependency).
   - Configure connection properties via Spring `application.properties` or environment variables.

4. **Supporting Services** (each optional depending on features used):
   - **Redis** – caching and session store (`spring-boot-starter-data-redis`).
   - **Elasticsearch** – search index (`co.elastic.clients:elasticsearch-java`).
   - **Kafka** – messaging (`spring-kafka`).
   - **Amazon S3** – file storage (AWS SDK for S3).
   - **Cloudinary** – image/media CDN.
   - **Cloudinary & AWS credentials** – injected via `.env` file or environment variables.
   - **SMTP / Email** – if notification features are used.

5. **Environment variables**
   - Project loads variables from the `.env` file located in `src/main/resources` by default.
   - Example keys: `DB_URL`, `DB_USER`, `DB_PASSWORD`, `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `CLOUDINARY_URL`, `KAFKA_BOOTSTRAP_SERVERS`, etc.

6. **Other**
   - `dotEnv` library is used to read `.env` into system properties at startup.
   - `Lombok` annotations require IDE support (install Lombok plugin).


## 📦 Building & Running

From the project root:

```sh
# compile & package
mvn clean package

# run using Maven
mvn spring-boot:run

# or run the JAR directly
java -jar target/ProductClientService-0.0.1-SNAPSHOT.jar
```

Configuration values can be supplied via `application.properties` or environment variables; the `.env` loader will copy them into `System` properties automatically.


## 🧠 What the Service Does

`ProductClientService` is a Spring Boot‑based backend that provides REST endpoints for
managing products, carts, coupons, banners, brands, sections, stock notifications,
wishlists, authentication, and other e‑commerce operations.

Key characteristics:

* **Controller layer** – defined under `com.ProductClientService.ProductClientService.Controller`.
  Separate packages exist for `admin`, `seller`, and user scopes.
* **Service & Repository layers** – use Spring Data JPA to persist models to PostgreSQL.
* **Asynchronous processing** – enabled with `@EnableAsync` and async beans.
* **Feign clients** – for calling other microservices (`@EnableFeignClients`).
* **Security** – JWT‑based authentication via `spring-boot-starter-security` and `jjwt`.
* **Cloud & external integrations**:
  * AWS S3 for object storage.
  * Cloudinary for media handling.
  * Elasticsearch for advanced search capabilities.
  * Kafka for event-driven messaging.
  * Redis for caching and session management.
* **Utilities & helpers** – common validators, constants, and interceptors live under `Utils`.
* **Configuration classes** – all third‑party clients and infrastructure beans are defined
  in `Configuration` package (e.g. `S3Config`, `KafkaConfig`, `RedisConfig`, etc.).

The service is designed to be a central API gateway for e‑commerce data operations, exposing
JSON endpoints consumed by front‑end applications or other microservices.


## ✅ Summary

1. Ensure JDK 17 and Maven are installed.
2. Configure your environment variables or `.env` file for database, AWS, Cloudinary,
   Kafka, Redis, etc.
3. Build with `mvn clean package` and run via Maven or by executing the JAR.
4. The application provides a RESTful API for product and user management with
   integrations to multiple external systems.

Refer to each configuration class in `src/main/java/com/ProductClientService/ProductClientService/Configuration`
for details on supported properties and defaults.
