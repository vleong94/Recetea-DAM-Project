package com.recetea.infrastructure.ui.javafx.features.recipe.components;

import com.recetea.core.recipe.application.ports.in.dto.AddRatingRequest;
import com.recetea.core.recipe.domain.vo.RecipeId;
import com.recetea.core.recipe.domain.vo.Score;
import com.recetea.infrastructure.ui.javafx.features.recipe.RecipeCommandProvider;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;

import java.io.IOException;

public class RatingComponent extends VBox {

    @FXML private ComboBox<Integer> scoreComboBox;
    @FXML private TextArea commentArea;
    @FXML private Button submitButton;

    private RecipeCommandProvider commandProvider;
    private RecipeId recipeId;
    private Runnable onSuccess;

    public RatingComponent() {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(
                "/com/recetea/infrastructure/ui/javafx/fxml/features/recipe/components/rating_component.fxml"));
        loader.setRoot(this);
        loader.setController(this);
        try {
            loader.load();
        } catch (IOException e) {
            throw new RuntimeException("Infrastructure Failure: Imposible instanciar RatingComponent.", e);
        }
    }

    @FXML
    private void initialize() {
        scoreComboBox.getItems().addAll(1, 2, 3, 4, 5);
    }

    public void setRecipeContext(RecipeCommandProvider commandProvider, RecipeId recipeId, Runnable onSuccess) {
        this.commandProvider = commandProvider;
        this.recipeId = recipeId;
        this.onSuccess = onSuccess;
    }

    @FXML
    private void onSubmit() {
        Integer selectedScore = scoreComboBox.getValue();
        if (selectedScore == null) {
            showError("Puntuación requerida", "Por favor, selecciona una puntuación entre 1 y 5.");
            return;
        }

        String comment = commentArea.getText() != null ? commentArea.getText().trim() : "";
        AddRatingRequest request = new AddRatingRequest(recipeId, new Score(selectedScore), comment);

        try {
            commandProvider.addRating().execute(request);
            submitButton.setDisable(true);
            scoreComboBox.setDisable(true);
            commentArea.setDisable(true);
            if (onSuccess != null) onSuccess.run();
        } catch (Exception e) {
            showError("Error al valorar", e.getMessage());
        }
    }

    private void showError(String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
