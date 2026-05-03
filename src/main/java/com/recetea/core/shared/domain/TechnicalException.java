package com.recetea.core.shared.domain;

import java.util.Objects;

/**
 * Application-layer cap on infrastructure failures. Concrete adapters subclass this
 * type (database adapter → {@code InfrastructureException}; XML interop adapter →
 * {@code XmlInteropException}; etc.), but use cases and ports observe only the
 * parent — they never need to import jdbc / JAXB concretions to handle a failure.
 *
 * <p>Each subclass carries a stable {@link #technicalCode()} (e.g. {@code "INF-500"},
 * {@code "INT-400"}) that downstream observers — error renderer, structured logging,
 * support-ticket correlation — can match against without performing {@code instanceof}
 * checks against infrastructure types.
 */
public class TechnicalException extends RuntimeException {

    private final String technicalCode;

    public TechnicalException(String message, String technicalCode) {
        this(message, technicalCode, null);
    }

    public TechnicalException(String message, String technicalCode, Throwable cause) {
        super(message, cause);
        this.technicalCode = Objects.requireNonNull(technicalCode, "technicalCode must not be null.");
    }

    /** Stable identifier for the technical category, e.g. {@code "INF-500"} or {@code "INT-400"}. */
    public String technicalCode() {
        return technicalCode;
    }
}
