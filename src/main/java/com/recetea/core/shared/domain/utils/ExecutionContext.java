package com.recetea.core.shared.domain.utils;

import org.slf4j.MDC;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Per-flow execution context carried via {@link ScopedValue}. Bindings survive
 * virtual-thread park/unmount cycles, so the metadata propagates through every
 * blocking JDBC or I/O call that the use case performs downstream.
 *
 * <p>The bound value is an immutable {@link ExecutionMetadata} record that captures:
 * <ul>
 *   <li><b>correlationId</b> — required, UUID minted at scope entry, surfaced as
 *       {@code [TraceID: ...]} in log lines and inside {@code InfrastructureException}
 *       messages so a production support ticket is always traceable to its log line.</li>
 *   <li><b>userId</b> — optional. Wrappers populate it from {@link IUserSessionService};
 *       {@code null} for anonymous flows (login, register, public read paths).</li>
 *   <li><b>startTimeNanos</b> — wall-clock latency anchor for downstream metrics that
 *       want to attribute end-to-end duration to a single user action.</li>
 * </ul>
 *
 * <p>The {@link #call} / {@link #run} helpers are idempotent: if a {@link #CONTEXT} is
 * already bound by an outer scope (typically the wrapper around the user action), the
 * inner call reuses it instead of generating a new one. SLF4J MDC is populated alongside
 * the ScopedValue binding so Logback patterns can reference {@code %X{correlationId}} and
 * {@code %X{userId}} without a custom converter. MDC is a thread-local that follows the
 * virtual thread across park/unmount, mirroring the ScopedValue lifetime.
 */
public final class ExecutionContext {

    /**
     * Immutable, structured per-flow context. {@code correlationId} is required;
     * {@code userId} may be null for anonymous flows. {@code startTimeNanos} captures
     * {@link System#nanoTime()} at scope entry — never wall-clock — so latency math
     * is monotonic across NTP corrections.
     */
    public record ExecutionMetadata(String correlationId, String userId, long startTimeNanos) {
        public ExecutionMetadata {
            Objects.requireNonNull(correlationId, "correlationId is required");
        }
    }

    /** Single ScopedValue carrying the full per-flow context. */
    public static final ScopedValue<ExecutionMetadata> CONTEXT = ScopedValue.newInstance();

    /** SLF4J MDC keys — must match the patterns in {@code logback.xml} / {@code logback-test.xml}. */
    public static final String MDC_CORRELATION_ID_KEY = "correlationId";
    public static final String MDC_USER_ID_KEY        = "userId";

    /** Backward-compat alias retained so existing call sites that referenced the trace key still compile. */
    public static final String MDC_KEY = MDC_CORRELATION_ID_KEY;

    private ExecutionContext() {}

    // ── Public entry points ─────────────────────────────────────────────────

    /** Anonymous variant: opens a fresh scope with {@code userId = null}. */
    public static <T> T call(Supplier<T> action) {
        return call(null, action);
    }

    /** Anonymous variant: opens a fresh scope with {@code userId = null}. */
    public static void run(Runnable action) {
        run(null, action);
    }

    /**
     * Session-aware variant. {@code userId} may be {@code null} (anonymous) — wrappers
     * pass {@code null} when {@link IUserSessionService#getCurrentUserId()} is empty.
     * If a {@link #CONTEXT} is already bound the inner call reuses it; the supplied
     * {@code userId} is ignored to keep ScopedValue immutability semantics clean.
     */
    public static <T> T call(String userId, Supplier<T> action) {
        if (CONTEXT.isBound()) return action.get();
        return withFreshContext(userId, action);
    }

    /** Session-aware {@code run} counterpart of {@link #call(String, Supplier)}. */
    public static void run(String userId, Runnable action) {
        if (CONTEXT.isBound()) { action.run(); return; }
        withFreshContext(userId, () -> { action.run(); return null; });
    }

    // ── Read-side helpers (replace the deleted ScopedValue<String> CORRELATION_ID) ─

    /** Returns the bound correlation ID, or empty if no scope is active. */
    public static Optional<String> currentCorrelationId() {
        return CONTEXT.isBound() ? Optional.of(CONTEXT.get().correlationId()) : Optional.empty();
    }

    /** Returns the bound user ID, or empty if no scope is active or the flow is anonymous. */
    public static Optional<String> currentUserId() {
        if (!CONTEXT.isBound()) return Optional.empty();
        return Optional.ofNullable(CONTEXT.get().userId());
    }

    // ── Internals ───────────────────────────────────────────────────────────

    private static <T> T withFreshContext(String userId, Supplier<T> action) {
        ExecutionMetadata meta = new ExecutionMetadata(
                UUID.randomUUID().toString(),
                userId,
                System.nanoTime());
        MDC.put(MDC_CORRELATION_ID_KEY, meta.correlationId());
        if (meta.userId() != null) MDC.put(MDC_USER_ID_KEY, meta.userId());
        try {
            return ScopedValue.where(CONTEXT, meta).call(action::get);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Unexpected checked exception in CID-bound action.", e);
        } finally {
            // Per-key remove rather than MDC.clear() so we don't trample anything an
            // outer (non-ExecutionContext) layer happened to put. If userId was never set
            // (anonymous flow), the remove is a harmless no-op.
            MDC.remove(MDC_CORRELATION_ID_KEY);
            MDC.remove(MDC_USER_ID_KEY);
        }
    }
}
