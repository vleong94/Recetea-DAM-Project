package com.recetea.infrastructure.ui.javafx.features.recipe.controllers;

import com.recetea.core.recipe.application.ports.in.dto.IngredientResponse;
import com.recetea.core.recipe.application.ports.in.dto.RecipeSummaryResponse;
import com.recetea.core.recipe.application.ports.in.dto.SearchCriteria;
import com.recetea.core.recipe.domain.Category;
import com.recetea.core.recipe.domain.Difficulty;
import com.recetea.core.recipe.domain.vo.RecipeId;
import com.recetea.core.shared.domain.PageRequest;
import com.recetea.infrastructure.ui.javafx.features.recipe.RecipeCommandProvider;
import com.recetea.infrastructure.ui.javafx.features.recipe.RecipeQueryProvider;
import com.recetea.infrastructure.ui.javafx.features.recipe.RecipeSearchViewModel;
import com.recetea.infrastructure.ui.javafx.features.recipe.components.RecipeCardComponent;
import com.recetea.infrastructure.ui.javafx.shared.components.UserHeaderController;
import com.recetea.infrastructure.ui.javafx.shared.notification.NotificationService;
import com.recetea.infrastructure.ui.javafx.shared.viewstate.ViewState;
import com.recetea.infrastructure.ui.javafx.utils.AutocompleteHelper;
import com.recetea.core.shared.application.ports.in.INavigationPort;
import javafx.animation.RotateTransition;
import javafx.animation.ScaleTransition;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.Region;
import javafx.scene.shape.SVGPath;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;

/**
 * Controller for the main dashboard — a paged recipe gallery with
 * client-side search, filter drawer, and rating-tier strip.
 *
 * <p><b>Bulk-load on mount.</b> The recipe set is loaded once via
 * {@code IGetAllRecipesUseCase.execute(new PageRequest(0, 200))} into
 * the {@link RecipeSearchViewModel}'s master list; subsequent filter
 * changes recompute the {@code FilteredList} predicate locally without
 * round-tripping the DB. The 200-row cap (see {@code MAX_RECIPES_LOADED})
 * is the soft ceiling — past that point server-side pagination would
 * become necessary.
 *
 * <p><b>ViewModel as the search API.</b> Every filter input
 * (text field, combo boxes, rating strip, ingredient chips) is
 * bidirectionally bound to a property on {@link RecipeSearchViewModel},
 * which owns the 300 ms debounce internally. The controller never
 * touches a {@code PauseTransition} — it just wires bindings.
 *
 * <p><b>ViewState rendering.</b> The gallery surface is driven through
 * {@link ViewState} with a pattern-matching switch — Loading paints
 * skeleton cards, Empty shows the placeholder, Error fires an inline
 * toast, Success populates the {@code FlowPane}. A {@code loaded}
 * latch suppresses the Empty flicker that would otherwise occur when
 * the ViewModel's {@code FilteredList} is empty during the initial
 * in-flight load.
 *
 * <p><b>ES — </b>Controlador del dashboard principal — una galería
 * de recetas paginada con búsqueda en cliente, cajón de filtros y
 * tira de niveles de valoración.
 *
 * <p><b>Carga masiva al montarlo.</b> El conjunto de recetas se
 * carga una vez vía
 * {@code IGetAllRecipesUseCase.execute(new PageRequest(0, 200))} a
 * la lista maestra del {@link RecipeSearchViewModel}; los cambios
 * de filtro posteriores recalculan el predicado del
 * {@code FilteredList} en local sin volver a la BD. El tope de 200
 * filas (ver {@code MAX_RECIPES_LOADED}) es el techo blando — más
 * allá se haría necesaria la paginación en servidor.
 *
 * <p><b>ViewModel como API de búsqueda.</b> Cada entrada de filtro
 * (campo de texto, ComboBox, tira de valoración, chips de
 * ingredientes) se enlaza bidireccionalmente a una propiedad del
 * {@link RecipeSearchViewModel}, que encapsula internamente el
 * debounce de 300 ms. El controlador no toca nunca una
 * {@code PauseTransition} — sólo cablea bindings.
 *
 * <p><b>Renderizado con ViewState.</b> La galería se controla a
 * través de {@link ViewState} con un switch pattern-matching —
 * Loading pinta tarjetas esqueleto, Empty muestra el placeholder,
 * Error dispara un toast inline, Success rellena el
 * {@code FlowPane}. Un latch {@code loaded} suprime el parpadeo de
 * Empty que ocurriría cuando el {@code FilteredList} del
 * ViewModel esté vacío durante la carga inicial en vuelo.
 */
