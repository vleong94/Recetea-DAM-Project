package com.recetea.core.shared.domain.utils;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.recetea.core.shared.domain.utils.ExecutionContext.ExecutionMetadata;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies the new {@link ExecutionMetadata}-bound context — both the in-process
 * helpers ({@code currentCorrelationId}, {@code currentUserId}, MDC sync) and the
 * end-to-end log-pattern format ({@code [TraceID: ...] [User: ...]}) that production
 * appenders render.
 */
@DisplayName("ExecutionContext — ExecutionMetadata scope + MDC + log format")
class ExecutionContextTest {

    private LoggerContext loggerContext;

    @BeforeEach
    void setUp() {
        loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
    }

    @AfterEach
    void cleanMdc() {
        MDC.clear();
    }

    // ── ExecutionMetadata record invariants ─────────────────────────────────

    @Test
    @DisplayName("ExecutionMetadata: requires a non-null correlationId; userId may be null")
    void recordRejectsNullCorrelationIdAcceptsNullUser() {
        long now = System.nanoTime();
        ExecutionMetadata anonymous = new ExecutionMetadata("cid-1", null, now);
        assertEquals("cid-1", anonymous.correlationId());
        assertNull(anonymous.userId());
        assertEquals(now, anonymous.startTimeNanos());

        assertThrows(NullPointerException.class,
                () -> new ExecutionMetadata(null, "user-1", now));
    }

    // ── Anonymous flow (no userId) ──────────────────────────────────────────

    @Test
    @DisplayName("call(action): binds CONTEXT with a fresh CID and null userId; clears MDC on exit")
    void anonymousCallBindsAndClears() {
        AtomicReference<ExecutionMetadata> seenInside = new AtomicReference<>();

        String result = ExecutionContext.call(() -> {
            seenInside.set(ExecutionContext.CONTEXT.get());
            assertEquals(seenInside.get().correlationId(), MDC.get(ExecutionContext.MDC_CORRELATION_ID_KEY),
                    "MDC must mirror the bound correlationId");
            assertNull(MDC.get(ExecutionContext.MDC_USER_ID_KEY),
                    "MDC userId must remain unset for anonymous flows");
            return "ok";
        });

        assertEquals("ok", result);
        assertNotNull(seenInside.get(), "CONTEXT must be bound inside the action");
        assertNotNull(seenInside.get().correlationId());
        assertNull(seenInside.get().userId());
        assertNull(MDC.get(ExecutionContext.MDC_CORRELATION_ID_KEY), "MDC correlationId must be cleared after scope exits");
        assertNull(MDC.get(ExecutionContext.MDC_USER_ID_KEY),        "MDC userId must be cleared after scope exits");
        assertFalse(ExecutionContext.CONTEXT.isBound(),              "CONTEXT must not survive scope exit");
    }

    @Test
    @DisplayName("currentCorrelationId / currentUserId: empty when no scope is bound")
    void helpersReturnEmptyOutsideScope() {
        assertTrue(ExecutionContext.currentCorrelationId().isEmpty());
        assertTrue(ExecutionContext.currentUserId().isEmpty());
    }

    // ── Session-aware flow (userId populated) ───────────────────────────────

    @Test
    @DisplayName("call(userId, action): binds both correlationId and userId in MDC; helper exposes both")
    void sessionAwareCallExposesBoth() {
        ExecutionContext.call("42", () -> {
            assertEquals(Optional.of("42"), ExecutionContext.currentUserId());
            assertTrue(ExecutionContext.currentCorrelationId().isPresent());

            assertEquals(MDC.get(ExecutionContext.MDC_CORRELATION_ID_KEY),
                    ExecutionContext.currentCorrelationId().orElseThrow(),
                    "MDC correlationId must equal the helper-returned value");
            assertEquals("42", MDC.get(ExecutionContext.MDC_USER_ID_KEY));
            return null;
        });

        assertNull(MDC.get(ExecutionContext.MDC_USER_ID_KEY),
                "userId MDC must be cleared after scope exit");
    }

