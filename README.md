# Ujorm PetStore

A demonstration of the **Ujorm3** framework integrated with **Spring Boot 3.5+** and **Java 25**. This project showcases a lightweight, type-safe approach to database persistence and web presentation without the need for heavy ORM configurations or complex template engines.

## Key Features

- **Java 25 Records**: Using modern immutable data structures for domain entities.
- **Type-Safe Metamodel**: Automatic generation of `Meta` classes for compile-time safe database queries.
- **Ujorm3 Web Builder**: Programmatic HTML generation using a streaming fluent API (no JSP/Thymeleaf needed).
- **Spring Integration**: Full support for `@Transactional` services and Spring-managed DataSources.
- **Zero-Config Database**: Uses an in-memory H2 database that initializes on startup.

## Project Structure

- `AppPetStore`: The entry point of the application, containing the main method and the internal `@Service` layer.
- `Entities`: A wrapper class containing all domain `record` entities (Pet, Category, Customer, PetOrder).
- `Dao`: A centralized data access layer using Ujorm's `EntityManager` and `SqlQuery` for high-performance data fetching.
- `PetServlet`: A standard Jakarta Servlet providing the web interface via Ujorm's `Element` builder.

## Prerequisites

- **Java 25** or higher.
- **Maven 3.9+** (or use the provided Maven Wrapper).

## How to Run

The easiest way to start the application is using the provided shell script:

1. **Make the script executable**:
   ```bash
   chmod +x run.sh
   ```

2. **Run the script**:
   ```bash
   ./run-ujorm-petstore.sh
   ```

Alternatively, you can use the Maven Wrapper directly:
```bash
./mvnw clean spring-boot:run
```

## Accessing the Application

Once the application starts, open your web browser and navigate to:

**[http://localhost:8080/](http://localhost:8080/)**

## Database Configuration

The application is configured to use an **H2 In-Memory database**. All tables and sample data are created automatically during the first database access using the Ujorm ORM initialization features.

## License

This project is licensed under the Apache License, Version 2.0.