public class RecipeDashboardController {

    private static final int MAX_RECIPES_LOADED = 200;

    // ── Search & filter controls ─────────────────────────────────────────────
    @FXML private TextField                       searchField;
    @FXML private Button                          clearSearchButton;
    @FXML private ToggleButton                    filterToggle;
    @FXML private VBox                            filterPanel;
    @FXML private FlowPane                        ingredientChipsContainer;
    @FXML private ComboBox<Category>              categoryFilter;
    @FXML private ComboBox<Difficulty>            difficultyFilter;
    @FXML private ComboBox<IngredientResponse>    ingredientFilter;
    @FXML private ComboBox<String>                authorFilter;
    @FXML private HBox                            ratingStarContainer;

    // ── Card gallery + empty state ───────────────────────────────────────────
    @FXML private FlowPane recipeContainer;
    @FXML private VBox     emptyStateView;

    // ── Shared header (injected via fx:include suffix convention) ────────────
    @FXML private UserHeaderController userHeaderController;

    private RecipeQueryProvider   queryProvider;
    private RecipeCommandProvider commandProvider;
    private INavigationPort       nav;
    private ExecutorService       executor;

    // Reactive filtering pipeline. Every filter dimension lives as a Property on the
    // ViewModel; UI nodes bind bidirectionally to those properties, the ViewModel runs
    // its own 300 ms debounced recompute, and this controller observes only the resulting
    // FilteredList for rendering.
    private final RecipeSearchViewModel searchViewModel = new RecipeSearchViewModel();

    // Editable-combo type-ahead: each combo's items live inside a FilteredList so the
    // editor's textProperty can narrow the dropdown without touching the source list.
    private final ObservableList<Category>           allCategories       = FXCollections.observableArrayList();
    private final ObservableList<Difficulty>         allDifficulties     = FXCollections.observableArrayList();
    private final ObservableList<IngredientResponse> allIngredients      = FXCollections.observableArrayList();
    private final ObservableList<String>             allAuthors          = FXCollections.observableArrayList();
    private final FilteredList<Category>             categoryFiltered    = new FilteredList<>(allCategories, c -> true);
    private final FilteredList<IngredientResponse>   ingredientFiltered  = new FilteredList<>(allIngredients, i -> true);
    private final FilteredList<String>               authorFiltered      = new FilteredList<>(allAuthors, a -> true);

    /** Mutable mirror of the user's favorited recipes; populated on load, mutated on toggle. */
    private final Set<RecipeId> currentFavorites = new HashSet<>();

    /** SVG nodes for the 5-star rating strip. The active tier itself lives on the ViewModel. */
    private final SVGPath[] starNodes = new SVGPath[5];

    /** Number of skeleton placeholder cards rendered during the {@link ViewState.Loading} state. */
    private static final int SKELETON_CARD_COUNT = 6;
    private static final double SKELETON_CARD_WIDTH  = 280;
    private static final double SKELETON_CARD_HEIGHT = 320;

    /**
     * Latch on the FilteredList listener — flips to {@code true} once {@link #loadAllRecipes}
     * resolves. Without it, every list mutation that fires before the load completes (e.g.
     * the {@code allRecipes.setAll(...)} on success) would render an Empty state on top of
     * the in-flight Loading skeleton.
     */
    private boolean loaded = false;