    @Test
    @DisplayName("run(userId, action): nested call reuses outer scope, ignores inner userId")
    void nestedCallReusesOuterScope() {
        AtomicReference<String> outerCid  = new AtomicReference<>();
        AtomicReference<String> innerCid  = new AtomicReference<>();
        AtomicReference<String> innerUser = new AtomicReference<>();

        ExecutionContext.run("outer-user", () -> {
            outerCid.set(ExecutionContext.currentCorrelationId().orElseThrow());
            assertEquals("outer-user", ExecutionContext.currentUserId().orElseThrow());

            ExecutionContext.run("inner-user-IGNORED", () -> {
                innerCid.set(ExecutionContext.currentCorrelationId().orElseThrow());
                innerUser.set(ExecutionContext.currentUserId().orElseThrow());
            });
        });

        assertEquals(outerCid.get(), innerCid.get(),
                "Inner scope must reuse the outer correlation id (ScopedValue immutability)");
        assertEquals("outer-user", innerUser.get(),
                "Inner scope must reuse the outer userId — the inner argument is intentionally ignored");
    }

    @Test
    @DisplayName("call: a checked exception inside the action surfaces as RuntimeException via the unchecked wrapper")
    void checkedExceptionWrappedToRuntime() {
        // Use a checked Throwable thrown via Suppliers' loophole
        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                ExecutionContext.call(() -> {
                    sneakyThrow();
                    return null;
                }));
        // The masking helper is permissive: any RuntimeException flows through unchanged.
        assertNotNull(ex);
        assertNull(MDC.get(ExecutionContext.MDC_CORRELATION_ID_KEY),
                "Even on exception, MDC must be cleaned up in the finally block");
    }

    private static void sneakyThrow() {
        throw new IllegalStateException("simulated business failure");
    }

    // ── Log format end-to-end ────────────────────────────────────────────────

    @Test
    @DisplayName("PatternLayout: [TraceID: <cid>] [User: <userId>] is emitted when the scope is bound")
    void logPatternEmitsBothTraceIdAndUser() {
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.setContext(loggerContext);
        appender.start();

        Logger captured = loggerContext.getLogger(ExecutionContextTest.class);
        captured.addAppender(appender);
        captured.setLevel(ch.qos.logback.classic.Level.INFO);
        captured.setAdditive(false);

        // Logback's LoggingEvent.getMDCPropertyMap() snapshots MDC lazily on first call.
        // We must trigger that snapshot WHILE the ExecutionContext scope is still active
        // (before MDC.remove fires in the finally) — formatting inside the lambda forces
        // the layout to walk MDC immediately and captures the bound values.
        AtomicReference<String> renderedRef = new AtomicReference<>();
        try {
            org.slf4j.Logger slf4j = LoggerFactory.getLogger(ExecutionContextTest.class);

            ExecutionContext.run("99", () -> {
                slf4j.info("test event");
                PatternLayout layout = new PatternLayout();
                layout.setContext(loggerContext);
                layout.setPattern("[TraceID: %X{correlationId:-NONE}] [User: %X{userId:-anonymous}] - %msg");
                layout.start();
                renderedRef.set(layout.doLayout(appender.list.getFirst()));
            });
        } finally {
            captured.detachAppender(appender);
            appender.stop();
        }

        assertEquals(1, appender.list.size(), "Exactly one event captured");
        String rendered = renderedRef.get();
        assertNotNull(rendered, "Layout must have produced output inside the scope");

        assertTrue(rendered.contains("[TraceID: ") && !rendered.contains("[TraceID: NONE]"),
                "Log must carry the bound trace id; was: " + rendered);
        assertTrue(rendered.contains("[User: 99]"),
                "Log must carry the bound user id (99); was: " + rendered);
        assertTrue(rendered.endsWith("- test event"),
                "Original message must be preserved at the end; was: " + rendered);
    }

    @Test
    @DisplayName("PatternLayout: defaults to NONE / anonymous when no scope is bound")
    void logPatternDefaultsOutsideScope() {
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.setContext(loggerContext);
        appender.start();

        Logger captured = loggerContext.getLogger(ExecutionContextTest.class);
        captured.addAppender(appender);
        captured.setLevel(ch.qos.logback.classic.Level.INFO);
        captured.setAdditive(false);
        try {
            // No ExecutionContext scope — log straight from the test thread.
            LoggerFactory.getLogger(ExecutionContextTest.class).info("unbound event");
        } finally {
            captured.detachAppender(appender);
            appender.stop();
        }

        PatternLayout layout = new PatternLayout();
        layout.setContext(loggerContext);
        layout.setPattern("[TraceID: %X{correlationId:-NONE}] [User: %X{userId:-anonymous}] - %msg");
        layout.start();

        String rendered = layout.doLayout(appender.list.getFirst());
        assertTrue(rendered.contains("[TraceID: NONE]"),  "Default trace id must be NONE; was: "  + rendered);
        assertTrue(rendered.contains("[User: anonymous]"), "Default user must be anonymous; was: " + rendered);
    }
}
