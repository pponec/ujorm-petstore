# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Build
./mvnw package

# Run the app (Jetty on localhost:8080)
./mvnw jetty:run

# Run all tests
./mvnw test

# Run a single test class
./mvnw test -Dtest=ServicesDatabaseTest

# Run a single test method
./mvnw test -Dtest=ServicesDatabaseTest#shouldLoadInitialPets
```

## Architecture

This is a Java 25 WAR application using **Avaje Inject** (compile-time DI) and **Ujorm 3** (ORM + HTML rendering). No Spring, no Hibernate, no Thymeleaf.

### Startup sequence

`Bootstrap` (`@WebListener`) is the entry point. On context init it:
1. Builds the Avaje `BeanScope` (all singletons are wired at this point)
2. Calls `DatabaseInitializer.createTables()` to create schema + seed data
3. Programmatically registers `TransactionFilter` (the filter uses constructor injection, so it cannot use `@WebFilter`)

Servlets are registered via `@WebServlet` and pull their dependencies from `Bootstrap.getBeanScope()` in their `init()` method (see `AbstractServlet`).

### Transaction model

Every HTTP request runs inside a single JDBC transaction:

```
HTTP request → TransactionFilter → TransactionManager.run()
                                      └─ opens Connection, stores in ThreadLocal
                                      └─ calls chain.doFilter(...)
                                      └─ commits or rolls back
```

`JdbcProvider` exposes a `Supplier<Connection>` bean that delegates to `TransactionManager::getConnection`. `Services` receives this supplier via constructor injection and calls `connection.get()` on every DB operation — no explicit transaction code in service methods.

To switch to explicit transactions instead, inject `TransactionManager` directly and wrap calls in `tm.run(() -> { ... })`.

### ORM — Ujorm 3

Domain entities are Java `record`s in `Entities.java` annotated with JPA-style `@Table` / `@Column` / `@Id`. The `ujorm-meta-processor` annotation processor generates metamodel classes at compile time into the `org.ujorm.petstore.meta` package with a `Q` prefix (e.g. `QPet`, `QCategory`). These are used for type-safe column references in `SelectQuery`.

```java
SelectQuery.run(connection(), PET_EM, query -> query
        .columns(true)
        .column(QPet.category, QCategory.name)   // join projection
        .tail("ORDER BY", QPet.id)
        .toList());
```

`EntityManager` instances are static singletons obtained once via `EntityContext.ofDefault()`.

### HTML rendering — ujo-web

Servlets generate HTML entirely in Java using `HtmlElement` with try-with-resources blocks. There are no template files. `AbstractServlet` wraps raw `HttpServletRequest`/`HttpServletResponse` into `HttpContext` before dispatching to the typed `doGet(HttpContext)` / `doPost(HttpContext)` overloads.

HTTP parameters are declared as `enum` implementing `HttpParameter` (see `PetServlet.Attrib` and `PetServlet.Action`), which prevents typos and centralises parameter names. `toString()` returns the lowercase wire name.

PRG (Post/Redirect/Get) is applied in every `doPost` handler to prevent duplicate submissions.

Shared page chrome (header, nav, logo) lives in `Layout`; individual servlets call `Layout.renderHeader(...)` and render their own content.

### Key files

| File | Role |
|---|---|
| `Bootstrap.java` | App lifecycle, DI container init, filter registration |
| `JdbcProvider.java` | Avaje `@Factory` — `DataSource` and `Supplier<Connection>` |
| `TransactionManager.java` | ThreadLocal connection, commit/rollback |
| `TransactionFilter.java` | Wraps each request in a transaction |
| `DatabaseInitializer.java` | DDL schema + seed data (idempotent) |
| `Entities.java` | All domain records (`Pet`, `Category`, `Customer`, `PetOrder`) |
| `Services.java` | Business logic + all ORM queries |
| `AbstractServlet.java` | Base class bridging raw servlet API → `HttpContext` |
| `PetServlet.java` | Controller + view for the pet catalog |
| `Layout.java` | Shared header/nav helpers |
| `Constants.java` | `Status` enum, `Css.*`, `Msg.*`, `Url.*` |

### Testing

Tests in `AbstractDatabaseTest` connect directly to an H2 in-memory DB (`testdb`). `@BeforeEach` runs `DROP ALL OBJECTS` then re-creates the schema via `initSchema(connection)`, giving each test a clean slate. `Services` is instantiated directly with `this::connection` — no DI container is involved in tests.

---

## Ujorm ORM API — class and method overview

Choose the class by priority (left = most type-safe, right = most general):

```
EntityManager.crud()  →  SelectQuery  →  SqlQuery
```

### 1. `EntityManager<D,V>` + `Crud<D,V>` — type-safe CRUD operations

Static instances are created once via `EntityContext.ofDefault()`:

```java
private static final EntityContext CTX = EntityContext.ofDefault();
private static final EntityManager<Pet, Long> PET_EM = CTX.entityManager(Pet.class);
```

`crud(connection)` returns a `Crud<D,V>` for a specific connection:

```java
// INSERT — returns the inserted entity (with generated ID)
Pet saved = PET_EM.crud(conn).insert(pet);