    /**
     * Storage root threaded down from the composition root via NavigationService —
     * decouples this controller from the deleted {@code StorageConfig} singleton.
     */
    private String storageBasePath;

    @FXML
    public void initialize() {
        // ── Bidirectional UI ↔ ViewModel bindings ────────────────────────────
        // Each filter widget's value property is now wired straight to the matching
        // ViewModel property. Mutations flow both ways: typing in the search field
        // updates titleQuery; clearFilters() resetting titleQuery clears the field.
        searchField.textProperty().bindBidirectional(searchViewModel.titleQueryProperty());
        categoryFilter.valueProperty().bindBidirectional(searchViewModel.selectedCategoryProperty());
        difficultyFilter.valueProperty().bindBidirectional(searchViewModel.selectedDifficultyProperty());
        authorFilter.valueProperty().bindBidirectional(searchViewModel.selectedAuthorProperty());

        wireSearchableCombo(categoryFilter, allCategories, categoryFiltered, Category::name,      AutocompleteHelper.SearchConfig.instant());
        wireSearchableCombo(authorFilter,   allAuthors,    authorFiltered,   Function.identity(), AutocompleteHelper.SearchConfig.topN(20));
        wireIngredientCombo();

        // Difficulty: simple non-editable selector. Catalogue is fixed at 4 values, so
        // type-ahead via JavaFX's built-in keyboard search is enough; no FilteredList,
        // no StringConverter, no danger-state plumbing required. Difficulty.toString()
        // already returns name, so default cell rendering shows the right text.
        difficultyFilter.setItems(allDifficulties);

        buildRatingStars();
        // Repaint the strip whenever the tier changes — covers both user clicks and
        // programmatic resets via ViewModel.clearFilters().
        searchViewModel.ratingTierProperty().addListener((obs, o, n) -> refreshStarStyles());

        // Chip strip rebuilds on every change to the ViewModel's ingredient list.
        // No explicit filter trigger here — the ViewModel's own listener on the same
        // list schedules the debounced recompute.
        searchViewModel.selectedIngredients().addListener(
                (ListChangeListener<? super IngredientResponse>) c -> renderChips());

        clearSearchButton.visibleProperty().bind(searchField.textProperty().isNotEmpty());
        clearSearchButton.managedProperty().bind(clearSearchButton.visibleProperty());

        filterPanel.visibleProperty().bind(filterToggle.selectedProperty());
        filterPanel.managedProperty().bind(filterToggle.selectedProperty());

        installFilterToggleChevron();

        // Chip strip collapses out of layout when no ingredients are selected.
        ingredientChipsContainer.visibleProperty()
                .bind(Bindings.isNotEmpty(searchViewModel.selectedIngredients()));
        ingredientChipsContainer.managedProperty().bind(ingredientChipsContainer.visibleProperty());

        // Visibility of emptyStateView and recipeContainer is now driven explicitly by
        // render(ViewState) — no Bindings.isEmpty wiring here, because the binding would
        // fight the explicit setVisible/setManaged calls inside the switch arms.

        // Once the initial load resolves, every subsequent FilteredList change recomputes
        // the surface state. Pre-load mutations (allRecipes.setAll on success) are
        // intentionally swallowed so the Loading skeleton stays on screen until
        // loadAllRecipes finishes.
        searchViewModel.filteredRecipes()
                .addListener((ListChangeListener<? super RecipeSummaryResponse>) c -> {
                    if (!loaded) return;
                    var snapshot = List.copyOf(searchViewModel.filteredRecipes());
                    render(snapshot.isEmpty() ? ViewState.empty() : ViewState.success(snapshot));
                });

        // Initial mount = nothing loaded yet.
        render(ViewState.loading());
    }

