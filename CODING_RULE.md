# FloodWatch – Coding Rules

These rules apply to every change, whether a person or an agent writes it. CI enforces the Java style rules (`python3 scripts/check-style.py`) and fails a PR on any violation.

## 1. Java style (mandatory)

### 1.1 Naming
- **PascalCase** for classes, interfaces, enums, records and file names: `CartService.java`, `NotFoundException`.
- **camelCase** for methods, variables and parameters: `addToCart`, `productId`, `newTotal`.
- **UPPER_SNAKE_CASE** for constants, meaning every `static final` field, loggers included: `MAX_FILE_BYTES`, `LOG`.

### 1.2 Parameter layout
When a method, constructor or record takes parameters:
- the name goes on its own line;
- `(` goes on the next line;
- the parameters are indented one level and grouped by type: consecutive parameters of the same type share one line, and a new line starts whenever the type changes;
- the block ends with `) {` (or `);` for an interface method).

```java
public LinkedList<CartItem> addToCart
(
    User user,
    int productId,
    String quantity
) {
    ...
}
```

A method with no parameters stays on one line: `getName() {`.

### 1.3 Braces
`if`, `else`, `for`, `while`, `try`, `catch` and `finally` use **Allman style**: the `{` goes on its own line, and `else`, `catch` and `finally` start on their own line after `}`.

```java
if (existing == null)
{
    user.getCart().addLast(new CartItem(productId, amount));
}
else
{
    existing.setQuantity(newTotal);
}
```

Class and method declarations keep `{` on the same line: `public class X {`, `) {`.

### 1.4 Blank lines
- 2 blank lines after the file header.
- 2 blank lines before each class Javadoc, and 2 after the class opening line.
- 2 blank lines after **every** Javadoc block.
- 1 blank line before a Javadoc that directly follows code.
- The file ends right after its closing `}`, with no trailing blank lines and no trailing newline.

### 1.5 Comments
- The only comments allowed are the file header and Javadoc. **No `//` comments and no `/* */` blocks inside code.** If a comment seems necessary, restructure the code instead. For example, add a check rather than swallowing an exception in an empty `catch`.
- Every file starts with this header:

```java
/*
 * File:    CartService.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: Add, update and remove items in a user's cart.
 */
```

- Every class, constructor and method has a Javadoc with `@param` for each parameter, plus `@return` and `@throws` where relevant. Every class also has `@author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan`.

### 1.6 Whitespace
- No trailing whitespace on any line.
- Indent with 4 spaces, never tabs.

## 2. Engineering rules

1. **Layering:** `controller → service → integration / repository`. Controllers hold no business logic, and the frontend never talks to Open-Meteo or Supabase directly.
2. **Errors:** throw the `ApiException` subclasses in `com.floodwatch.service` (`BadRequestException`, `UnauthorizedException`, `ForbiddenException`, `NotFoundException`, `ConflictException`, `BusinessRuleException`, `TooManyRequestsException`, `ServiceUnavailableException`). Never build error JSON by hand. Unexpected errors must never leak internal messages to the caller.
3. **Time:** inject `java.time.Clock` and never call `Instant.now()` or `LocalDate.now()` directly. Pakistan dates use `Asia/Karachi`.
4. **Security:** protect endpoints with `@RequiresRole`, and get the user with `CurrentUser.of(request)`. Never return password hashes, or volunteer phone numbers to non-coordinators. Never log passwords, tokens or keys.
5. **Data:** go through `JsonFileRepository<T>`. A write that touches two files must undo the first write if the second fails. Validate all input with bean validation, and check coordinates against the Pakistan bounding box.
6. **Never guess:** sample or unavailable data must never produce a risk level or an alert. Every forecast shows its source, unit, update time and status.
7. **Edge cases:** handle and test boundaries (exact limits ±1), empty and missing input, wrong roles, concurrency where shared state changes, and outages of external services.

## 3. Testing rules

1. Every new behaviour has JUnit tests, and every edge case listed in the task has its own test.
2. Every `@SpringBootTest` uses `@ActiveProfiles("test")`.
3. Tests never call the network. Use fakes, `@MockitoBean` or `MockRestServiceServer`.
4. Tests that write data use `InMemoryJsonStorage` or a `@TempDir`, never `src/test/resources/test-data`.
5. Use `MutableClock` (`api/src/test/java/com/floodwatch/support`) for anything time-based.
6. Before opening a PR: `python3 scripts/check-style.py` prints `0 violations` and `cd api && ./mvnw test` passes.

## 4. Git rules

See `AGENT.md` for the rules that also bind agents. In short:
- branch from the latest `main` with a type prefix (`feat/`, `fix/`, `docs/`, `test/`, `refactor/`, `chore/`);
- one focused PR per task;
- small commits with clear messages;
- never commit `.env` or any secret.