// INSERT OR UPDATE depending on whether PK is set
PET_EM.crud(conn).insertOrUpdate(pet);

// SELECT by PK
Optional<Pet> p = PET_EM.crud(conn).findById(id);
Pet pOrNull      = PET_EM.crud(conn).findByIdNullable(id);

// UPDATE (optional column filter — without filter all columns are updated)
PET_EM.crud(conn).update(pet);
PET_EM.crud(conn).update(pet, QPet.name, QPet.status); // selected columns only

// UPDATE — only changed columns (entity must implement SnapshotProvider)
PET_EM.crud(conn).updateChanged(modifiedPet);

// DELETE
PET_EM.crud(conn).deleteById(id);
PET_EM.crud(conn).delete(pet);

// Bulk operations via Stream
PET_EM.crud(conn).insert(stream, pet -> {});   // bulk INSERT
PET_EM.crud(conn).delete(stream);              // bulk DELETE

// Escape hatch → SqlQuery for a custom WHERE, returns any type R
List<Pet> r = PET_EM.crud(conn).selectWhere(
        "WHERE status = :s", q -> q.bind("s", "AVAILABLE").toStream(PET_EM.mapper()).toList());
```

`EntityManager.mapper()` returns `SqlFunction<ResultSet, D>` for manual result set mapping.

---

### 2. `SelectQuery<D>` — SELECT with joins, type-safe via metamodel

Preferred approach for SELECT with projection across multiple tables. Uses keys generated by the annotation processor (`QPet`, `QCategory`…).

```java
// Static factory — opens the query, passes it to the builder, closes it automatically
List<Pet> pets = SelectQuery.run(conn, PET_EM, q -> q
        .columns(true)                              // all columns of domain D
        .column(QPet.category, QCategory.name)      // join projection (2-level path)
        // .column(QPet.a, QA.b, QB.c)             // 3-level path
        .where(QPet.status.whereEq(Status.AVAILABLE))
        .whereAnd(QPet.name.whereGe("A"), QPet.id.whereLe(100L)) // AND conditions
        .tail("ORDER BY", QPet.id)                  // raw SQL suffix (ORDER BY, LIMIT…)
        .toList());                                 // → List<D>

// Other terminal operations:
// .toStream()      → Stream<D>   (must be closed in try-with-resources or consumed immediately)
// .findFirst()     → Optional<D>
// .findUnique()    → Optional<D> (throws if more than one row is returned)

// Direct construction without run() — must be closed manually:
try (var q = new SelectQuery<>(conn, PET_EM)) {
    q.columns(true).where(...);
    return q.toList();
}
```

**`columns(true)` vs `.column(...)`** — `columns(true)` selects all columns of entity D; `.column(key)` adds a column from a joined table (required to populate nested records such as `pet.category().name()`).

---

### 3. `SqlQuery` — raw SQL, full control, no restrictions

Used for DDL, DML with named parameters, and SELECT with manual mapping. No compile-time column name checks.

```java
// Constructor — use with try-with-resources (AutoCloseable)
try (var q = new SqlQuery(conn)) {

    // DDL / DML without parameters
    q.sql("CREATE TABLE foo (id BIGINT PRIMARY KEY, name VARCHAR(100))").execute();

    // DML with named parameters (:name)
    q.sql("INSERT INTO category (name) VALUES (:name)");
    long id = q.bind("name", "Dogs")
               .executeInsert()
               .getGeneratedLastKey(rs -> rs.getLong(1));  // → last generated key

    // Repeated INSERT with the same template — bind + executeInsert can be chained
    q.sql("INSERT INTO pet (name, status, category_id) VALUES (:n, :s, :c)");
    q.bind("n","Rex").bind("s","AVAILABLE").bind("c", dogsId).executeInsert()
     .getGeneratedLastKey(rs -> rs.getLong(1));

    // Multi-row INSERT with multiple :params in one SQL
    q.sql("INSERT INTO pet (name, status, category_id) VALUES ('A','AVAILABLE',:a),('B','SOLD',:b)")
     .bind("a", dogsId).bind("b", catsId).execute();

    // SELECT — toStream maps each row via a lambda
    boolean exists = q.sql("SELECT 1 FROM information_schema.tables WHERE table_name = 'pet'")
                      .toStream(rs -> rs)          // identity → Stream<ResultSet> (singleton stream)
                      .findFirst()
                      .isPresent();

    // SELECT mapped to a custom type
    List<String> names = q.sql("SELECT name FROM category ORDER BY id")
                          .toStream(rs -> rs.getString(1))
                          .toList();

    // forEach — consuming variant without Stream (throws checked SQLException)
    q.sql("SELECT id, name FROM pet").forEach(rs -> {
        long petId  = rs.getLong(1);
        String name = rs.getString(2);
    });
}

