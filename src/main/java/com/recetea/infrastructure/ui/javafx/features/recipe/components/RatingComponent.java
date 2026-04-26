package com.recetea.infrastructure.ui.javafx.features.recipe.components;

import com.recetea.core.recipe.application.ports.in.dto.AddRatingRequest;
import com.recetea.core.recipe.domain.vo.RecipeId;
import com.recetea.core.recipe.domain.vo.Score;
import com.recetea.infrastructure.ui.javafx.features.recipe.RecipeCommandProvider;
import com.recetea.infrastructure.ui.javafx.shared.i18n.I18n;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.io.IOException;

public class RatingComponent extends VBox {

    @FXML private HBox     starContainer;
    @FXML private TextArea commentArea;
    @FXML private Button   submitButton;
    @FXML private Label    statusLabel;

    private int selectedScore = 0;

    private RecipeCommandProvider commandProvider;
    private RecipeId              recipeId;
    private Runnable              onSuccess;

    public RatingComponent() {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(
                "/com/recetea/infrastructure/ui/javafx/fxml/features/recipe/components/rating_component.fxml"));
        loader.setRoot(this);
        loader.setController(this);
        loader.setResources(I18n.bundle());
        try {
            loader.load();
        } catch (IOException e) {
            throw new RuntimeException("Infrastructure Failure: Imposible instanciar RatingComponent.", e);
        }
    }

    @FXML
    private void initialize() {
        for (int i = 1; i <= 5; i++) {
            final int star = i;
            Label lbl = new Label("★");
            lbl.getStyleClass().add("star-icon");
            lbl.setOnMouseEntered(e -> updateStarClasses(star, "preview"));
            lbl.setOnMouseExited(e -> updateStarClasses(selectedScore, "active"));
            lbl.setOnMouseClicked(e -> {
                selectedScore = star;
                updateStarClasses(selectedScore, "active");
            });
            starContainer.getChildren().add(lbl);
        }
    }

    public void setRecipeContext(RecipeCommandProvider commandProvider, RecipeId recipeId, Runnable onSuccess) {
        boolean isNewRecipe = !recipeId.equals(this.recipeId);
        this.commandProvider = commandProvider;
        this.recipeId        = recipeId;
        this.onSuccess       = onSuccess;
        if (isNewRecipe) reset();
    }

    public void disableWithStatus(String message) {
        starContainer.setDisable(true);
        commentArea.setDisable(true);
        submitButton.setDisable(true);
        statusLabel.setText(message);
        statusLabel.setVisible(true);
        statusLabel.setManaged(true);
    }

    private void reset() {
        selectedScore = 0;
        updateStarClasses(0, "active");
        starContainer.setDisable(false);
        commentArea.setDisable(false);
        commentArea.clear();
        submitButton.setDisable(false);
        statusLabel.setText("");
        statusLabel.setVisible(false);
        statusLabel.setManaged(false);
    }

    private void updateStarClasses(int upTo, String styleClass) {
        var stars = starContainer.getChildren();
        for (int i = 0; i < stars.size(); i++) {
            Label lbl = (Label) stars.get(i);
            lbl.getStyleClass().removeAll("active", "preview");
            if (i < upTo) {
                lbl.getStyleClass().add(styleClass);
            }
        }
    }

    @FXML
    private void onSubmit() {
        if (selectedScore == 0) {
            statusLabel.setText(I18n.get("rating.error.noScore"));
            statusLabel.setVisible(true);
            statusLabel.setManaged(true);
            return;
        }
        String comment = commentArea.getText() != null ? commentArea.getText().trim() : "";
        AddRatingRequest request = new AddRatingRequest(recipeId, new Score(selectedScore), comment);
        commandProvider.addRating().execute(request);
        disableWithStatus(I18n.get("rating.notification.submitted"));
        if (onSuccess != null) onSuccess.run();
    }
}