    /**
     * Builds 5 clickable {@link SVGPath} stars in {@link #ratingStarContainer} and wires
     * each to the toggle-on-click / toggle-off-on-same semantics. The first star at index 0
     * represents rating tier 1.
     */
    private void buildRatingStars() {
        ratingStarContainer.getChildren().clear();
        // Material Icons "star" path, 24x24 viewbox.
        final String starPath = "M12 17.27L18.18 21l-1.64-7.03L22 9.24l-7.19-.61L12 2 9.81 8.63 2 9.24l5.46 4.73L5.82 21z";
        for (int i = 0; i < 5; i++) {
            var star = new SVGPath();
            star.setContent(starPath);
            starNodes[i] = star;
            final int rank = i + 1;
            star.setOnMouseClicked(e -> onStarClick(rank));
            ratingStarContainer.getChildren().add(star);
        }
        refreshStarStyles();
    }

    /**
     * Click semantics:
     * <ul>
     *   <li>Click the same rank that's currently active → reset to 0 (no filter).</li>
     *   <li>Click any other rank N → tier becomes N.</li>
     * </ul>
     * Mutates the ViewModel's tier property; the property listener handles repaint and
     * the ViewModel's debounce schedules the predicate recompute.
     */
    private void onStarClick(int rank) {
        int current = searchViewModel.ratingTierProperty().get();
        searchViewModel.ratingTierProperty().set(rank == current ? 0 : rank);
    }

    /** Re-applies fill colours: gold for stars 1..ratingTier, muted grey for the rest. */
    private void refreshStarStyles() {
        int tier = searchViewModel.ratingTierProperty().get();
        for (int i = 0; i < starNodes.length; i++) {
            boolean active = (i + 1) <= tier;
            starNodes[i].setStyle(
                    "-fx-fill: " + (active ? "app-star-active" : "app-star-empty") + ";"
                  + " -fx-cursor: hand;");
        }
    }

    /**
     * Thin facade over {@link AutocompleteHelper#wire} that adds the dashboard's controller-specific
     * concern: every successful pick needs to clear the no-match red border. Predicate updates
     * are now driven by the bidirectional binding on {@code combo.valueProperty()} → ViewModel
     * property → debounced recompute, so this method no longer manually triggers filtering.
     */
    private <T> void wireSearchableCombo(ComboBox<T> combo, ObservableList<T> source,
                                         FilteredList<T> filtered, Function<T, String> displayText,
                                         AutocompleteHelper.SearchConfig config) {
        AutocompleteHelper.wire(combo, source, filtered, displayText, config);
        combo.valueProperty().addListener((obs, o, n) -> {
            if (n != null) AutocompleteHelper.clearDangerState(combo);
        });
    }

    /**
     * Mounts a chevron SVG as the toggle's graphic. The path is fixed (chevron-down);
     * a {@link RotateTransition} animates it ±180° on selectedProperty changes, so the
     * arrow visually flips up when the panel expands and back down when it collapses.
     * Fill stays at {@code -color-fg-default} in every state — per the spec, the toggle
     * only changes via the arrow direction, never via colour.
     */
    private void installFilterToggleChevron() {
        var chevron = new SVGPath();
        chevron.setContent("M7 10l5 5 5-5z"); // chevron-down (rest position)
        chevron.setStyle("-fx-fill: -color-fg-default;");

        var rotate = new RotateTransition(Duration.millis(180), chevron);
        filterToggle.selectedProperty().addListener((obs, was, isNow) -> {
            rotate.stop();
            rotate.setToAngle(isNow ? 180 : 0);
            rotate.play();
        });

        filterToggle.setGraphic(chevron);
    }

