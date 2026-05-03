package com.recetea.infrastructure.metrics;

import com.recetea.core.shared.application.ports.out.IMetricsPort;

/**
 * Zero-cost default. Used in tests and as a safe fallback when no metrics
 * backend is configured at the composition root.
 */
public final class NoOpMetricsAdapter implements IMetricsPort {

    @Override public void recordQueryTime(String context, long nanos) {}
    @Override public void recordCacheHit(String catalogue) {}
    @Override public void recordFileOperation(String operation) {}
}
