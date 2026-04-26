package com.recetea.infrastructure.ui.javafx.features.recipe.controllers;

import com.recetea.core.recipe.application.ports.in.dto.RecipeDetailResponse;
import com.recetea.core.recipe.application.ports.in.dto.RecipeIngredientResponse;
import com.recetea.core.recipe.domain.vo.RecipeId;
import com.recetea.infrastructure.storage.StorageConfig;
import com.recetea.infrastructure.ui.javafx.features.recipe.RecipeCommandProvider;
import com.recetea.infrastructure.ui.javafx.features.recipe.RecipeQueryProvider;
import com.recetea.infrastructure.ui.javafx.features.recipe.components.CommentItemComponent;
import com.recetea.infrastructure.ui.javafx.features.recipe.components.MediaGalleryComponent;
import com.recetea.infrastructure.ui.javafx.features.recipe.components.RatingComponent;
import com.recetea.infrastructure.ui.javafx.shared.i18n.I18n;
import com.recetea.infrastructure.ui.javafx.shared.navigation.NavigationService;
import com.recetea.infrastructure.ui.javafx.shared.notification.NotificationService;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.FileOutputStream;
import java.util.concurrent.ExecutorService;

public class RecipeDetailController {

    @FXML private Label titleLabel;
    @FXML private Label prepTimeLabel;
    @FXML private Label servingsLabel;
    @FXML private Label descriptionLabel;
    @FXML private Label categoryLabel;
    @FXML private Label difficultyLabel;
    @FXML private Label scoreLabel;

    @FXML private ScrollPane mainContentScroll;
    @FXML private FlowPane   ingredientsContainer;
    @FXML private VBox       stepsContainer;

    @FXML private MediaGalleryComponent mediaGallery;
    @FXML private RatingComponent ratingComponent;
    @FXML private ToggleButton favoriteButton;
    @FXML private VBox reviewsList;

    private RecipeQueryProvider   queryProvider;
    private RecipeCommandProvider commandProvider;
    private NavigationService     nav;
    private ExecutorService       executor;
    private RecipeId              currentRecipeId;
    private String                currentTitle = "receta";

    public void init(RecipeQueryProvider queryProvider, RecipeCommandProvider commandProvider,
                     NavigationService nav, ExecutorService executor) {
        this.queryProvider   = queryProvider;
        this.commandProvider = commandProvider;
        this.nav             = nav;
        this.executor        = executor;

        favoriteButton.setOnAction(e -> {
            commandProvider.toggleFavorite().execute(currentRecipeId);
            refreshFavoriteButton(currentRecipeId);
        });
    }

    public void loadRecipeDetails(RecipeId recipeId) {
        this.currentRecipeId = recipeId;
        mainContentScroll.setVvalue(0);
        ratingComponent.setRecipeContext(commandProvider, recipeId, () -> loadRecipeDetails(currentRecipeId));
        executor.execute(() -> {
            boolean isFav = commandProvider.isFavorite().execute(recipeId);
            var response  = queryProvider.getRecipeById().execute(recipeId);
            Platform.runLater(() -> {
                favoriteButton.setSelected(isFav);
                favoriteButton.setText(isFav ? I18n.get("recipe.detail.button.inFavorites")
                                             : I18n.get("recipe.detail.button.addFavorite"));
                response.ifPresentOrElse(
                        this::populateView,
                        () -> NotificationService.error(titleLabel,
                                I18n.get("recipe.detail.error.notFound"))
                );
            });
        });
    }

    private void refreshFavoriteButton(RecipeId recipeId) {
        executor.execute(() -> {
            boolean isFav = commandProvider.isFavorite().execute(recipeId);
            Platform.runLater(() -> {
                favoriteButton.setSelected(isFav);
                favoriteButton.setText(isFav ? I18n.get("recipe.detail.button.inFavorites")
                                             : I18n.get("recipe.detail.button.addFavorite"));
            });
        });
    }