    /**
     * Ingredient combo follows the chip pattern — picking an item appends it to the
     * ViewModel's {@code selectedIngredients} list (which the UI's chip strip mirrors)
     * and clears the combo so the next pick fires even if it's the same ingredient. The
     * ViewModel's own list listener handles the debounced predicate refresh.
     */
    private void wireIngredientCombo() {
        AutocompleteHelper.wire(ingredientFilter, allIngredients, ingredientFiltered,
                IngredientResponse::name, AutocompleteHelper.SearchConfig.defaults());
        ingredientFilter.valueProperty().addListener((obs, o, picked) -> {
            // safeGet against the value the listener fired on, in case focus-loss racing
            // ever delivers a non-IngredientResponse here.
            if (!(picked instanceof IngredientResponse ing)) return;
            var ingredients = searchViewModel.selectedIngredients();
            if (!ingredients.contains(ing)) ingredients.add(ing);
            AutocompleteHelper.clearDangerState(ingredientFilter);
            // Clear immediately so the next selection event fires even if the user
            // re-picks the same item later (after removing its chip). Explicit hide()
            // collapses the dropdown without waiting for the editor.clear() text-change
            // to propagate through the autocomplete debounce.
            javafx.application.Platform.runLater(() -> {
                ingredientFilter.getSelectionModel().clearSelection();
                ingredientFilter.getEditor().clear();
                ingredientFilter.hide();
            });
        });
    }

    public void init(RecipeQueryProvider queryProvider, RecipeCommandProvider commandProvider,
                     INavigationPort nav, ExecutorService executor, String storageBasePath) {
        this.queryProvider   = queryProvider;
        this.commandProvider = commandProvider;
        this.nav             = nav;
        this.executor        = executor;
        this.storageBasePath = storageBasePath;

        allCategories.setAll(commandProvider.getAllCategories().execute());
        allDifficulties.setAll(commandProvider.getAllDifficulties().execute());
        allIngredients.setAll(commandProvider.getAllIngredients().execute());

        userHeaderController.init(commandProvider.sessionService(), nav, UserHeaderController.Mode.DASHBOARD);

        loadAllRecipes();
    }

    // ── Server-side load (once per dashboard mount) ──────────────────────────

