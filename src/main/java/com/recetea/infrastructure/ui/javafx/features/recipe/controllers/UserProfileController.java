package com.recetea.infrastructure.ui.javafx.features.recipe.controllers;

import com.recetea.core.recipe.application.ports.in.dto.RecipeSummaryResponse;
import com.recetea.core.recipe.domain.vo.RecipeId;
import com.recetea.core.shared.domain.PageRequest;
import com.recetea.core.user.domain.UserId;
import com.recetea.infrastructure.ui.javafx.features.recipe.RecipeCommandProvider;
import com.recetea.infrastructure.ui.javafx.features.recipe.RecipeQueryProvider;
import com.recetea.infrastructure.ui.javafx.features.recipe.components.RecipeCardComponent;
import com.recetea.infrastructure.ui.javafx.shared.components.UserHeaderController;
import com.recetea.infrastructure.ui.javafx.shared.i18n.I18n;
import com.recetea.core.shared.application.ports.in.INavigationPort;
import com.recetea.infrastructure.ui.javafx.shared.notification.NotificationService;
import com.recetea.infrastructure.ui.javafx.shared.viewstate.ViewState;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.TabPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;

/**
 * Controller for the user-profile page — two tabs, "my creations" and
 * "favorites", each rendered as a card gallery via
 * {@link RecipeCardComponent}.
 *
 * <p><b>Two distinct hydration paths.</b>
 * <ul>
 *   <li><b>Creations</b>: paged {@code IGetRecipesByAuthorUseCase} call
 *       scoped to the current session user. SQL-level filter — never
 *       returns rows that don't belong to the user.</li>
 *   <li><b>Favorites</b>: two-step. {@code IGetUserFavoritesUseCase}
 *       returns the favorited {@link RecipeId}s in
 *       most-recently-favorited-first order; then
 *       {@code IGetRecipeSummariesByIdsUseCase} hydrates the summaries.
 *       The controller reorders the result to match the input id list
 *       because the SQL row order is unspecified.</li>
 * </ul>
 *
 * <p><b>Skeleton loader.</b> Both tabs paint
 * {@link #SKELETON_CARD_COUNT} placeholder cards (matching
 * {@link RecipeCardComponent}'s shape via the {@code skeleton-pulse}
 * style class) while the in-flight {@link Task} resolves. The rendered
 * skeletons are sized to {@link #SKELETON_CARD_WIDTH} ×
 * {@link #SKELETON_CARD_HEIGHT} so the layout doesn't shift when real
 * cards replace them.
 *
 * <p><b>ES — </b>Controlador para la página de perfil de usuario —
 * dos pestañas, "mis creaciones" y "favoritos", cada una
 * renderizada como una galería de tarjetas vía
 * {@link RecipeCardComponent}.
 *
 * <p><b>Dos rutas de hidratación distintas.</b>
 * <ul>
 *   <li><b>Creaciones</b>: llamada paginada a
 *       {@code IGetRecipesByAuthorUseCase} acotada al usuario
 *       actual de sesión. Filtro a nivel SQL — nunca devuelve
 *       filas que no pertenezcan al usuario.</li>
 *   <li><b>Favoritos</b>: en dos pasos.
 *       {@code IGetUserFavoritesUseCase} devuelve los
 *       {@link RecipeId} favoritados en orden de más recientes
 *       primero; luego {@code IGetRecipeSummariesByIdsUseCase}
 *       hidrata los resúmenes. El controlador reordena el
 *       resultado para que cuadre con la lista de ids de entrada,
 *       porque el orden de filas SQL no está especificado.</li>
 * </ul>
 *
 * <p><b>Loader de esqueleto.</b> Ambas pestañas pintan
 * {@link #SKELETON_CARD_COUNT} tarjetas placeholder (con la forma
 * de {@link RecipeCardComponent} vía la clase
 * {@code skeleton-pulse}) mientras se resuelve la {@link Task} en
 * vuelo. Los esqueletos renderizados se dimensionan a
 * {@link #SKELETON_CARD_WIDTH} × {@link #SKELETON_CARD_HEIGHT}
 * para que el layout no se desplace cuando las tarjetas reales
 * los reemplacen.
 */
