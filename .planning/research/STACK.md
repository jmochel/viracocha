# Stack Research

**Domain:** Java CLI workspace management tool — v3 unified sources/destinations with remote HTTP source support
**Researched:** 2026-05-08
**Confidence:** HIGH (all critical decisions verified against JDK 21 API docs and local Maven cache)

---

## Baseline Stack (unchanged from v2)

| Component | Version | Source |
|-----------|---------|--------|
| JDK | 21.0.2 (GraalVM CE) | Runtime |
| Micronaut BOM | 4.10.10 | pom.xml `micronaut-parent` |
| picocli | 4.7.7 | BOM-managed via `micronaut-picocli` |
| Freemarker | 2.3.34 | Explicit in pom.xml `<freemarker.version>` |
| jackson-dataformat-yaml | BOM-managed (~2.18.x) | via Micronaut BOM |
| SnakeYAML | 2.4 | BOM-managed, runtime scope |
| Logback Classic | BOM-managed | runtime scope |
| logstash-logback-encoder | 7.4 | Explicit in pom.xml |
| Lombok | BOM-managed | provided/annotation processor scope |

**No version upgrades required for v3 features.**

---

## New Capability: Remote HTTP(S) Source Fetching

### Decision: Use JDK 21 `java.net.http.HttpClient` — no new dependency

The `java.net.http.HttpClient` API (JEP 321, standard since JDK 11, mature in JDK 21) covers all v3 requirements for `RemoteFetcher`:

- Synchronous `send()` with `BodyHandlers.ofByteArray()` — fetches file bytes for copy/generate
- `BodyHandlers.ofString()` — fetches text for Freemarker template expansion
- `AutoCloseable` — clean resource management in `try-with-resources`
- Configurable `connectTimeout(Duration)` — prevents hanging on unreachable URLs
- No external dependency — zero pom.xml change required

**Why not Micronaut's `micronaut-http-client-jdk`:** That module (`io.micronaut:micronaut-http-client-jdk`, pinned to Micronaut core version by the BOM) wraps JDK HttpClient for reactive and annotation-driven HTTP clients. It adds Micronaut-managed beans, reactive publishers, and framework integration overhead that `RemoteFetcher` does not need. `RemoteFetcher` is a utility class making fire-and-forget GET calls to raw file URLs; raw JDK HttpClient is simpler, has no framework bootstrap cost, and is equally correct.

**Why not OkHttp or Apache HttpClient:** Additional transitive dependencies on a tool that already avoids dependency sprawl. OkHttp 4.x requires Kotlin stdlib. Apache HttpComponents 5.x is well-maintained but adds ~800KB of JARs for functionality JDK 21 already provides. The v3 use case is trivially simple (GET → bytes), which JDK HttpClient handles with a two-method call.

**Implementation pattern for `RemoteFetcher`:**

```java
@Singleton
public class RemoteFetcher {
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);

    public byte[] fetchBytes(String url) throws IOException {
        try (HttpClient client = HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .build()) {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();
            HttpResponse<byte[]> response = client.send(request, BodyHandlers.ofByteArray());
            if (response.statusCode() != 200) {
                throw new RemoteFetchException("HTTP " + response.statusCode() + " from: " + url);
            }
            return response.body();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted fetching: " + url, e);
        }
    }
}
```

**Confidence: HIGH** — verified against JDK 21 API docs (docs.oracle.com/en/java/javase/21/docs/api/java.net.http/java/net/http/HttpClient.html). `HttpClient` implements `AutoCloseable` as of JDK 21.

---

## New Capability: Glob Matching for Mapping Filters

### Decision: JDK `java.nio.file.FileSystems.getDefault().getPathMatcher()` — no new dependency

The JDK `PathMatcher` via `FileSystem.getPathMatcher(String syntaxAndPattern)` supports two syntaxes:

- `glob:pattern` — standard glob (*, ?, [], {})
- `regex:pattern` — full Java regex

Both work on `Path` objects. For the v3 `GlobMatcher` utility, `PathMatcher` is sufficient with one critical caveat documented below.

