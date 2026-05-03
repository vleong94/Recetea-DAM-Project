package com.recetea.infrastructure.storage;

import com.recetea.core.shared.domain.utils.ExecutionContext;
import com.recetea.infrastructure.persistence.recipe.jdbc.InfrastructureException;
import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pure-HTTP integration test for {@link SupabaseMediaStorage}: routes the adapter
 * at a {@code MockWebServer} bound to localhost so cloud-specific failure shapes
 * (403 RLS denial, 413 storage-limit overflow, read-timeout) can be simulated
 * deterministically without touching the public Supabase endpoint.
 *
 * <p><b>Note on package</b>: imports {@link MockWebServer} from {@code mockwebserver3}
 * (the canonical 5.x API). The legacy {@code okhttp3.mockwebserver} bridge that ships in
 * mockwebserver-5.3.2 silently fails to dispatch enqueued responses — every request times
 * out regardless of what's queued — so we use the real underlying API instead. The behaviour
 * verified is identical and the public class name is still {@code MockWebServer}.
 *
 * <p><b>No external dependencies</b>: no internet, no PostgreSQL — does NOT extend
 * {@code BaseRepositoryTest}. The test runs against a freshly-bound ephemeral port
 * inside the JVM, so it is safe to execute in air-gapped CI / hermetic build envs.
 */
@DisplayName("SupabaseMediaStorage — cloud-failure mapping with TraceID-aware InfrastructureException")
class SupabaseMediaStorageIntegrationTest {

    /** Auth token never reaches a real backend — MockWebServer accepts whatever Authorization header arrives. */
    private static final String DUMMY_TOKEN = "test-service-key-dummy";
    /** Bucket name embedded into the public-URL shape that {@code SupabaseMediaStorage} validates. */
    private static final String BUCKET      = "recipes";

    private MockWebServer        server;
    private SupabaseMediaStorage adapter;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();

        // 3s read timeout: long enough for the mock server's normal response cycle
        // (typically <50ms on localhost), short enough to keep the timeout-scenario test
        // (5s body delay) runnable inside a sane wall-clock budget.
        OkHttpClient fastClient = new OkHttpClient.Builder()
                .connectTimeout(Duration.ofSeconds(2))
                .writeTimeout(Duration.ofSeconds(2))
                .readTimeout(Duration.ofSeconds(3))
                // Force HTTP/1.1: OkHttp 5 will otherwise advertise H2C cleartext
                // upgrade headers that older MockWebServer dispatchers can leave parked.
                .protocols(List.of(Protocol.HTTP_1_1))
                .build();

        String publicReadUrl = server.url("/storage/v1/object/public/" + BUCKET + "/").toString();
        adapter = new SupabaseMediaStorage(publicReadUrl, DUMMY_TOKEN, fastClient);
    }

    @AfterEach
    void tearDown() throws IOException {
        if (server != null) server.close();
    }

    private static InputStream payload() {
        return new ByteArrayInputStream("dummy image bytes".getBytes(StandardCharsets.UTF_8));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2.1 Authentication failure (403)
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("store: 403 → InfrastructureException naming 'Forbidden'")
    void store_WrapsForbiddenAsInfrastructureException() {
        // Explicit HTTP status line: MockResponse.Builder.code(403) alone yields the
        // generic phrase "Client Error" — we want the canonical "Forbidden" so the
        // adapter's wrapped InfrastructureException reads naturally to operators.
        server.enqueue(new MockResponse.Builder()
                .status("HTTP/1.1 403 Forbidden")
                .body("{\"message\":\"new row violates row-level security policy\"}")
                .build());

        InfrastructureException ex = assertThrows(InfrastructureException.class,
                () -> adapter.store(payload(), "image.jpg"));

        String msg = ex.getMessage();
        assertTrue(msg.contains("403"),       "Message should embed the HTTP status code; was: " + msg);
        assertTrue(msg.contains("Forbidden"), "Message should embed the HTTP reason phrase 'Forbidden'; was: " + msg);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2.2 Storage limit exceeded (413)
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("store: 413 → InfrastructureException naming 'Payload Too Large'")
    void store_WrapsPayloadTooLargeAsInfrastructureException() {
        server.enqueue(new MockResponse.Builder()
                .status("HTTP/1.1 413 Payload Too Large")
                .body("{\"message\":\"Payload exceeds storage tier limit\"}")
                .build());

        InfrastructureException ex = assertThrows(InfrastructureException.class,
                () -> adapter.store(payload(), "huge.png"));

        String msg = ex.getMessage();
        assertTrue(msg.contains("413"),                "Message should embed the HTTP status code; was: " + msg);
        assertTrue(msg.contains("Payload Too Large"),  "Message should embed the HTTP reason phrase 'Payload Too Large'; was: " + msg);
        // The adapter mints a UUID-based filename but preserves the original extension —
        // so the message names a *.png upload, even if the leading UUID differs every run.
        assertTrue(msg.contains(".png"),               "Message should preserve the file extension that triggered the failure; was: " + msg);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2.3 Read-timeout (network failure)
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("store: response delayed beyond read timeout → SocketTimeoutException wrapped as InfrastructureException")
    void store_WrapsSocketTimeoutAsInfrastructureException() {
        // headersDelay (not bodyDelay): the adapter only reads the response status — never the
        // body — on the success path, so a body-only delay would not trigger any timeout. The
        // header delay forces OkHttp's read timeout to fire while waiting for the status line.
        server.enqueue(new MockResponse.Builder()
                .code(200)
                .body("late body")
                .headersDelay(6, TimeUnit.SECONDS)
                .build());

        InfrastructureException ex = assertThrows(InfrastructureException.class,
                () -> adapter.store(payload(), "slow.jpg"));

        String msg = ex.getMessage();
        assertTrue(msg.contains("Supabase upload IO error"), "Message should announce the IO-error category; was: " + msg);
        assertTrue(msg.contains(".jpg"),                     "Message should preserve the file extension that timed out; was: " + msg);
        // The original SocketTimeoutException is the cause chain.
        assertNotNull(ex.getCause(), "Cause must be preserved for stack-trace forensics");
        assertEquals("java.net.SocketTimeoutException", ex.getCause().getClass().getName(),
                "OkHttp's SocketTimeoutException must be the wrapped cause");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Traceability: TraceID is folded into the message when a CID scope is bound
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("store: when invoked inside an ExecutionContext scope the TraceID is embedded in the InfrastructureException")
    void store_EmbedsTraceIdWhenCorrelationScopeIsBound() {
        server.enqueue(new MockResponse.Builder()
                .status("HTTP/1.1 403 Forbidden").body("forbidden").build());

        InfrastructureException ex = ExecutionContext.call(() -> assertThrows(InfrastructureException.class,
                () -> adapter.store(payload(), "image.jpg")));

        assertTrue(ex.getMessage().startsWith("[TraceID:"),
                "InfrastructureException must prepend the active correlation id so the message survives MDC cleanup");
    }
}
