# Ujorm PetStore

Ujorm PetStore is a practical showcase of a web application built on the combination of **Spring Boot 3.5** and **Ujorm 3**. The project serves as an inspiration for developing your own web application with an emphasis on straightforwardness and maximum type safety.
No external templates and no hidden "magic" behind the scenes. Just pure Java, full control over the generated SQL, and HTML rendered safely straight from the code.
The application utilizes two key modules from the Ujorm3 library, which distinguish themselves from common established standards by their approach:

* **ujo-orm module (database access):** Unlike traditional JPA frameworks, it avoids complex stateful abstractions and string-based queries (HQL/JPQL). Instead, it fully supports **immutable data entities** using modern Java `record`s and brings **type-safe SQL query building**. Thanks to an automatically generated metamodel, the compiler instantly catches typos in column names, minimizing runtime errors.
* **ujo-web module (UI creation):** It completely eliminates the need for classic templating engines (such as Thymeleaf or JSP). HTML is rendered directly from pure Java using an object tree (the `Element` class) and standard `try-with-resources` blocks. This eliminates context switching between the template and backend; the page structure is verified at compile-time, and UI refactoring is as simple as modifying any other Java class thanks to the IDE.

![Ujorm PetStore UI](documents/ujorm-petstore.png)

## Key Features of Ujorm in this Project
* **Pure Java HTML Rendering:** Instead of traditional templating engines (e.g., Thymeleaf, JSP, or FreeMarker), the UI is rendered by a type-safe HTML builder (`HtmlElement`). It generates code using Java blocks (`try-with-resources`) directly within the Servlet.
* **Immutable Database Entities as Java Records:** Modern Java `record`s (e.g., `Pet`, `Category`) are used as domain objects. This ensures absolute immutability and clean code while maintaining compatibility with classic JPA annotations (`@Table`, `@Column`).
* **Type-Safe SQL Queries:** During compilation, an annotation processor automatically generates entity metamodels (e.g., `MetaPet`, `MetaCategory`). These are then used in the DAO layer to safely and cleanly build SQL queries (`SqlQuery`), completely eliminating the risk of column name typos.
* **Safe HTTP Parameter Handling:** Web request processing uses the `HttpParameter` interface (implemented here via `enum`). This centralizes parameter definitions and protects the application from mapping errors or form-name typos.

---

## Tech Stack

* **Java:** 25
* **Framework:** Spring Boot 3.5.0 (Web, JDBC)
* **ORM and Web:** Ujorm 3.0.0-SNAPSHOT (ujo-orm, ujo-web)
* **Database:** H2 (In-memory database)
* **UI Styling:** Bootstrap 5.3.3 (CSS loaded dynamically via CDN)

## Project Structure
* `AppPetStore.java` – The main Spring Boot application class, which also contains a fully transactional application/service layer.
* `Dao.java` – The data access layer (Repository) integrating the standard Spring JDBC Connection with the Ujorm `EntityManager`.
* `Entities.java` – Database schema definitions using Java records.
* `PetServlet.java` – A stateless Servlet acting as both Controller and View. It handles all HTTP communication (GET, POST), implements the PRG (Post/Redirect/Get) pattern, and builds the final HTML.
* `Constants.java` – Keeps the application clean by grouping enums (`Status`) and CSS classes for UI components.

## How to Run the Project

1. Ensure you have **JDK 25** and **Maven** installed on your system.
2. Run the following command in the root directory of the project:
   ```bash
   mvn spring-boot:run
   ```
3. Once the application successfully starts, open your web browser at:
   [http://localhost:8080](http://localhost:8080)