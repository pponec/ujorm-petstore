# Ujorm PetStore

Ujorm PetStore is a practical showcase of a web application built on **Avaje Inject** and
[**Ujorm 3**](https://github.com/pponec/ujorm/tree/ujorm3?tab=readme-ov-file#-ujorm3-library).
The project serves as an inspiration for developing web applications with an emphasis on straightforwardness, maximum type safety, and zero hidden "magic".

Just pure Java, full control over generated SQL, compile-time dependency injection, and HTML rendered safely straight from the code.

![Ujorm PetStore UI](documents/ujorm-petstore.png)

---

## Dependencies & Footprint

The project is designed with an extreme focus on minimalism and zero bloat. The dependency tree consists only of essential, lightweight libraries:

* **Avaje Inject:** Compile-time dependency injection with no runtime reflection overhead.
* **HikariCP:** Lightning-fast database connection pooling.
* **Ujorm:** (`ujo-core`, `ujo-orm`, `ujo-web`) for database operations and HTML rendering.
* **Jakarta APIs:** (`jakarta.inject`, `jakarta.persistence`, `jakarta.servlet`) for standardized interfaces.

Because of this lean architecture, the application has an incredibly small memory and storage footprint. 
If you configure the H2 database (or any other database driver) as a `provided` dependency so that it is supplied by the application server (e.g., Tomcat or Jetty), the total size of the compiled `WAR` file shrinks to a mere **~850 KB** (869,458 bytes).

---

## Key Features & Modules

The application demonstrates the power of Ujorm3 modules combined with modern compile-time DI, streamlining development by eliminating common abstractions:

### 1. Database Access (ujo-orm)
* **Immutable Records:** Uses modern Java `record`s as domain objects (`Pet`, `Category`), ensuring clean code and absolute immutability while maintaining compatibility with `@Table` and `@Column` annotations.
* **Type-Safe SQL Builder:** An annotation processor generates metamodels (e.g., `QPet`) at compile-time. This eliminates typos in column names and allows the compiler to catch errors before the app even runs.
* **SQL Transparency:** Unlike heavy JPA frameworks, there are no `LazyInitializationException` or hidden N+1 issues. You have full control over the `SelectQuery`.
* **The Mapping Advantage:** Ujorm bridges the gap between raw SQL and object mapping. You can write native SQL and easily map results to Java records, keeping the SQL debuggable in any DB client.

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
    var contextPath = req.getContextPath();
    var ctx = HttpContext.ofServlet(req, resp);
    var action = ctx.parameter(ACTION, Action::paramValueOf, Action.UNKNOWN);
    var petId = ctx.parameter(PET_ID, Long::parseLong);
    var pets = services.getPets();
    var categories = services.getCategories();
    var petToEdit = switch(action) {
        case EDIT -> services.getPetById(petId);
        default -> Optional.<Pet>empty(); 
    };

    try (var html = HtmlElement.of(ctx, BOOTSTRAP_CSS)) {
        try (var body = html.addBody(Css.container, Css.mt5)) {
            renderHeader(body, contextPath);
            renderTable(body, pets);
            renderForm(body, petToEdit, categories);
        }
    }
}
```

Here is what a native SQL query looks like in pure Java using the generated metamodel:

```java
public List<Pet> findAll(long fromId) {
    return SelectQuery.run(connection.get(), PET_EM, query -> query
            .columnsOfDomain(true)
            .column(QPet.category, QCategory.name)
            .where(QPet.id.whereGe(fromId))
            .tail("ORDER BY", QPet.id)
            .toList());
}
```

---

## Tech Stack

* **Java:** 21
* **DI Framework:** Avaje Inject 10.4
* **ORM and Web:** Ujorm 3.0.0-RC5 (`ujo-orm`, `ujo-web`)
* **Database:** H2 (In-memory)
* **Server:** Jetty (via Maven Plugin) / Tomcat compatible
* **UI Styling:** Bootstrap 5.3.3 (CDN)

## Project Structure

* `AppPetStore.java` – Main application logic and transactional service layer.
* `DaoFactory.java` – Data access factory containing the `DaoFacade` and internal DAOs interacting with Ujorm `EntityManager`.
* `Entities.java` – Database schema definitions using Java records.
* `PetServlet.java` – A stateless Servlet acting as both Controller and View. It handles HTTP communication (PRG pattern) and builds the HTML.
* `Constants.java` – Shared enums (`Status`) and CSS classes.

## How to Run the Project

1. Ensure you have **JDK 21** and **Maven** installed.
2. Run in the root directory:
   ```bash
   mvn jetty:run
   ```
3. Open your browser at: [http://localhost:8080](http://localhost:8080)

---

## Conclusion: Why this approach?

This "rebellious" architecture is ideal for developers seeking a simpler alternative to heavy JPA, Reflection-based DI containers, or complex SPA frontends.

* **Use Cases:** Perfect for microservices, B2B tools, internal apps, or HTMX-driven projects where productivity, fast startup times, and maintainability are priorities.
* **The "Java-First" Philosophy:** By keeping everything (SQL mapping, Dependency Injection, UI structure, Logic) within the Java compiler's reach, you minimize context switching and maximize reliability.

**Alternative Comparison:**
* **ORM:** MyBatis, Jdbi, Spring Data JDBC.
* **Web:** j2html, Wicket, Vaadin.
* **DI:** Dagger 2, Micronaut Inject.

---

## Benchmarks & Resources

For more technical details and performance metrics, please refer to:

* [**Ujorm ORM Library**](https://github.com/pponec/ujorm/tree/ujorm3?tab=readme-ov-file#-ujorm3-library) – The official project page for the `ORM` module.
* [**Ujorm Element Library**](https://github.com/pponec/ujorm/tree/ujorm3?tab=readme-ov-file#-ujorm3-library) – The official project page for the `UI` module.
* [**Benchmark for Java ORM frameworks**](https://github.com/pponec/orm-benchmarks?tab=readme-ov-file#orm-benchmark) – Compare the performance of different `ORM` frameworks.
* [**Benchmark for Java WEB frameworks**](https://github.com/pponec/html-benchmarks?tab=readme-ov-file#html-builder-benchmark) – Compare the performance of different `HTML` rendering engines.