    private void populateView(RecipeDetailResponse recipe) {
        currentTitle = recipe.title();
        titleLabel.setText(recipe.title());
        prepTimeLabel.setText(String.format("%d min", recipe.prepTimeMinutes()));
        servingsLabel.setText(String.valueOf(recipe.servings()));
        descriptionLabel.setText(recipe.description());
        categoryLabel.setText(recipe.categoryName());
        difficultyLabel.setText(recipe.difficultyName());
        scoreLabel.setText(I18n.format("recipe.detail.meta.scoreFormat",
                recipe.averageScore().setScale(1, java.math.RoundingMode.HALF_UP),
                recipe.totalRatings()));

        mediaGallery.setMedia(recipe.media(), StorageConfig.getBasePath());

        buildIngredientChips(recipe);
        buildStepCards(recipe);

        if (recipe.alreadyRatedByCurrentUser()) {
            ratingComponent.disableWithStatus(I18n.get("rating.disabled.alreadyRated"));
        }
        commandProvider.sessionService().getCurrentUserId()
                .filter(id -> id.equals(recipe.userId()))
                .ifPresent(__ -> ratingComponent.disableWithStatus(I18n.get("rating.disabled.ownRecipe")));

        reviewsList.getChildren().clear();
        recipe.ratings().forEach(r -> reviewsList.getChildren().add(new CommentItemComponent(r)));
    }

    private void buildIngredientChips(RecipeDetailResponse recipe) {
        ingredientsContainer.getChildren().clear();
        for (RecipeIngredientResponse ing : recipe.ingredients()) {
            String qty  = ing.quantity().stripTrailingZeros().toPlainString();
            Label  chip = new Label(qty + " " + ing.unitName() + "  ·  " + ing.ingredientName());
            chip.getStyleClass().add("ingredient-chip");
            ingredientsContainer.getChildren().add(chip);
        }
    }

    private void buildStepCards(RecipeDetailResponse recipe) {
        stepsContainer.getChildren().clear();
        for (RecipeDetailResponse.RecipeStepResponse step : recipe.steps()) {
            VBox card = new VBox(6);
            card.getStyleClass().add("step-detail-card");

            Label num = new Label(I18n.format("recipe.detail.step.label", step.stepOrder()));
            num.getStyleClass().add("step-detail-number");

            Label instr = new Label(step.instruction());
            instr.setWrapText(true);
            instr.getStyleClass().add("step-detail-instruction");

            card.getChildren().addAll(num, instr);
            stepsContainer.getChildren().add(card);
        }
    }

    @FXML
    public void onGeneratePdfClick() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(I18n.get("dialog.savePdf.title"));
        chooser.setInitialFileName(sanitizeFilename(currentTitle) + ".pdf");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(I18n.get("dialog.filter.pdf"), "*.pdf"));

        File file = chooser.showSaveDialog(titleLabel.getScene().getWindow());
        if (file == null) return;

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                try (FileOutputStream out = new FileOutputStream(file)) {
                    commandProvider.generateTechnicalSheet().execute(currentRecipeId, out);
                }
                return null;
            }
        };
        task.setOnSucceeded(e ->
                NotificationService.success(titleLabel,
                        I18n.format("recipe.detail.notification.pdfSaved", file.getName())));
        task.setOnFailed(e -> Thread.getDefaultUncaughtExceptionHandler()
                .uncaughtException(Thread.currentThread(), task.getException()));
        executor.execute(task);
    }

    @FXML
    public void onExportButtonClick() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(I18n.get("dialog.exportXml.title"));
        chooser.setInitialFileName(sanitizeFilename(currentTitle) + ".xml");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(I18n.get("dialog.filter.xml"), "*.xml"));

        File file = chooser.showSaveDialog(titleLabel.getScene().getWindow());
        if (file == null) return;

        commandProvider.exportRecipe().execute(currentRecipeId, file);
        NotificationService.success(titleLabel, I18n.format("recipe.detail.notification.xmlExported", file.getName()));
    }

    private static String sanitizeFilename(String title) {
        return title.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
    }

    @FXML
    public void onBackButtonClick() {
        nav.toDashboard();
    }
}
