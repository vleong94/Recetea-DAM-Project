package com.recetea.infrastructure.ui.javafx.features.recipe.components;

import com.recetea.core.recipe.application.ports.in.dto.AddRatingRequest;
import com.recetea.core.recipe.domain.vo.RecipeId;
import com.recetea.core.recipe.domain.vo.Score;
import com.recetea.infrastructure.ui.javafx.features.recipe.IRatingWriteProvider;
import com.recetea.infrastructure.ui.javafx.shared.i18n.I18n;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.io.IOException;

/**
 * Recipe-detail sub-component for casting a single rating. Five-star
 * row (click to select) plus a {@link TextArea} for the comment and a
 * submit button.
 *
 * <p><b>ISP-narrowed dependency.</b> The component takes
 * {@link IRatingWriteProvider} — not the umbrella
 * {@code RecipeCommandProvider} — because rating-write is the only
 * use case it touches. Keeps the surface area minimum and makes the
 * component trivially testable.
 *
 * <p><b>Self-rating + duplicate-vote</b> are both rejected at the
 * domain layer ({@code Recipe.addRating}); the controller wires this
 * component to be disabled when {@code alreadyRatedByCurrentUser}
 * is true on the response, so users see the disabled state rather
 * than triggering a {@link com.recetea.core.shared.domain.DomainException}.
 *
 * <p><b>ES — </b>Sub-componente del detalle de receta para emitir
 * una valoración. Fila de cinco estrellas (click para seleccionar)
 * más una {@link TextArea} para el comentario y un botón de
 * submit.
 *
 * <p><b>Dependencia estrechada por ISP.</b> El componente recibe
 * {@link IRatingWriteProvider} — no el umbrella
 * {@code RecipeCommandProvider} — porque la escritura de
 * valoración es el único caso de uso que toca. Mantiene la
 * superficie al mínimo y hace al componente trivialmente
 * testeable.
 *
 * <p><b>Auto-valoración + voto duplicado</b> los rechaza la capa
 * de dominio ({@code Recipe.addRating}); el controlador cablea
 * este componente para que esté deshabilitado cuando
 * {@code alreadyRatedByCurrentUser} sea true en la respuesta, de
 * modo que los usuarios vean el estado deshabilitado en lugar de
 * disparar una
 * {@link com.recetea.core.shared.domain.DomainException}.
 */
public class RatingComponent extends VBox {

    @FXML private HBox     starContainer;
    @FXML private TextArea commentArea;
    @FXML private Button   submitButton;
    @FXML private Label    statusLabel;

    private int selectedScore = 0;

    /**
     * ISP-narrowed: the component only writes ratings, so the granular
     * {@link IRatingWriteProvider} is enough — the umbrella {@code RecipeCommandProvider}
     * exposed dozens of unrelated use cases (recipe CRUD, media attach, interop, reports)
     * that this component had no business seeing.
     */
    private IRatingWriteProvider ratingProvider;
    private RecipeId             recipeId;
    private Runnable             onSuccess;

    public RatingComponent() {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(
                "/com/recetea/infrastructure/ui/javafx/fxml/features/recipe/components/rating_component.fxml"));
        loader.setRoot(this);
        loader.setController(this);
        loader.setResources(I18n.bundle());
        try {
            loader.load();
        } catch (IOException e) {
            throw new RuntimeException("Infrastructure failure: could not instantiate RatingComponent.", e);
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

    public void setRecipeContext(IRatingWriteProvider ratingProvider, RecipeId recipeId, Runnable onSuccess) {
        boolean isNewRecipe = !recipeId.equals(this.recipeId);
        this.ratingProvider = ratingProvider;
        this.recipeId       = recipeId;
        this.onSuccess      = onSuccess;
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
        ratingProvider.addRating().execute(request);
        disableWithStatus(I18n.get("rating.notification.submitted"));
        if (onSuccess != null) onSuccess.run();
    }
}
