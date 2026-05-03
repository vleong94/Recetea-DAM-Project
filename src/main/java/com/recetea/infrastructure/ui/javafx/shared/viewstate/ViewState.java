package com.recetea.infrastructure.ui.javafx.shared.viewstate;

import java.util.List;

/**
 * Sealed view-state hierarchy for list-driven UI surfaces.
 *
 * <p>The four cases correspond to the lifecycle of an asynchronous list fetch:
 * <ul>
 *   <li>{@link Loading} — request in flight, no payload yet.</li>
 *   <li>{@link Empty}   — request resolved successfully but produced no items
 *       (or the user's filters narrowed the visible set to nothing).</li>
 *   <li>{@link Error}   — request failed; carries a localised user-facing message.</li>
 *   <li>{@link Success} — request resolved with at least one item.</li>
 * </ul>
 *
 * <p>Controllers consume a {@code ViewState<T>} via a pattern-matching switch — each
 * case toggles the relevant UI nodes (skeleton loader, empty placeholder, gallery
 * container, inline error toast) declaratively, so the visibility logic for the
 * whole surface lives in one expression instead of being scattered across the load
 * lifecycle.
 *
 * <p>The static factories ({@link #loading()}, {@link #empty()}, {@link #error},
 * {@link #success}) return the canonical singletons for the data-free cases
 * (avoiding a fresh {@code new Loading<>()} on every transition) and just delegate
 * to the record constructors for the data-bearing ones.
 */
public sealed interface ViewState<T>
        permits ViewState.Loading, ViewState.Empty, ViewState.Error, ViewState.Success {

    record Loading<T>()                    implements ViewState<T> {}
    record Empty<T>()                      implements ViewState<T> {}
    record Error<T>(String message)        implements ViewState<T> {}
    record Success<T>(List<T> data)        implements ViewState<T> {}

    /** Cached singletons — the data-free cases carry no state and need only one instance. */
    Loading<?> LOADING_INSTANCE = new Loading<>();
    Empty<?>   EMPTY_INSTANCE   = new Empty<>();

    @SuppressWarnings("unchecked")
    static <T> Loading<T> loading() { return (Loading<T>) LOADING_INSTANCE; }

    @SuppressWarnings("unchecked")
    static <T> Empty<T> empty() { return (Empty<T>) EMPTY_INSTANCE; }

    static <T> Error<T> error(String message) { return new Error<>(message); }

    static <T> Success<T> success(List<T> data) { return new Success<>(data); }
}