**Critical: Glob `+` is NOT a quantifier.** In JDK glob syntax, `+` is a literal character, not a "one or more" quantifier (that's regex). The architecture doc shows example patterns like `[A-Za-z]+*.mdc` and `3[0-9]+*.mdc` — these will NOT match as intended under glob syntax. Verified by direct test:

```
glob:[A-Za-z]+*.mdc  matches 'A+general.mdc'  (+ is literal)
glob:[A-Za-z]+*.mdc  does NOT match 'general-rules.mdc'
```

The `GlobMatcher` wrapper must either:
1. Accept patterns with an explicit `glob:` or `regex:` prefix and pass through to `PathMatcher` directly, OR
2. Default to `glob:` prefix with a documented note that `+` is literal, not a quantifier

**Recommendation:** Accept both `glob:` and `regex:` prefixes in the `MappingEntry.glob` field. Default to `glob:` when no prefix is present. Document the difference in CLI help text.

**Why not Apache Commons IO `FilenameUtils.wildcardMatchOnSystem`:** Commons IO uses a different wildcard syntax (only `*` and `?`) without bracket expressions. JDK `PathMatcher` is strictly more capable. Adding Commons IO as a dependency for glob matching alone is not justified.

**Why not `java.util.regex` directly:** `PathMatcher` with `regex:` prefix already delegates to Java regex. Wrapping `PathMatcher` in `GlobMatcher` provides a consistent API surface for tests and avoids dual code paths.

**Implementation pattern for `GlobMatcher`:**

```java
public class GlobMatcher {
    private final PathMatcher delegate;
    private final String originalPattern;

    public GlobMatcher(String pattern) {
        this.originalPattern = pattern;
        // If pattern already has glob: or regex: prefix, use as-is; else add glob:
        String prefixed = (pattern.startsWith("glob:") || pattern.startsWith("regex:"))
            ? pattern
            : "glob:" + pattern;
        this.delegate = FileSystems.getDefault().getPathMatcher(prefixed);
    }

    public boolean matches(Path path) {
        // Match against filename only, not full path, for single-directory glob context
        return delegate.matches(path.getFileName());
    }
}
```

**Confidence: HIGH** — verified by direct JDK 21 test execution. PathMatcher behavior confirmed for all pattern types used in architecture examples.

---

## Testing: HTTP Integration Tests

### Decision: `com.sun.net.httpserver.HttpServer` from `jdk.httpserver` module — no new test dependency

For integration tests of `RemoteFetcher`, the JDK 21 built-in `jdk.httpserver` module (`com.sun.net.httpserver.HttpServer`) provides a lightweight in-process HTTP server that:

- Binds to a random port (`new InetSocketAddress(0)`) — no port conflicts in CI
- Serves static byte content for simulating remote file responses
- Is available without any pom.xml additions — it is part of JDK distribution
- Starts/stops in `@BeforeEach`/`@AfterEach` — suitable for JUnit 5 test lifecycle

**Verified by direct test:** `HttpServer.create(new InetSocketAddress(0), 0)` works without `--add-opens` or other JVM flags in JDK 21.

If more complex HTTP mock behavior is needed (conditional responses, latency simulation, etc.), WireMock 3.x is an option — but for v3 fetch-and-copy semantics, the built-in server is sufficient.

```java
// Example test setup
HttpServer mockServer;
int mockPort;

@BeforeEach
void startServer() throws Exception {
    mockServer = HttpServer.create(new InetSocketAddress(0), 0);
    mockPort = mockServer.getAddress().getPort();
    mockServer.createContext("/action.yml", exchange -> {
        byte[] body = "name: test".getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(200, body.length);
        exchange.getResponseBody().write(body);
        exchange.close();
    });
    mockServer.start();
}

@AfterEach
void stopServer() {
    mockServer.stop(0);
}
```

**Confidence: HIGH** — verified by direct JDK 21 execution.

---

## No Changes Needed

| Capability | How It Works in v3 | Why No Change |
|------------|-------------------|---------------|
| Config YAML load/save | `ObjectMapper(YAMLFactory)` on new POJOs (`SourceEntry`, `DestinationEntry`, `MappingEntry`) | Schema changes, not library changes |
| Freemarker expansion | `Configuration(VERSION_2_3_34)` per `templates:true` source | Same pattern as v2 `PathExpander` |
| picocli command tree | New `@Command` subcommand classes (`SourceCommand`, `DestinationCommand`) | Same pattern as v2 |
| Micronaut DI | New `@Singleton` service classes wired via `@Inject` | Same pattern as v2 |
| Skip-existing on generate | `Files.exists()` check before copy | Unchanged |
| Conflict detection on sync | `Files.mismatch()` (JDK 12+ NIO) | Unchanged |
| YAML config path (XDG) | `XdgPaths` utility | Unchanged |

---

## pom.xml Changes Required

**Add nothing.** All v3 capabilities are covered by the existing dependency set plus JDK 21 standard library. The `java.net.http` and `jdk.httpserver` modules are available without explicit module declarations in a non-modular Maven project (standard classpath mode).

Verify `<freemarker.version>` is `2.3.34` (already correct in pom.xml — the old STACK.md cited 2.3.33 but pom.xml has 2.3.34).

---

## Alternatives Considered

| Decision | Alternative | Why Rejected |
|----------|-------------|--------------|
| JDK HttpClient for remote fetch | `io.micronaut:micronaut-http-client-jdk` | Framework overhead; reactive API not needed for fire-and-forget GET |
| JDK HttpClient for remote fetch | OkHttp 4.12 | Adds Kotlin stdlib transitive dep; no capability advantage for simple GET |
| JDK HttpClient for remote fetch | Apache HttpClient 5.x | ~800KB+ JARs; no advantage over JDK for simple use case |
| JDK PathMatcher for glob | Apache Commons IO `wildcardMatch` | Only `*`/`?`; no bracket expressions; less capable than JDK |
| JDK PathMatcher for glob | `io.github.GlobPattern` or similar | Additional dependency; JDK PathMatcher already sufficient |
| `jdk.httpserver` for test mocks | WireMock 3.x | WireMock is appropriate if conditional responses are needed; for v3 static GET, built-in server is sufficient |

---

## Version Compatibility

| Library | Version in Use | Compatible With | Notes |
|---------|---------------|-----------------|-------|
| Freemarker | 2.3.34 | JDK 21 | Stable 2.3.x branch; no known JDK 21 issues |
| jackson-dataformat-yaml | BOM-managed (2.18.x range) | Micronaut 4.10.10 | Do not override BOM version |
| SnakeYAML | 2.4 (BOM-managed) | jackson-dataformat-yaml 2.18.x | Micronaut BOM ensures alignment |
| JDK HttpClient | Built-in JDK 21 | No extra deps | `AutoCloseable` contract added in JDK 21 |
| jdk.httpserver | Built-in JDK 21 | test scope only | `com.sun.*` but stable; in `jdk.httpserver` named module |

---

## Sources

- JDK 21 `HttpClient` API: https://docs.oracle.com/en/java/javase/21/docs/api/java.net.http/java/net/http/HttpClient.html — HIGH confidence
- JDK 21 `FileSystem.getPathMatcher`: https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/nio/file/FileSystem.html — HIGH confidence (fetched)
- JDK 21 `Files` tree walking: https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/nio/file/Files.html — HIGH confidence (fetched)
- Micronaut 4.10.10 platform BOM: `/home/jmochel/.m2/repository/io/micronaut/platform/micronaut-platform/4.10.10/micronaut-platform-4.10.10.pom` — HIGH confidence (local)
- PathMatcher glob behavior: verified by direct JDK 21 test execution (see test output in research notes) — HIGH confidence
- `jdk.httpserver` availability: verified by direct JDK 21 test execution — HIGH confidence

---

*Stack research for: Viracocha v3 — unified sources/destinations with remote HTTP source support*
*Researched: 2026-05-08*
