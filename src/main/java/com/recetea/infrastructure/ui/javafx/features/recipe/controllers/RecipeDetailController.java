package com.recetea.infrastructure.ui.javafx.features.recipe.controllers;

import com.recetea.core.recipe.application.ports.in.dto.RecipeDetailResponse;
import com.recetea.core.recipe.application.ports.in.dto.RecipeIngredientResponse;
import com.recetea.core.recipe.domain.vo.RecipeId;
import com.recetea.infrastructure.ui.javafx.features.recipe.RecipeCommandProvider;
import com.recetea.infrastructure.ui.javafx.features.recipe.RecipeQueryProvider;
import com.recetea.infrastructure.ui.javafx.features.recipe.components.RatingComponent;
import com.recetea.infrastructure.ui.javafx.shared.navigation.NavigationService;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.math.BigDecimal;

public class RecipeDetailController {

    @FXML private Label titleLabel;
    @FXML private Label prepTimeLabel;
    @FXML private Label servingsLabel;
    @FXML private Label descriptionLabel;
    @FXML private Label categoryLabel;
    @FXML private Label difficultyLabel;
    @FXML private Label scoreLabel;

    @FXML private TableView<RecipeIngredientResponse> ingredientsTable;
    @FXML private TableColumn<RecipeIngredientResponse, String> colIngredientName;
    @FXML private TableColumn<RecipeIngredientResponse, String> colUnit;
    @FXML private TableColumn<RecipeIngredientResponse, BigDecimal> colQuantity;

    @FXML private TableView<RecipeDetailResponse.RecipeStepResponse> stepsTable;
    @FXML private TableColumn<RecipeDetailResponse.RecipeStepResponse, Integer> colStepOrder;
    @FXML private TableColumn<RecipeDetailResponse.RecipeStepResponse, String> colInstruction;

    @FXML private RatingComponent ratingComponent;
    @FXML private ToggleButton favoriteButton;

    private RecipeQueryProvider queryProvider;
    private RecipeCommandProvider commandProvider;
    private NavigationService nav;
    private RecipeId currentRecipeId;

    public void init(RecipeQueryProvider queryProvider, RecipeCommandProvider commandProvider, NavigationService nav) {
        this.queryProvider = queryProvider;
        this.commandProvider = commandProvider;
        this.nav = nav;

        colIngredientName.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().ingredientName()));
        colUnit.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().unitName()));
        colQuantity.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().quantity()));
        colStepOrder.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().stepOrder()));
        colInstruction.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().instruction()));

        favoriteButton.setOnAction(e -> {
            commandProvider.toggleFavorite().execute(currentRecipeId);
            refreshFavoriteButton(currentRecipeId);
        });
    }

    public void loadRecipeDetails(RecipeId recipeId) {
        this.currentRecipeId = recipeId;
        ratingComponent.setRecipeContext(commandProvider, recipeId, () -> loadRecipeDetails(currentRecipeId));
        refreshFavoriteButton(recipeId);
        queryProvider.getRecipeById().execute(recipeId).ifPresentOrElse(
                this::populateView,
                () -> showError("Error de Consulta", "El sistema no pudo localizar la receta solicitada.")
        );
    }

    private void refreshFavoriteButton(RecipeId recipeId) {
        boolean isFav = commandProvider.isFavorite().execute(recipeId);
        favoriteButton.setSelected(isFav);
        favoriteButton.setText(isFav ? "★ En favoritos" : "☆ Añadir a favoritos");
    }

    private void populateView(RecipeDetailResponse recipe) {
        titleLabel.setText(recipe.title());
        prepTimeLabel.setText(String.format("%d min", recipe.prepTimeMinutes()));
        servingsLabel.setText(String.valueOf(recipe.servings()));
        descriptionLabel.setText(recipe.description());
        categoryLabel.setText(recipe.categoryName());
        difficultyLabel.setText(recipe.difficultyName());
        scoreLabel.setText(String.format("%s (%d valoraciones)",
                recipe.averageScore().setScale(1, java.math.RoundingMode.HALF_UP),
                recipe.totalRatings()));

        ingredientsTable.setItems(FXCollections.observableArrayList(recipe.ingredients()));
        stepsTable.setItems(FXCollections.observableArrayList(recipe.steps()));

        commandProvider.sessionService().getCurrentUserId()
                .filter(currentId -> currentId.equals(recipe.userId()))
                .ifPresent(__ -> ratingComponent.disableWithStatus("No puedes valorar tu propia receta."));
    }

    @FXML
    public void onBackButtonClick() {
        nav.toDashboard();
    }

    private void showError(String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