public class UserProfileController {

    private static final int SKELETON_CARD_COUNT = 4;
    private static final double SKELETON_CARD_WIDTH  = 280;
    private static final double SKELETON_CARD_HEIGHT = 320;

    @FXML private TabPane  workspaceTabs;
    @FXML private VBox     creationsEmpty;
    @FXML private FlowPane creationsContainer;

    @FXML private VBox     favoritesEmpty;
    @FXML private FlowPane favoritesContainer;

    @FXML private Button   btnImportXml;
    @FXML private Button   btnImportXmlFav;

    // Shared header (injected via fx:include suffix convention).
    @FXML private UserHeaderController userHeaderController;

    private RecipeQueryProvider   queryProvider;
    private RecipeCommandProvider commandProvider;
    private INavigationPort       nav;
    private ExecutorService       executor;

    /** Storage root for recipe-card image URLs; injected via {@link #init}. */
    private String storageBasePath;

    /** Mutable mirror of the user's favorited recipes; powers the per-card star initial state. */
    private final Set<RecipeId> currentFavorites = new HashSet<>();

    /**
     * Two physical import buttons swap based on which tab is active so the import slot
     * stays at a static screen position (no shift, no resize) across tab switches.
     * managedProperty mirrors visibleProperty so the hidden one collapses out of layout
     * instead of leaving a gap. Both share {@link #onImportButtonClick} as their handler.
     */
    @FXML
    public void initialize() {
        ReadOnlyIntegerProperty selectedIndex = workspaceTabs.getSelectionModel().selectedIndexProperty();

        btnImportXml.visibleProperty().bind(selectedIndex.isEqualTo(0));
        btnImportXml.managedProperty().bind(btnImportXml.visibleProperty());

        btnImportXmlFav.visibleProperty().bind(selectedIndex.isEqualTo(1));
        btnImportXmlFav.managedProperty().bind(btnImportXmlFav.visibleProperty());
    }

    public void init(RecipeQueryProvider queryProvider, RecipeCommandProvider commandProvider,
                     INavigationPort nav, ExecutorService executor, String storageBasePath) {
        this.queryProvider   = queryProvider;
        this.commandProvider = commandProvider;
        this.nav             = nav;
        this.executor        = executor;
        this.storageBasePath = storageBasePath;

        userHeaderController.init(commandProvider.sessionService(), nav, UserHeaderController.Mode.WORKSPACE);

        loadAll();
    }

    // ── Data loading ──────────────────────────────────────────────────────────

