package com.recetea.infrastructure.ui.javafx.utils;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.input.KeyCode;
import javafx.util.Duration;
import javafx.util.StringConverter;

import java.text.Normalizer;
import java.util.function.Function;

/**
 * Reusable searchable / autocomplete behaviour for an editable {@link ComboBox}.
 *
 * <p>Caller supplies the combo, the source list (full catalogue), a {@link FilteredList}
 * wrapping the source, a display-text extractor, and a {@link SearchConfig}. The helper
 * wires:
 *
 * <ul>
 *   <li>{@code combo.setEditable(true)} + {@code combo.setItems(filtered)} so the dropdown
 *       reflects the live filter view.</li>
 *   <li>A {@link StringConverter} that renders display text on the way out and resolves
 *       case-insensitive exact matches on commit (Tab / Enter / focus loss). Matching
 *       returns the underlying item so the combo's {@code valueProperty} stays type-safe.</li>
 *   <li>A debounced editor-text listener that updates the {@link FilteredList} predicate.
 *       Three regimes — empty editor: show all; below {@code minChars}: hide; ≥ threshold:
 *       case- and accent-insensitive contains-match capped at {@code maxResults}.</li>
 *   <li>A "no match" visual: the {@code state-danger} CSS class is added when the current
 *       editor text resolves to zero items, removed otherwise. Style rule lives in app.css.</li>
 *   <li>An arrow-open override: clicking the chevron with an empty editor resets the
 *       predicate to show every item; with text already in the editor the typing filter's
 *       last verdict holds.</li>
 * </ul>
 *
 * <p><b>Caller responsibility</b>: attach a {@code valueProperty} listener for selection
 * effects (chip add, downstream filter rebuild, etc.) — the helper does not assume any
 * specific reaction to a successful pick.
 */
public final class AutocompleteHelper {

    /** CSS class flipped on the combo when the typed text matches no item. Targeted by app.css. */
    public static final String STATE_DANGER = "state-danger";

    /**
     * Tunable search regime. Larger catalogues benefit from a higher {@code minChars}
     * (avoid flooding the dropdown on a single keystroke); small fixed-set catalogues
     * (categories, difficulties) work better with {@code minChars = 1}.
     * {@code maxResults} caps the dropdown so an autocomplete on thousands of authors
     * doesn't render an unscrollable wall.
     */
    public record SearchConfig(int minChars, Duration debounce, int maxResults) {

        /** 3-char threshold, 300 ms debounce, no cap — sane defaults for catalogues of dozens. */
        public static SearchConfig defaults() {
            return new SearchConfig(3, Duration.millis(300), Integer.MAX_VALUE);
        }

        /** 1-char threshold, 300 ms debounce, no cap — tiny catalogues (categories, difficulties). */
        public static SearchConfig instant() {
            return new SearchConfig(1, Duration.millis(300), Integer.MAX_VALUE);
        }

        /** 1-char threshold, 200 ms debounce, top-N cap — large catalogues (authors). */
        public static SearchConfig topN(int n) {
            return new SearchConfig(1, Duration.millis(200), n);
        }
    }

    private AutocompleteHelper() {}

    /**
     * Single entry point. Call once at controller {@code init} time after both the source
     * list and the FilteredList wrapper have been created. Idempotent — calling twice on
     * the same combo replaces the previous wiring.
     */
    public static <T> void wire(ComboBox<T> combo,
                                ObservableList<T> source,
                                FilteredList<T> filtered,
                                Function<T, String> displayText,
                                SearchConfig config) {
        combo.setItems(filtered);
        combo.setEditable(true);
        installDisplay(combo, source, displayText);
        installDebouncedFilter(combo, filtered, displayText, config);
        installArrowOverride(combo, filtered);
    }