// Static factory (alternative to try-with-resources)
String result = SqlQuery.run(conn, q -> q.sql("SELECT MAX(id) FROM pet")
        .toStream(rs -> rs.getLong(1)).findFirst().orElse(0L).toString());
```

**`bind(String, T...)` — supported types** — Boolean, Byte, Short, Integer, Long, BigDecimal, String, LocalDate, LocalDateTime. For other types use: `bindObject(String, JDBCType, Object...)`.

**`getGeneratedLastKey` vs `getGeneratedKeys`** — `getGeneratedLastKey` returns the first (and only) key or throws `NoSuchElementException`; `getGeneratedKeys` returns `Stream<R>` for a batch INSERT.

---

### When to use which class

| Situation | Class |
|---|---|
| Simple CRUD (insert, update, delete, findById) | `EntityManager.crud()` |
| SELECT across multiple tables with join projection | `SelectQuery` |
| Custom WHERE conditions using metamodel keys | `SelectQuery` with `.where(Criterion)` |
| DDL, schema creation, data seeding | `SqlQuery` |
| Complex SQL with no matching higher-level API | `SqlQuery` |
| Escape hatch from inside `Crud` | `Crud.selectWhere(sql, q -> ...)` |

---

## ujo-web API — `HtmlElement` and `Element`

### Class hierarchy

```
HtmlElement (AutoCloseable)  ← root of the whole page; closing it flushes the response
  └─ Element (AutoCloseable) ← any HTML tag; closing it closes the tag
       └─ Element ...
```

`HtmlElement` extends `AbstractHtmlElement`; both implement `Html` (tag and attribute constants). `Element` implements `Html` and extends `XmlBuilder<Element>`.

---

### `HtmlElement` — factory methods and page management

```java
// Most common: title from config, ctx = HttpContext, varargs CSS links from CDN
try (var html = HtmlElement.of(ctx, Layout.BOOTSTRAP_CSS)) { ... }

// With an explicit page title
try (var html = HtmlElement.of("Page title", ctx, Layout.BOOTSTRAP_CSS)) { ... }

// Pretty-printed output (indented HTML) — useful for debugging
try (var html = HtmlElement.niceOf(ctx, Layout.BOOTSTRAP_CSS)) { ... }

// Add inline CSS to <head> (must be called BEFORE addBody)
html.addCssBody("""
    body { background: #f8fafc; }
    """);
html.addCssBodies(sharedCss, extraCss);   // multiple blocks at once

// Add a CSS link to <head>
html.addCssLink("https://cdn.example.com/app.css");

// Add inline JavaScript
html.addJavascriptBody("console.log('hello');");

// Add a JS link (defer = true/false)
html.addJavascriptLink(true, "/app.js");

// Direct access to <head> and <body> without try-with-resources (no scoped close needed)
var head = html.getHead();
var body = html.getBody();

// Open <body> with CSS classes → returns an Element for page content
try (var body = html.addBody(Css.container, Css.mt5)) {
    // ... render page content
}
```

Closing `HtmlElement` finalises the page and sends the response. **Never omit the close** — always use try-with-resources.

---

### `Element` — adding children

Every `addXxx()` method returns a **new child** `Element`. Content methods (`addText`, `addRawText`) return **the same** element for chaining.

#### Structural elements

```java
// Generic element (tag + CSS classes)
try (var div = parent.addElement("article", Css.article)) { ... }
try (var div = parent.addDiv(Css.row, Css.g3)) { ... }
try (var span = parent.addSpan(Css.badge, Css.bgSuccess)) { ... }
try (var p = parent.addParagraph(Css.mb3)) { ... }
var li = parent.addListItem(Css.navItem);

// Conditional element — renders nothing if condition == false
try (var el = parent.addElementIf(condition, "div", Css.colMd4)) { ... }

// Headings
parent.addHeading(1, "Title", Css.textPrimary, Css.mb0);  // level + text + CSS
parent.addHeading("Title", Css.h2css);                    // without level number
```

#### Table

```java
try (var table = body.addTable(Css.table, Css.tableHover)) {
    try (var thead = table.addTableHead(Css.tableDark)) {
        try (var tr = thead.addTableRow()) {
            tr.addTableDetail().addText("Column A");
            tr.addTableDetail().addText("Column B");
        }
    }
    try (var tbody = table.addTableBody()) {
        try (var tr = tbody.addTableRow()) {
            tr.addTableDetail().addText(value);
        }
    }
}

