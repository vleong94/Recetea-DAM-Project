package com.recetea.infrastructure.ui.javafx.features.recipe.controllers;

import com.recetea.core.recipe.application.ports.in.dto.RecipeSummaryResponse;
import com.recetea.core.recipe.application.ports.in.dto.SearchCriteria;
import com.recetea.core.shared.domain.PageRequest;
import com.recetea.infrastructure.storage.StorageConfig;
import com.recetea.infrastructure.ui.javafx.features.recipe.RecipeCommandProvider;
import com.recetea.infrastructure.ui.javafx.features.recipe.RecipeQueryProvider;
import com.recetea.infrastructure.ui.javafx.features.recipe.components.RecipeCardComponent;
import com.recetea.infrastructure.ui.javafx.shared.i18n.I18n;
import com.recetea.infrastructure.ui.javafx.shared.navigation.NavigationService;
import com.recetea.infrastructure.ui.javafx.shared.notification.NotificationService;
import javafx.animation.PauseTransition;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.util.Duration;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;
import java.util.concurrent.ExecutorService;

public class RecipeDashboardController {

    // ── Search & filter controls ─────────────────────────────────────────────
    @FXML private TextField  searchField;
    @FXML private TitledPane filterPane;
    @FXML private TextField  categoryFilter;
    @FXML private TextField  difficultyFilter;
    @FXML private TextField  ingredientFilter;
    @FXML private TextField  authorFilter;
    @FXML private VBox       emptyPlaceholder;

    // ── Card gallery ─────────────────────────────────────────────────────────
    @FXML private FlowPane recipeContainer;

    // ── Action bar ───────────────────────────────────────────────────────────
    @FXML private Button globalReportButton;

    private RecipeQueryProvider   queryProvider;
    private RecipeCommandProvider commandProvider;
    private NavigationService     nav;
    private ExecutorService       executor;

    private PauseTransition debounce;

    @FXML
    public void initialize() {
        debounce = new PauseTransition(Duration.millis(300));
        debounce.setOnFinished(e -> executeSearch());

        searchField.textProperty().addListener((obs, o, n)      -> debounce.playFromStart());
        categoryFilter.textProperty().addListener((obs, o, n)   -> debounce.playFromStart());
        difficultyFilter.textProperty().addListener((obs, o, n) -> debounce.playFromStart());
        ingredientFilter.textProperty().addListener((obs, o, n) -> debounce.playFromStart());
        authorFilter.textProperty().addListener((obs, o, n)     -> debounce.playFromStart());
    }

    public void init(RecipeQueryProvider queryProvider, RecipeCommandProvider commandProvider,
                     NavigationService nav, ExecutorService executor) {
        this.queryProvider   = queryProvider;
        this.commandProvider = commandProvider;
        this.nav             = nav;
        this.executor        = executor;
        executeSearch();
    }

    // ── Search & debounce ────────────────────────────────────────────────────

    private void executeSearch() {
        if (executor == null) return;
        SearchCriteria criteria = buildCriteria();
        Task<SearchResult> task = new Task<>() {
            @Override
            protected SearchResult call() {
                List<RecipeSummaryResponse> recipes = queryProvider.searchRecipes()
                        .execute(criteria, new PageRequest(0, 200)).content();
                boolean hasFavorites = !queryProvider.getUserFavorites().execute().isEmpty();
                return new SearchResult(recipes, hasFavorites);
            }
        };
        task.setOnSucceeded(e -> {
            SearchResult result = task.getValue();
            displayResults(result.recipes());
            globalReportButton.setDisable(!result.hasFavorites());
        });
        task.setOnFailed(e -> Thread.getDefaultUncaughtExceptionHandler()
                .uncaughtException(Thread.currentThread(), task.getException()));
        executor.execute(task);
    }

    private void displayResults(List<RecipeSummaryResponse> recipes) {
        recipeContainer.getChildren().clear();
        boolean empty = recipes.isEmpty();
        recipeContainer.setVisible(!empty);
        recipeContainer.setManaged(!empty);
        emptyPlaceholder.setVisible(empty);
        emptyPlaceholder.setManaged(empty);
        for (RecipeSummaryResponse recipe : recipes) {
            recipeContainer.getChildren().add(
                    new RecipeCardComponent(recipe,
                            id -> nav.toRecipeDetail(id),
                            StorageConfig.getBasePath()));
        }
    }

    private SearchCriteria buildCriteria() {
        return new SearchCriteria(
                nullIfBlank(searchField.getText()),
                null,
                null,
                nullIfBlank(categoryFilter.getText()),
                nullIfBlank(difficultyFilter.getText()),
                nullIfBlank(ingredientFilter.getText()),
                nullIfBlank(authorFilter.getText()));
    }

    private String nullIfBlank(String s) {
        return (s == null || s.isBlank()) ? null : s.strip();
    }

    private void filterByAuthor(String username) {
        authorFilter.setText(username);
        if (!filterPane.isExpanded()) filterPane.setExpanded(true);
    }

    public void focusSearch() {
        searchField.requestFocus();
    }

    @FXML
    public void onClearSearch() {
        searchField.clear();
        categoryFilter.clear();
        difficultyFilter.clear();
        ingredientFilter.clear();
        authorFilter.clear();
        debounce.stop();
        executeSearch();
    }

    // ── Action handlers ───────────────────────────────────────────────────────

    @FXML
    public void onGlobalReportButtonClick() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(I18n.get("dialog.exportFavoritesPdf.title"));
        chooser.setInitialFileName(I18n.get("dialog.exportFavoritesPdf.filename"));
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(I18n.get("dialog.filter.pdf"), "*.pdf"));
        File file = chooser.showSaveDialog(recipeContainer.getScene().getWindow());
        if (file == null) return;

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                try (FileOutputStream out = new FileOutputStream(file)) {
                    commandProvider.generateGlobalInventory().execute(out);
                }
                return null;
            }
        };
        task.setOnSucceeded(e ->
                NotificationService.success(recipeContainer, I18n.format("dashboard.notification.favoritesExported", file.getName())));
        task.setOnFailed(e -> Thread.getDefaultUncaughtExceptionHandler()
                .uncaughtException(Thread.currentThread(), task.getException()));
        executor.execute(task);
    }

    @FXML public void onLogoutButtonClick()  { nav.logout(); }
    @FXML public void onProfileButtonClick() { nav.toUserProfile(); }
    @FXML public void onCreateButtonClick()  { nav.toRecipeCreate(); }

    @FXML
    public void onImportButtonClick() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(I18n.get("dialog.importXml.title"));
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(I18n.get("dialog.filter.xml"), "*.xml"));
        File file = chooser.showOpenDialog(recipeContainer.getScene().getWindow());
        if (file == null) return;

        commandProvider.importRecipe().execute(file);
        executeSearch();
        NotificationService.success(recipeContainer,
                I18n.format("dashboard.notification.recipeImported", file.getName()));
    }

    private record SearchResult(List<RecipeSummaryResponse> recipes, boolean hasFavorites) {}
}
