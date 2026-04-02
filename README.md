# Ujorm PetStore

Ujorm PetStore is a practical showcase of a web application built on **Spring Boot 3.5** and
[**Ujorm 3**](https://github.com/pponec/ujorm/tree/ujorm3?tab=readme-ov-file#-ujorm3-library).
The project serves as an inspiration for developing web applications with an emphasis on straightforwardness, maximum type safety, and zero hidden "magic".

Just pure Java, full control over generated SQL, and HTML rendered safely straight from the code.

![Ujorm PetStore UI](documents/ujorm-petstore.png)

---

## Key Features & Modules

The application demonstrates the power of two core Ujorm3 modules that streamline development by eliminating common abstractions:

### 1. Database Access (ujo-orm)
* **Immutable Records:** Uses modern Java `record`s as domain objects (`Pet`, `Category`), ensuring clean code and absolute immutability while maintaining compatibility with `@Table` and `@Column` annotations.
* **Type-Safe SQL Builder:** An annotation processor generates metamodels (e.g., `MetaPet`) at compile-time. This eliminates typos in column names and allows the compiler to catch errors before the app even runs.
* **SQL Transparency:** Unlike heavy JPA frameworks, there are no `LazyInitializationException` or hidden N+1 issues. You have full control over the `SqlQuery`.
* **The Mapping Advantage:** Ujorm bridges the gap between raw SQL and object mapping. You can write native SQL and easily map results to Java records using the `label()` method, keeping the SQL debuggable in any DB client.

### 2. UI Creation (ujo-web)
* **Pure Java HTML Rendering:** Replaces traditional engines like Thymeleaf or JSP. HTML is rendered directly from Java using the `HtmlElement` builder and `try-with-resources` blocks.
* **Refactoring Power:** Since the UI is just Java code, you get full IDE support. Complex UI blocks can be instantly refactored into smaller, reusable methods (e.g., `renderTable()`) without the overhead of fragment files or context passing.
* **Type Safety:** The page structure is verified at compile-time. No more runtime errors caused by a typo in a template variable.

### 3. Safe Request Handling
* **HttpParameter Interface:** Uses `enum` implementations to centralize web parameter definitions, protecting the application from mapping errors or form-name typos.

## Code Samples

The project is designed with an emphasis on straightforwardness.
The following example from a stateless servlet demonstrates how elegantly logic, parameters, and HTML generation can be connected:

```java
protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
    var ctx = HttpContext.ofServlet(req, resp);
    var contextPath = req.getContextPath();
    var action = ctx.parameter(ACTION, Action::paramValueOf, Action.UNKNOWN);
    var petId = ctx.parameter(PET_ID, Long::parseLong);
    var pets = services.getPets();
    var categories = services.getCategories();
    var petToEdit = switch(action) {
        case EDIT -> services.getPetById(petId);
        default -> Optional.<Pet>empty(); };

    try (var html = HtmlElement.of(ctx, BOOTSTRAP_CSS)) {
        try (var body = html.addBody(Css.container, Css.mt5)) {
            renderHeader(body, contextPath);
            renderTable(body, pets);
            renderForm(body, petToEdit, categories);
        }
    }
}
```

Here is what a native SQL query looks like in pure Java:

```java
public List<Pet> findAll() {
    var sql = """
            SELECT p.id AS ${p.id}
            , p.name    AS ${p.name}
            , p.status  AS ${p.status}
            , c.id      AS ${c.id}
            , c.name    AS ${c.name}
            FROM pet p
            LEFT JOIN category c ON c.id = p.category_id
            WHERE p.id >= :id
            ORDER BY p.id
            """;

    return SqlQuery.run(connection.get(), query -> query
            .sql(sql)
            .label("p.id", MetaPet.id)
            .label("p.name", MetaPet.name)
            .label("p.status", MetaPet.status)
            .label("c.id", MetaPet.category, MetaCategory.id)
            .label("c.name", MetaPet.category, MetaCategory.name)
            .bind("id", 1L)
            .streamMap(PET_EM.mapper())
            .toList());
}
```

---

## Tech Stack

* **Java:** 25
* **Framework:** Spring Boot 3.5.0 (Web, JDBC)
* **ORM and Web:** Ujorm 3.0.0-SNAPSHOT (`ujo-orm`, `ujo-web`)
* **Database:** H2 (In-memory)
* **UI Styling:** Bootstrap 5.3.3 (CDN)

## Project Structure

* [AppPetStore.java](src/main/java/org/ujorm/petstore/AppPetStore.java) – Main Spring Boot class and transactional service layer.
* [Dao.java](src/main/java/org/ujorm/petstore/Dao.java) – Data access layer integrating Spring JDBC with Ujorm `EntityManager`.
* [Entities.java](src/main/java/org/ujorm/petstore/Entities.java) – Database schema definitions using Java records.
* [PetServlet.java](src/main/java/org/ujorm/petstore/PetServlet.java) – A stateless Servlet acting as both Controller and View. It handles HTTP communication (PRG pattern) and builds the HTML.
* [Constants.java](src/main/java/org/ujorm/petstore/Constants.java) – Shared enums (`Status`) and CSS classes.

## How to Run the Project

1. Ensure you have **JDK 25** and **Maven** installed.
2. Run in the root directory:
   ```bash
   mvn spring-boot:run
   ```
3. Open your browser at: [http://localhost:8080](http://localhost:8080)

---

## Conclusion: Why this approach?

This "rebellious" architecture is ideal for developers seeking a simpler alternative to heavy JPA or complex SPA frontends.

* **Use Cases:** Perfect for microservices, B2B tools, internal apps, or HTMX-driven projects where productivity and maintainability are priorities.
* **The "Java-First" Philosophy:** By keeping everything (SQL mapping, UI structure, Logic) within the Java compiler's reach, you minimize context switching and maximize reliability.

**Alternative Comparison:**
* **ORM:** MyBatis, Jdbi, Spring Data JDBC.
* **Web:** j2html, Wicket, Vaadin.

---

## Benchmarks & Resources

For more technical details and performance metrics, please visit:
* [**Ujorm 3 Library**](https://github.com/pponec/ujorm/tree/ujorm3?tab=readme-ov-file#-ujorm3-library) – The official project page.
* [**ORM Benchmark**](https://github.com/pponec/orm-benchmarks?tab=readme-ov-file#orm-benchmark) – Compare the performance of Ujorm ORM with other frameworks.
* [**HTML Builder Benchmark**](https://github.com/pponec/html-benchmarks?tab=readme-ov-file#html-builder-benchmark) – See how the `ujo-web` module stands against other HTML rendering engines.