package com.recetea.infrastructure.ui.javafx.features.recipe.components;

import com.recetea.core.recipe.application.ports.in.dto.AddRatingRequest;
import com.recetea.core.recipe.domain.vo.RecipeId;
import com.recetea.core.recipe.domain.vo.Score;
import com.recetea.infrastructure.ui.javafx.features.recipe.RecipeCommandProvider;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;

import java.io.IOException;

public class RatingComponent extends VBox {

    @FXML private ComboBox<Integer> scoreComboBox;
    @FXML private TextArea commentArea;
    @FXML private Button submitButton;
    @FXML private Label statusLabel;

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
        boolean isNewRecipe = !recipeId.equals(this.recipeId);
        this.commandProvider = commandProvider;
        this.recipeId = recipeId;
        this.onSuccess = onSuccess;
        if (isNewRecipe) reset();
    }

    public void disableWithStatus(String message) {
        scoreComboBox.setDisable(true);
        commentArea.setDisable(true);
        submitButton.setDisable(true);
        statusLabel.setText(message);
        statusLabel.setVisible(true);
        statusLabel.setManaged(true);
    }

    private void reset() {
        scoreComboBox.setDisable(false);
        scoreComboBox.setValue(null);
        commentArea.setDisable(false);
        commentArea.clear();
        submitButton.setDisable(false);
        statusLabel.setText("");
        statusLabel.setVisible(false);
        statusLabel.setManaged(false);
    }

    @FXML
    private void onSubmit() {
        Integer selectedScore = scoreComboBox.getValue();
        if (selectedScore == null) {
            statusLabel.setText("Por favor, selecciona una puntuación entre 1 y 5.");
            statusLabel.setVisible(true);
            statusLabel.setManaged(true);
            return;
        }

        String comment = commentArea.getText() != null ? commentArea.getText().trim() : "";
        AddRatingRequest request = new AddRatingRequest(recipeId, new Score(selectedScore), comment);

        commandProvider.addRating().execute(request);
        disableWithStatus("Valoración enviada. ¡Gracias!");
        if (onSuccess != null) onSuccess.run();
    }
}