    /**
     * Convenience entry point for the recipe-form pattern. Wraps the supplied source list
     * in a {@link FilteredList}, wires it through {@link #wire} with a 1-character
     * threshold, and adds three UX layers tuned for the larger (80+ ingredient) catalogues:
     * <ul>
     *   <li><b>{@code setVisibleRowCount(10)}</b> — caps the dropdown so a contains-match
     *       on a short prefix doesn't overflow the screen.</li>
     *   <li><b>TAB / ENTER auto-select</b> — when the editor has filtered results but no
     *       value committed yet, pressing TAB or ENTER picks the first filtered item.
     *       Exact-match commits are still handled by the {@link StringConverter} installed
     *       in {@link #wire}, so typing a full name keeps its original behaviour.</li>
     *   <li><b>Caret-at-end on popup show</b> — JavaFX occasionally resets the caret to
     *       position 0 when the dropdown re-renders. We {@link Platform#runLater} a
     *       caret reposition so the user keeps typing where they expect.</li>
     * </ul>
     *
     * <p>Caller still owns {@code source} — mutating it (e.g. {@code source.setAll(...)} at
     * {@code init} time) propagates to the FilteredList automatically.
     */
    public static <T> void setupSearchableComboBox(ComboBox<T> combo,
                                                   ObservableList<T> source,
                                                   Function<T, String> displayText) {
        FilteredList<T> filtered = new FilteredList<>(source, t -> true);
        wire(combo, source, filtered, displayText, SearchConfig.instant());
        combo.setVisibleRowCount(10);
        installEnterTabAutoSelect(combo, filtered);
        installCaretAtEndOnShow(combo);
    }

    /** Removes the {@link #STATE_DANGER} class from {@code combo}. Public so controllers
     *  can clear the visual state from their own value-change listeners after a successful pick. */
    public static void clearDangerState(ComboBox<?> combo) {
        combo.getStyleClass().remove(STATE_DANGER);
    }

    /**
     * NFD-decomposed, lower-cased form for case- and accent-insensitive matching.
     * "HUEVO", "huevo", and "Huëvo" all collapse to the same key, so a typed
     * "hue" matches every variant in the same dropdown.
     */
    public static String normalize(String s) {
        if (s == null) return "";
        return Normalizer.normalize(s.trim(), Normalizer.Form.NFD)
                .replaceAll("[\\u0300-\\u036f]+", "")
                .toLowerCase();
    }

    // ── internals ───────────────────────────────────────────────────────────

    /**
     * Two responsibilities, kept together because they always travel as a pair:
     * <ol>
     *   <li><b>StringConverter</b> — toString delegates to {@code displayText};
     *       fromString resolves to the matching item in {@code source} (case-insensitive
     *       exact match), or null if nothing matches. Returning null on no-match keeps
     *       the typed valueProperty type-safe; returning the matching item lets focus-loss
     *       commits resolve to a real selection when the user typed an exact name.</li>
     *   <li><b>Cell factory + button cell</b> — both render the same display text via a
     *       fresh {@code ListCell}, so the dropdown rows and the closed-combo label both
     *       show only the name (no record-style metadata).</li>
     * </ol>
     */
    private static <T> void installDisplay(ComboBox<T> combo,
                                           ObservableList<T> source,
                                           Function<T, String> displayText) {
        combo.setConverter(new StringConverter<T>() {
            @Override public String toString(T item) {
                return item == null ? "" : displayText.apply(item);
            }
            @Override public T fromString(String s) {
                if (s == null || s.isBlank()) return null;
                String needle = s.trim();
                return source.stream()
                        .filter(item -> {
                            String name = displayText.apply(item);
                            return name != null && name.equalsIgnoreCase(needle);
                        })
                        .findFirst()
                        .orElse(null);
            }
        });
        combo.setCellFactory(lv -> renderCell(displayText));
        combo.setButtonCell(renderCell(displayText));
    }

    /**
     * Debounced editor listener. The {@link PauseTransition} restarts on every keystroke
     * so we run the predicate update once per typing burst, not once per character.
     */
    private static <T> void installDebouncedFilter(ComboBox<T> combo,
                                                   FilteredList<T> filtered,
                                                   Function<T, String> displayText,
                                                   SearchConfig config) {
        PauseTransition pause = new PauseTransition(config.debounce());
        pause.setOnFinished(e -> applyTypingFilter(combo, filtered, displayText, config));
        combo.getEditor().textProperty().addListener((obs, o, n) -> pause.playFromStart());
    }

