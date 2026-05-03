package com.recetea.infrastructure.ui.javafx.features.recipe;

import com.recetea.core.recipe.application.ports.in.dto.IngredientResponse;
import com.recetea.core.recipe.application.ports.in.dto.RecipeSummaryResponse;
import com.recetea.core.recipe.application.ports.in.dto.SearchCriteria;
import com.recetea.core.recipe.domain.Category;
import com.recetea.core.recipe.domain.Difficulty;
import com.recetea.infrastructure.ui.javafx.utils.AutocompleteHelper;
import javafx.animation.PauseTransition;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.util.Duration;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.function.Predicate;

/**
 * Reactive search/filter state for the recipe dashboard.
 *
 * <p><b>Properties as the API:</b> the controller binds UI nodes (text field,
 * combo boxes, star strip) directly to {@link #titleQueryProperty()},
 * {@link #selectedCategoryProperty()}, {@link #selectedDifficultyProperty()},
 * {@link #selectedAuthorProperty()}, {@link #selectedIngredients()}, and
 * {@link #ratingTierProperty()}. Every property mutation feeds a
 * 300 ms debounced recompute that updates {@link #filteredRecipes()} —
 * the controller never has to wire up its own {@code PauseTransition}
 * or manually call an {@code updateFilters()} method.
 *
 * <p><b>NFD normalisation</b> is applied to the title query and to each
 * recipe title, via {@link AutocompleteHelper#normalize(String)} — the same
 * normaliser the autocomplete dropdowns use. That makes "Huëvo" and "huevo"
 * match identically.
 *
 * <p>The class is deliberately UI-node-free — it imports {@code javafx.beans},
 * {@code javafx.collections}, {@code javafx.animation} (for the debounce),
 * but no {@code javafx.scene.*}.
 */
public final class RecipeSearchViewModel {

    /** Debounce window for property-driven recomputes. Matches the dashboard's prior cadence. */
    private static final Duration DEBOUNCE = Duration.millis(300);

    // ── Filter inputs ────────────────────────────────────────────────────────

    private final StringProperty             titleQuery          = new SimpleStringProperty("");
    private final ObjectProperty<Category>   selectedCategory    = new SimpleObjectProperty<>();
    private final ObjectProperty<Difficulty> selectedDifficulty  = new SimpleObjectProperty<>();
    private final ObjectProperty<String>     selectedAuthor      = new SimpleObjectProperty<>();
    private final ObservableList<IngredientResponse> selectedIngredients = FXCollections.observableArrayList();
    private final IntegerProperty            ratingTier          = new SimpleIntegerProperty(0);

    // ── Master + filtered lists ──────────────────────────────────────────────

    private final ObservableList<RecipeSummaryResponse> allRecipes      = FXCollections.observableArrayList();
    private final FilteredList<RecipeSummaryResponse>   filteredRecipes = new FilteredList<>(allRecipes, r -> true);

    // ── Debounce orchestration ───────────────────────────────────────────────

    private final PauseTransition debounce = new PauseTransition(DEBOUNCE);

    public RecipeSearchViewModel() {
        debounce.setOnFinished(e -> recomputePredicate());
        // Every filter input scheduled the same debounced recompute. playFromStart()
        // resets the timer on each successive change, so a quick burst of edits
        // (typing "rice") produces a single recompute 300 ms after the last keystroke.
        titleQuery.addListener((obs, o, n)         -> debounce.playFromStart());
        selectedCategory.addListener((obs, o, n)   -> debounce.playFromStart());
        selectedDifficulty.addListener((obs, o, n) -> debounce.playFromStart());
        selectedAuthor.addListener((obs, o, n)     -> debounce.playFromStart());
        ratingTier.addListener((obs, o, n)         -> debounce.playFromStart());
        selectedIngredients.addListener(
                (ListChangeListener<? super IngredientResponse>) c -> debounce.playFromStart());
    }

    // ── Property accessors (binding hooks) ───────────────────────────────────

    public StringProperty             titleQueryProperty()         { return titleQuery; }
    public ObjectProperty<Category>   selectedCategoryProperty()   { return selectedCategory; }
    public ObjectProperty<Difficulty> selectedDifficultyProperty() { return selectedDifficulty; }
    public ObjectProperty<String>     selectedAuthorProperty()     { return selectedAuthor; }
    public ObservableList<IngredientResponse> selectedIngredients(){ return selectedIngredients; }
    public IntegerProperty            ratingTierProperty()         { return ratingTier; }

    public ObservableList<RecipeSummaryResponse> allRecipes()      { return allRecipes; }
    public FilteredList<RecipeSummaryResponse>   filteredRecipes() { return filteredRecipes; }