    private void loadAllRecipes() {
        if (executor == null) return;
        render(ViewState.loading());
        var emptyCriteria = new SearchCriteria(null, null, null, null, null, List.of(), null, null);
        var task = new Task<List<RecipeSummaryResponse>>() {
            @Override protected List<RecipeSummaryResponse> call() {
                return queryProvider.searchRecipes()
                        .execute(emptyCriteria, new PageRequest(0, MAX_RECIPES_LOADED)).content();
            }
        };
        task.setOnSucceeded(e -> {
            searchViewModel.allRecipes().setAll(task.getValue());
            allAuthors.setAll(task.getValue().stream()
                    .map(RecipeSummaryResponse::authorUsername)
                    .filter(s -> s != null && !s.isBlank())
                    .distinct()
                    .sorted()
                    .toList());
            currentFavorites.clear();
            currentFavorites.addAll(queryProvider.getUserFavorites().execute());
            // Open the listener gate AFTER the bulk allRecipes.setAll has fired, so the
            // initial render reflects the loaded set rather than producing two transitions
            // (Empty during the in-progress setAll, then Success on completion).
            loaded = true;
            var snapshot = List.copyOf(searchViewModel.filteredRecipes());
            render(snapshot.isEmpty() ? ViewState.empty() : ViewState.success(snapshot));
        });
        task.setOnFailed(e -> {
            loaded = true; // open the gate so future filter changes still re-render
            Throwable cause = task.getException();
            String message = cause != null && cause.getMessage() != null
                    ? cause.getMessage()
                    : "Recipe load failed";
            render(ViewState.error(message));
            Thread.getDefaultUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), cause);
        });
        executor.execute(task);
    }

    /**
     * Card-level favorite toggle: write to DB on a worker thread, mirror the change in the
     * in-memory {@link #currentFavorites} set on FX thread so subsequent re-renders pick up
     * the new state without re-fetching.
     */
    private void onCardFavoriteToggle(RecipeId recipeId) {
        if (executor == null) return;
        var task = new Task<Void>() {
            @Override protected Void call() {
                commandProvider.toggleFavorite().execute(recipeId);
                return null;
            }
        };
        task.setOnSucceeded(e -> {
            if (currentFavorites.contains(recipeId)) currentFavorites.remove(recipeId);
            else                                     currentFavorites.add(recipeId);
        });
        task.setOnFailed(e -> Thread.getDefaultUncaughtExceptionHandler()
                .uncaughtException(Thread.currentThread(), task.getException()));
        executor.execute(task);
    }

    // ── Rendering ────────────────────────────────────────────────────────────

    /**
     * Single declarative entry point for every UI surface change. Each {@link ViewState}
     * arm sets the visibility/managed state of {@link #recipeContainer} and
     * {@link #emptyStateView} explicitly — there's no scattered {@code setVisible}
     * elsewhere in the controller, and no {@code Bindings.isEmpty} wiring fights for
     * control of the empty placeholder.
     */
    private void render(ViewState<RecipeSummaryResponse> state) {
        switch (state) {
            case ViewState.Loading<RecipeSummaryResponse> _ -> {
                paintSkeleton();
                setShown(emptyStateView, false);
                setShown(recipeContainer, true);
            }
            case ViewState.Empty<RecipeSummaryResponse> _ -> {
                recipeContainer.getChildren().clear();
                setShown(recipeContainer, false);
                setShown(emptyStateView, true);
            }
            case ViewState.Error<RecipeSummaryResponse>(var message) -> {
                recipeContainer.getChildren().clear();
                setShown(recipeContainer, false);
                setShown(emptyStateView, true);
                NotificationService.error(emptyStateView, message);
            }
            case ViewState.Success<RecipeSummaryResponse>(var data) -> {
                renderCards(data);
                setShown(emptyStateView, false);
                setShown(recipeContainer, true);
            }
        }
    }

    private void renderCards(List<RecipeSummaryResponse> data) {
        recipeContainer.getChildren().clear();
        for (var recipe : data) {
            recipeContainer.getChildren().add(
                    new RecipeCardComponent(recipe,
                            id -> nav.toRecipeDetail(id),
                            storageBasePath,
                            currentFavorites.contains(recipe.id()),
                            this::onCardFavoriteToggle));
        }
    }

    /**
     * Fills {@link #recipeContainer} with skeleton placeholder rectangles. Each carries
     * both {@code recipe-card} and {@code skeleton-pulse} style classes so the existing
     * {@code .skeleton-pulse} animation in app.css drives the breathing effect on a
     * card-shaped silhouette while the real data is in flight.
     */
    private void paintSkeleton() {
        recipeContainer.getChildren().clear();
        for (int i = 0; i < SKELETON_CARD_COUNT; i++) {
            var skeleton = new Region();
            skeleton.getStyleClass().addAll("recipe-card", "skeleton-pulse");
            skeleton.setPrefSize(SKELETON_CARD_WIDTH, SKELETON_CARD_HEIGHT);
            recipeContainer.getChildren().add(skeleton);
        }
    }

    private static void setShown(Node node, boolean shown) {
        node.setVisible(shown);
        node.setManaged(shown);
    }

    private void renderChips() {
        ingredientChipsContainer.getChildren().clear();
        for (var ingredient : searchViewModel.selectedIngredients()) {
            ingredientChipsContainer.getChildren().add(createChip(ingredient));
        }
    }

    /**
     * Removable tag for one selected ingredient. The X button hosts an {@link SVGPath} cross
     * with two-state styling: muted grey at rest, danger emphasis on hover. The hover scales
     * the icon 1.0 → 1.2 over 150 ms via a reusable {@link ScaleTransition} — single instance
     * per chip, restarted on each enter/exit, so rapid in/out reads from the icon's current
     * scale and doesn't snap.
     */
    private Node createChip(IngredientResponse ingredient) {
        var nameLabel = new Label(ingredient.name());

        var icon = new SVGPath();
        icon.setContent("M 0 0 L 10 10 M 10 0 L 0 10");
        var restStyle  = "-fx-fill: null; -fx-stroke: -color-fg-muted; -fx-stroke-width: 1.5;";
        var hoverStyle = "-fx-fill: null; -fx-stroke: -color-danger-emphasis; -fx-stroke-width: 1.5;";
        icon.setStyle(restStyle);

        var removeBtn = new Button();
        removeBtn.setGraphic(icon);
        removeBtn.getStyleClass().addAll("button-circle", "flat", "small");
        removeBtn.setOnAction(e -> searchViewModel.selectedIngredients().remove(ingredient));

        // Single ScaleTransition reused for both directions. Without setFromX/Y, each play()
        // animates from the icon's current scale to the new target — so an exit fired mid-grow
        // smoothly continues from 1.1 to 1.0 instead of jumping back to 1.2 first.
        var scale = new ScaleTransition(Duration.millis(150), icon);

        removeBtn.setOnMouseEntered(e -> {
            icon.setStyle(hoverStyle);
            scale.stop();
            scale.setToX(1.2);
            scale.setToY(1.2);
            scale.play();
        });
        removeBtn.setOnMouseExited(e -> {
            icon.setStyle(restStyle);
            scale.stop();
            scale.setToX(1.0);
            scale.setToY(1.0);
            scale.play();
        });

        var chip = new HBox(5, nameLabel, removeBtn);
        chip.getStyleClass().add("ingredient-chip");
        chip.setAlignment(Pos.CENTER_LEFT);
        return chip;
    }

    // ── Public API for NavigationService ─────────────────────────────────────

    public void focusSearch() {
        searchField.requestFocus();
    }

    /**
     * Ctrl+F handler: toggles the advanced filter panel. When the panel newly opens,
     * focus jumps to the search field so the user can start typing without an extra click.
     */
    public void onCtrlF() {
        boolean willOpen = !filterToggle.isSelected();
        filterToggle.setSelected(willOpen);
        if (willOpen) searchField.requestFocus();
    }

    /**
     * Esc handler: progressive reset cascade. Each press peels back one layer of state
     * so the user always knows what the next press will do.
     * <ol>
     *   <li>If the search field has text → clear just that.</li>
     *   <li>Else if the filter panel is open → close just the panel.</li>
     *   <li>Else → full clear (delegates to {@link #onClearSearch()}).</li>
     * </ol>
     */
    public void onEscape() {
        String text = searchField.getText();
        if (text != null && !text.isEmpty()) {
            searchField.clear();
        } else if (filterToggle.isSelected()) {
            filterToggle.setSelected(false);
        } else {
            onClearSearch();
        }
    }

    // ── FXML handlers ────────────────────────────────────────────────────────

    @FXML
    public void onClearSearchText() {
        searchField.clear();
    }

    /**
     * "Clear filters" button: delegates the full reset to the ViewModel's
     * {@link RecipeSearchViewModel#clearFilters()} (which zeroes every property and
     * recomputes immediately, bypassing the debounce). The bidirectional bindings carry
     * the property resets back into the UI nodes; only the per-combo editor / danger
     * state needs an explicit nudge here, since those aren't driven by valueProperty.
     *
     * <p>Intentionally NOT touching {@code filterToggle} — the panel stays open and the
     * chevron stays rotated. Limpiar resets data only; toggle state is a UX preference
     * set by the user (Ctrl+F or click), not part of the filter state.
     */
    @FXML
    public void onClearSearch() {
        searchViewModel.clearFilters();
        clearComboEditor(categoryFilter);
        clearComboEditor(difficultyFilter);
        clearComboEditor(authorFilter);
        clearComboEditor(ingredientFilter);
    }

    /** Wipes a combo's editor text + danger state. Value is reset by the bidirectional binding. */
    private static <T> void clearComboEditor(ComboBox<T> combo) {
        if (combo.isEditable()) combo.getEditor().clear();
        AutocompleteHelper.clearDangerState(combo);
    }

    @FXML public void onCreateButtonClick() { nav.toRecipeCreate(); }
}
