
Ujorm PetStore is a practical showcase of a small, fully working web application built on
**Avaje Inject** and the **Ujorm 3** library. The project is intentionally minimal — it is
meant as a reference implementation that highlights how to develop a servlet-based web app
with an emphasis on **straightforwardness**, **maximum type safety** and **zero hidden
"magic"**. Just pure Java, full control over the generated SQL, compile-time dependency
injection and HTML rendered straight from code.

## Project Goals

- Provide a small, easy-to-read reference application for the Ujorm 3 ecosystem.
- Demonstrate a stateless servlet acting as both Controller and View (Post/Redirect/Get pattern).
- Keep the dependency tree minimal — only essential, lightweight libraries.
- Show that a real, database-backed Java web application can fit into a tiny WAR file (~850 KB).

## Architecture in Short

- **Runtime:** Java 25 + Jakarta Servlet API, deployed on Jetty (Tomcat-compatible).
- **DI container:** Avaje Inject — compile-time, no runtime reflection overhead.
- **Persistence:** Ujorm ORM with type-safe metamodels generated at compile time.
- **UI rendering:** Ujorm Web — pure Java HTML rendering using `try-with-resources` blocks.
- **Styling:** Bootstrap 5.3.3 served from a CDN.
- **Database:** H2 in-memory; the schema is initialized automatically on startup.

## Key Features

### 1. Type-Safe Persistence (`ujo-orm`)

- Immutable Java `record`s as domain objects: `Pet`, `Category`, `Customer`, `PetOrder`.
- Generated metamodels (`QPet`, `QCategory`, ...) catch typos at compile time.
- Full SQL transparency — no hidden N+1 problems and no `LazyInitializationException`.
- Native SQL can be written directly and mapped back to Java records without ceremony.

### 2. Pure-Java HTML (`ujo-web`)

- HTML is produced via the `HtmlElement` and `Element` builders — no template engine.
- The page structure is verified by the Java compiler — refactor-friendly UI code.
- Complex UI blocks are simply extracted into smaller methods such as `renderTable()`.

### 3. Safe Request Handling

- `HttpParameter` enums centralize parameter definitions and prevent typos in form names.
- The Post/Redirect/Get pattern protects against duplicate form submissions.
- A single `TransactionFilter` wraps every HTTP request in a database transaction.

## Servlets In This Application

- **PetServlet** — the main UI for browsing the catalog, buying, adding and editing pets.
- **InfoServlet** — this page; renders project documentation from a Markdown source.

## Source Code & References

- Source code on GitHub: [github.com/pponec/ujorm-petstore](https://github.com/pponec/ujorm-petstore)
- Ujorm library home page: [ujorm.org](https://ujorm.org/)
- Ujorm 3 on GitHub: [github.com/pponec/ujorm (branch ujorm3)](https://github.com/pponec/ujorm/tree/ujorm3)
- ORM benchmarks: [github.com/pponec/orm-benchmarks](https://github.com/pponec/orm-benchmarks)
- HTML-builder benchmarks: [github.com/pponec/html-benchmarks](https://github.com/pponec/html-benchmarks)

## Author

Pavel Ponec — [github.com/pponec](https://github.com/pponec)
