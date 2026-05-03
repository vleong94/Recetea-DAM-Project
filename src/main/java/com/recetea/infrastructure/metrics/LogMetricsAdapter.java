package com.recetea.infrastructure.metrics;

import com.recetea.core.shared.application.ports.out.IMetricsPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * Forwards all metric events to SLF4J. Slow queries (above the configured
 * threshold) escalate to WARN so they appear in the default-level Logback
 * output; everything else stays at DEBUG and is silent in production.
 */
public final class LogMetricsAdapter implements IMetricsPort {

    private static final Logger log = LoggerFactory.getLogger(LogMetricsAdapter.class);

    private static final long DEFAULT_SLOW_QUERY_NANOS = TimeUnit.MILLISECONDS.toNanos(500);

    private final long slowQueryThresholdNanos;

    public LogMetricsAdapter() {
        this(DEFAULT_SLOW_QUERY_NANOS);
    }

    public LogMetricsAdapter(long slowQueryThresholdNanos) {
        this.slowQueryThresholdNanos = slowQueryThresholdNanos;
    }

    @Override
    public void recordQueryTime(String context, long nanos) {
        if (nanos >= slowQueryThresholdNanos) {
            log.warn("Slow operation [{}]: {} ms", context, TimeUnit.NANOSECONDS.toMillis(nanos));
        } else if (log.isDebugEnabled()) {
            log.debug("Operation [{}]: {} ms", context, TimeUnit.NANOSECONDS.toMillis(nanos));
        }
    }

    @Override
    public void recordCacheHit(String catalogue) {
        if (log.isDebugEnabled()) log.debug("Cache hit: {}", catalogue);
    }

    @Override
    public void recordFileOperation(String operation) {
        if (log.isDebugEnabled()) log.debug("File op: {}", operation);
    }
}