    // ── External operations ──────────────────────────────────────────────────

    /**
     * Resets every filter property to its empty state and applies the resulting
     * (always-true) predicate immediately, bypassing the debounce delay. Calling
     * this from a UI handler ("Clear" button, Esc cascade) gives the user instant
     * feedback rather than the 300 ms wait.
     */
    public void clearFilters() {
        debounce.stop();
        titleQuery.set("");
        selectedCategory.set(null);
        selectedDifficulty.set(null);
        selectedAuthor.set(null);
        selectedIngredients.clear();
        ratingTier.set(0);
        recomputePredicate();
    }

    /**
     * Translates the UI search DTO into a Predicate and applies it to the FilteredList.
     * Useful for callers that want to drive the filter from outside the property bindings
     * (tests, programmatic searches). Bypasses the debounce — the predicate is set
     * synchronously.
     */
    public void updateSearchPredicate(SearchCriteria criteria) {
        debounce.stop();
        filteredRecipes.setPredicate(predicateFor(criteria));
    }

    /** Removes all filtering — every master-list element becomes visible again. */
    public void clearSearch() {
        debounce.stop();
        filteredRecipes.setPredicate(r -> true);
    }

    // ── Predicate construction ───────────────────────────────────────────────

    /**
     * Pure function: builds the conjunctive predicate that matches a recipe against every
     * non-empty field of {@code criteria}. A null or fully-empty criteria yields a
     * tautology. Title matching uses {@link AutocompleteHelper#normalize} (NFD + lowercase)
     * for case- and accent-insensitive contains semantics.
     */
    public static Predicate<RecipeSummaryResponse> predicateFor(SearchCriteria criteria) {
        if (criteria == null) return r -> true;
        return r -> {
            if (notBlank(criteria.title())
                    && !AutocompleteHelper.normalize(r.title())
                            .contains(AutocompleteHelper.normalize(criteria.title()))) return false;
            if (notBlank(criteria.categoryName())
                    && !equalsIgnoreCase(r.categoryName(), criteria.categoryName())) return false;
            if (notBlank(criteria.difficultyName())
                    && !equalsIgnoreCase(r.difficultyName(), criteria.difficultyName())) return false;
            if (notBlank(criteria.authorUsername())
                    && !equalsIgnoreCase(r.authorUsername(), criteria.authorUsername())) return false;
            if (criteria.ingredientIds() != null && !criteria.ingredientIds().isEmpty()
                    && !new HashSet<>(r.ingredientIds()).containsAll(criteria.ingredientIds())) return false;
            if (criteria.minScore() != null
                    && r.averageScore().compareTo(BigDecimal.valueOf(criteria.minScore())) < 0) return false;
            if (criteria.maxPreparationTime() != null
                    && r.prepTimeMinutes() > criteria.maxPreparationTime()) return false;
            if (criteria.minServings() != null
                    && r.servings() < criteria.minServings()) return false;
            return true;
        };
    }

    // ── Internal: property snapshot → predicate → FilteredList ───────────────

    private void recomputePredicate() {
        SearchCriteria criteria = currentCriteria();
        Predicate<RecipeSummaryResponse> base = predicateFor(criteria);
        Predicate<RecipeSummaryResponse> tier = ratingTierPredicate();
        filteredRecipes.setPredicate(base.and(tier));
    }

    private SearchCriteria currentCriteria() {
        Category   c = selectedCategory.get();
        Difficulty d = selectedDifficulty.get();
        return new SearchCriteria(
                titleQuery.get(),
                null,
                null,
                c != null ? c.name() : null,
                d != null ? d.name() : null,
                selectedIngredients.stream().map(IngredientResponse::id).toList(),
                selectedAuthor.get(),
                null);
    }

    /**
     * Star-strip tier: 0 = no filter; N = bracket {@code [N, N+1)}. Kept as a separate
     * predicate (not a SearchCriteria field) because the half-open bracket isn't
     * expressible inside {@code SearchCriteria.minScore} (which is "score &gt;= N").
     * At N=5 the upper bound is 6, naturally capturing exactly 5.0 since
     * {@code average_score} is bounded at 5.0.
     */
    private Predicate<RecipeSummaryResponse> ratingTierPredicate() {
        int tier = ratingTier.get();
        if (tier <= 0) return r -> true;
        BigDecimal lower = BigDecimal.valueOf(tier);
        BigDecimal upper = BigDecimal.valueOf(tier + 1);
        return r -> {
            BigDecimal score = r.averageScore();
            return score.compareTo(lower) >= 0 && score.compareTo(upper) < 0;
        };
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    private static boolean equalsIgnoreCase(String a, String b) {
        return a != null && a.equalsIgnoreCase(b);
    }
}
