package com.recetea.infrastructure.persistence.recipe.jdbc;

import com.recetea.core.shared.domain.TechnicalException;
import com.recetea.core.shared.domain.utils.ExecutionContext;

/**
 * Database / persistence-layer failure. Extends {@link TechnicalException} with the
 * stable code {@code "INF-500"} so the application layer never needs to import this
 * type to handle a persistence error — observing {@code TechnicalException} is enough.
 *
 * <p>The TraceID is captured into the message at construction time so it survives
 * MDC cleanup. By the time the {@code GlobalExceptionHandler} logs an uncaught
 * InfrastructureException, the originating CID scope has already closed and MDC
 * has been cleared — but the message itself still names the trace, which makes any
 * production support ticket immediately correlatable to its log line.
 */
public class InfrastructureException extends TechnicalException {

    private static final String CODE = "INF-500";

    public InfrastructureException(String message, Throwable cause) {
        super(prependTrace(message), CODE, cause);
    }

    private static String prependTrace(String message) {
        return ExecutionContext.currentCorrelationId()
                .map(cid -> "[TraceID: " + cid + "] " + message)
                .orElse(message);
    }
}