    private void loadAll() {
        UserId currentUser = commandProvider.sessionService().getCurrentUserId().orElse(null);
        if (currentUser == null || executor == null) return;

        // Show skeletons in both panes while the fetch resolves.
        render(creationsContainer, creationsEmpty, ViewState.loading());
        render(favoritesContainer, favoritesEmpty, ViewState.loading());

        Task<ProfileData> task = new Task<>() {
            @Override
            protected ProfileData call() {
                List<RecipeSummaryResponse> creations =
                        queryProvider.getRecipesByAuthor().execute(currentUser, new PageRequest(0, 200)).content();
                List<RecipeId> favoriteIds = queryProvider.getUserFavorites().execute();
                List<RecipeSummaryResponse> favorites =
                        queryProvider.getRecipeSummariesByIds().execute(favoriteIds);
                return new ProfileData(creations, favorites);
            }
        };
        task.setOnSucceeded(e -> {
            ProfileData data = task.getValue();
            currentFavorites.clear();
            currentFavorites.addAll(queryProvider.getUserFavorites().execute());
            render(creationsContainer, creationsEmpty, asState(data.creations()));
            render(favoritesContainer, favoritesEmpty, asState(data.favorites()));
        });
        task.setOnFailed(e -> {
            Throwable cause = task.getException();
            String message = cause != null && cause.getMessage() != null
                    ? cause.getMessage()
                    : "Profile load failed";
            render(creationsContainer, creationsEmpty, ViewState.error(message));
            render(favoritesContainer, favoritesEmpty, ViewState.error(message));
            Thread.getDefaultUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), cause);
        });
        executor.execute(task);
    }

    /**
     * Single declarative entry point for one tab's gallery surface. Each {@link ViewState}
     * arm fully controls the visibility of {@code container} and {@code emptyPlaceholder}
     * — no scattered {@code setVisible} elsewhere in the controller, and the same switch
     * shape used by the dashboard so the two surfaces evolve together.
     */
    private void render(FlowPane container, VBox emptyPlaceholder, ViewState<RecipeSummaryResponse> state) {
        switch (state) {
            case ViewState.Loading<RecipeSummaryResponse> _ -> {
                paintSkeleton(container);
                setShown(emptyPlaceholder, false);
                setShown(container, true);
            }
            case ViewState.Empty<RecipeSummaryResponse> _ -> {
                container.getChildren().clear();
                setShown(container, false);
                setShown(emptyPlaceholder, true);
            }
            case ViewState.Error<RecipeSummaryResponse>(var message) -> {
                container.getChildren().clear();
                setShown(container, false);
                setShown(emptyPlaceholder, true);
                NotificationService.error(emptyPlaceholder, message);
            }
            case ViewState.Success<RecipeSummaryResponse>(var data) -> {
                renderCards(container, data);
                setShown(emptyPlaceholder, false);
                setShown(container, true);
            }
        }
    }

    private void renderCards(FlowPane container, List<RecipeSummaryResponse> items) {
        container.getChildren().clear();
        for (RecipeSummaryResponse recipe : items) {
            container.getChildren().add(
                    new RecipeCardComponent(recipe,
                            id -> nav.toRecipeDetail(id),
                            storageBasePath,
                            currentFavorites.contains(recipe.id()),
                            this::onCardFavoriteToggle));
        }
    }

    /**
     * Fills {@code container} with skeleton placeholder rectangles. Each carries
     * {@code recipe-card} + {@code skeleton-pulse} style classes so the existing CSS
     * animation drives the breathing effect on a card-shaped silhouette.
     */
    private static void paintSkeleton(FlowPane container) {
        container.getChildren().clear();
        for (int i = 0; i < SKELETON_CARD_COUNT; i++) {
            var skeleton = new Region();
            skeleton.getStyleClass().addAll("recipe-card", "skeleton-pulse");
            skeleton.setPrefSize(SKELETON_CARD_WIDTH, SKELETON_CARD_HEIGHT);
            container.getChildren().add(skeleton);
        }
    }

    /** Empty list → Empty state; non-empty → Success carrying the items. */
    private static ViewState<RecipeSummaryResponse> asState(List<RecipeSummaryResponse> items) {
        return items.isEmpty() ? ViewState.empty() : ViewState.success(items);
    }

    private static void setShown(Node node, boolean shown) {
        node.setVisible(shown);
        node.setManaged(shown);
    }

    /** Mirrors the dashboard's favorite-toggle path: write through, then mutate the local set. */
    private void onCardFavoriteToggle(RecipeId recipeId) {
        if (executor == null) return;
        Task<Void> task = new Task<>() {
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

    // ── Actions ───────────────────────────────────────────────────────────────

    @FXML
    public void onBackButtonClick() {
        nav.toDashboard();
    }

    @FXML
    public void onCreateRecipeClick() {
        nav.toRecipeCreate();
    }

    @FXML
    public void onImportButtonClick() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(I18n.get("dialog.importXml.title"));
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(I18n.get("dialog.filter.xml"), "*.xml"));
        File file = chooser.showOpenDialog(creationsContainer.getScene().getWindow());
        if (file == null) return;

        // Capture the new ID so we can favorite it when the user triggered the import
        // from the Favoritos tab — keeps the use case tab-agnostic while letting the UI
        // honour the contextual expectation that "imported here → appears here".
        RecipeId imported = commandProvider.importRecipe().execute(file);
        if (workspaceTabs.getSelectionModel().getSelectedIndex() == 1) {
            commandProvider.toggleFavorite().execute(imported);
        }
        loadAll();
        NotificationService.success(creationsContainer,
                I18n.format("dashboard.notification.recipeImported", file.getName()));
    }

    private record ProfileData(List<RecipeSummaryResponse> creations, List<RecipeSummaryResponse> favorites) {}
}