    private static <T> void applyTypingFilter(ComboBox<T> combo,
                                              FilteredList<T> filtered,
                                              Function<T, String> displayText,
                                              SearchConfig config) {
        String raw = combo.getEditor().getText();
        String needle = normalize(raw);

        // ── below threshold: hide dropdown, no danger (incomplete input ≠ error) ──
        if (needle.isEmpty()) {
            filtered.setPredicate(item -> true);
            clearDangerState(combo);
            if (combo.isShowing()) combo.hide();
            return;
        }
        if (needle.length() < config.minChars()) {
            filtered.setPredicate(item -> false);
            clearDangerState(combo);
            if (combo.isShowing()) combo.hide();
            return;
        }

        // ── ≥ threshold: case + accent-insensitive contains, capped at maxResults ──
        // FilteredList walks the source in order; the counter trims to the first N hits
        // without materialising a separate list. Reset on each setPredicate by closing
        // over a fresh int[] each call.
        final int cap = config.maxResults();
        final int[] count = {0};
        filtered.setPredicate(item -> {
            String name = displayText.apply(item);
            if (name == null || !normalize(name).contains(needle)) return false;
            return cap == Integer.MAX_VALUE || ++count[0] <= cap;
        });

        if (filtered.isEmpty()) {
            applyDangerState(combo);
            if (combo.isShowing()) combo.hide();
            return;
        }
        clearDangerState(combo);

        // Suppress the re-fire that follows a successful pick: when the user clicks an
        // item, StringConverter.toString writes the picked name back into the editor,
        // which schedules another debounce → applyTypingFilter call. If we showed again
        // here, the dropdown would re-pop 300 ms after every pick. Skip when the typed
        // text already matches the current selection's display name exactly.
        T currentValue = combo.getValue();
        if (currentValue != null) {
            String currentName = displayText.apply(currentValue);
            if (currentName != null && currentName.equalsIgnoreCase(raw.trim())) return;
        }

        if (!combo.isShowing()) combo.show();
    }

    /**
     * When the dropdown is opened via the arrow with an empty editor, reset the predicate
     * to show every item. If the user has already typed (anything), respect their input —
     * the typing filter's last verdict holds.
     */
    private static <T> void installArrowOverride(ComboBox<T> combo, FilteredList<T> filtered) {
        combo.setOnShowing(e -> {
            String text = combo.getEditor().getText();
            if (text == null || text.isBlank()) {
                filtered.setPredicate(item -> true);
            }
        });
    }

    private static <T> ListCell<T> renderCell(Function<T, String> displayText) {
        return new ListCell<>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(displayText.apply(item));
                    if (!getStyleClass().contains("text-small")) getStyleClass().add("text-small");
                }
            }
        };
    }

    private static void applyDangerState(ComboBox<?> combo) {
        if (!combo.getStyleClass().contains(STATE_DANGER)) {
            combo.getStyleClass().add(STATE_DANGER);
        }
    }

    /**
     * Editor-level key handler. The default {@link ComboBox} commits TAB / ENTER through
     * the {@link StringConverter}, which only matches exact names — so a user who typed
     * a 3-letter prefix and pressed TAB ended up with a null value. Here we intercept
     * the keystroke and, if the editor still has no committed value but the FilteredList
     * has at least one match, select the first one. ENTER also closes the popup; TAB lets
     * JavaFX's traversal advance focus to the next field naturally.
     */
    private static <T> void installEnterTabAutoSelect(ComboBox<T> combo, FilteredList<T> filtered) {
        combo.getEditor().setOnKeyPressed(event -> {
            if (event.getCode() != KeyCode.TAB && event.getCode() != KeyCode.ENTER) return;
            if (combo.getValue() != null) return;        // exact-match path already committed
            if (filtered.isEmpty()) return;              // nothing to pick from
            combo.setValue(filtered.get(0));
            if (event.getCode() == KeyCode.ENTER) {
                combo.hide();
                event.consume();                         // suppress accidental "default button" trigger
            }
        });
    }

    /**
     * Forces the caret to the end of the editor text every time the popup opens.
     * The {@link Platform#runLater} hop is required because the popup-open machinery
     * may reset the caret to 0 on layout — we want the next keystroke to extend the
     * existing search term, not insert at the start.
     */
    private static void installCaretAtEndOnShow(ComboBox<?> combo) {
        combo.showingProperty().addListener((obs, was, isNow) -> {
            if (!isNow) return;
            Platform.runLater(() -> {
                String text = combo.getEditor().getText();
                if (text != null) combo.getEditor().positionCaret(text.length());
            });
        });
    }
}