// Shorthand — table from a 2D array (no try-with-resources)
body.addTable(new Object[][] {{"A", 1}, {"B", 2}}, Css.table);

// Table from a Stream with column extractors
body.addTable(stream, new String[]{"Name", "Age"}, headers,
              Pet::name, Pet::age);
```

#### Form and inputs

```java
try (var form = body.addForm(Css.dInline)
        .setMethod(Html.V_POST)
        .setAction("?" + ACTION + "=" + Action.SAVE)) {

    form.addHiddenInput(PET_ID, pet.id());

    // Text input with placeholder
    form.addTextInput(Css.formControl)
        .setNameValue(NAME, petName)
        .setAttr("placeholder", "Name");

    // Shorthand with HttpParameter — sets name, value, placeholder and CSS in one call
    form.addTextInp(NAME, petName, "placeholder text", Css.formControl);

    // Select element
    var select = form.addSelect(Css.formSelect).setName(STATUS);
    select.addSelectOptions(selectedValue, optionsMap);   // Map<T,String>

    // Or build options manually:
    var opt = select.addElement("option").setAttr("value", cat.id());
    opt.addText(cat.name());
    if (isSelected) opt.setAttr("selected", "selected");

    // Submit button
    form.addSubmitButton(Css.btn, Css.btnPrimary)
        .setNameValue(ACTION, Action.SAVE)
        .addText("Save");

    // Conditionally disabled button — passing null as the attribute name skips the attribute
    form.addSubmitButton(Css.btn, Css.btnSuccess)
        .setAttr(available ? null : "disabled", "disabled")
        .addText("Buy");
}
```

#### Anchors, images, miscellaneous

```java
parent.addAnchor("/pets", Css.navLink, Css.active)
      .addText("Home");

parent.addAnchor("/")
      .addImage("/images/logo.png", "Logo")
      .setAttr("width", 150)
      .setAttr("height", 150);

// Separate href setter (same result)
parent.addAnchor(url).setHref(url2).addText("link");

// Paragraph with mixed text and anchor
parent.addParagraph()
      .addText("See ")
      .addAnchor("https://ujorm.org").addText("ujorm.org");

// Preformatted text
parent.addPreformatted().addText(sourceCode);

// Raw HTML (no escaping)
parent.addRawText("<strong>bold</strong>");

// Markdown → HTML (via the library converter)
markdownToHtmlConverter.render(article, markdownString);
```

#### Setting attributes

```java
element.setAttr("data-id", pet.id())   // any arbitrary attribute
       .setAttr("aria-hidden", "true");

element.setAttribute("disabled");       // boolean attribute (no value)

element.setId("myId")
       .setClass(Css.formControl)
       .setName("fieldName")
       .setValue(currentValue)
       .setNameValue("fieldName", currentValue)  // setName + setValue in one call
       .setMethod(Html.V_POST)
       .setAction("/url")
       .setHref("/url")
       .setFor("inputId")
       .setTitle("tooltip text")
       .setRows(5)
       .setCols(80)
       .setColSpan(3)
       .setRowSpan(2)
       .setChecked("checked")
       .setCheckBoxValue(true);
```

---

### `HttpContext` — reading request parameters

```java
// Created automatically in AbstractServlet
HttpContext ctx = HttpContext.of(req, resp);

// Read parameter as String
String raw = ctx.parameter("name");
String withDefault = ctx.parameter("name", "defaultValue");

// Read with conversion + fallback on parse failure
Long petId = ctx.parameter(PET_ID, Long::parseLong);       // null on failure
Action action = ctx.parameter(ACTION, Action::paramValueOf, Action.UNKNOWN);
Status status = ctx.parameter(STATUS, s -> Status.valueOf(s.toUpperCase()));

// Context path ending with a slash (for building URLs)
String base = ctx.getPathSlash();   // "/" or "/app/"

// Redirect (PRG pattern)
ctx.sendRedirect(base + "?action=edit&id=" + petId);
```

---

### `HttpParameter` — type-safe request parameters as enums

```java
enum Attrib implements HttpParameter {
    ACTION, PET_ID, NAME, STATUS, CATEGORY_ID;

    @Override
    public String toString() {
        return name().toLowerCase().replace('_', '-');  // wire name: "pet-id"
    }
}

// Alternative reading directly on the parameter instance
String val = ACTION.of(ctx, "default");
long id    = PET_ID.of(ctx, 0L);          // primitive types with a default value

// Look up an enum value from a string (with fallback)
Action a = HttpParameter.paramValueOf(Action.class, "buy", Action.UNKNOWN);
```

`HttpParameter` implements `CharSequence`, so it can be passed anywhere a parameter name `String`/`CharSequence` is expected.
