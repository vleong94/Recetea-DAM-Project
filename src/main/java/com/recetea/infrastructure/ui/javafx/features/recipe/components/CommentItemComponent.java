package com.recetea.infrastructure.ui.javafx.features.recipe.components;

import com.recetea.core.recipe.application.ports.in.dto.RecipeDetailResponse;
import com.recetea.core.shared.domain.utils.RelativeTimeFormatter;
import com.recetea.infrastructure.ui.javafx.shared.i18n.I18n;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * Renders one row in the rating-comment list on the recipe-detail page.
 * Shows the voter's username, score (as a star string), the comment
 * body, and a relative-time label.
 *
 * <p>Time formatting goes through {@link RelativeTimeFormatter} with
 * {@code I18n::get} / {@code I18n::format} as the lookup functions —
 * the formatter itself stays in {@code core.shared.domain.utils} (no
 * UI dependency). Null username (account deleted post-rating) renders
 * as a localised placeholder rather than blank.
 *
 * <p><b>ES — </b>Renderiza una fila en la lista de comentarios de
 * valoración en la página de detalle de receta. Muestra el
 * username del votante, la puntuación (como cadena de estrellas),
 * el cuerpo del comentario y una etiqueta de tiempo relativo.
 *
 * <p>El formato de tiempo pasa por {@link RelativeTimeFormatter}
 * con {@code I18n::get} / {@code I18n::format} como funciones de
 * búsqueda — el propio formateador queda en
 * {@code core.shared.domain.utils} (sin dependencia de UI). El
 * username nulo (cuenta eliminada tras la valoración) se renderiza
 * como un placeholder localizado en lugar de en blanco.
 */
public class CommentItemComponent extends VBox {

    @FXML private Label starsLabel;
    @FXML private Label usernameLabel;
    @FXML private Label dateLabel;
    @FXML private Label commentLabel;

    public CommentItemComponent(RecipeDetailResponse.RatingDetail rating) {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(
                "/com/recetea/infrastructure/ui/javafx/fxml/features/recipe/components/comment_item.fxml"));
        loader.setRoot(this);
        loader.setController(this);
        loader.setResources(I18n.bundle());
        try {
            loader.load();
        } catch (IOException e) {
            throw new RuntimeException("Infrastructure failure: could not instantiate CommentItemComponent.", e);
        }
        populate(rating);
    }

    private void populate(RecipeDetailResponse.RatingDetail rating) {
        int score = rating.score();
        starsLabel.setText("★".repeat(score) + "☆".repeat(5 - score));
        usernameLabel.setText(rating.username() != null ? rating.username() : I18n.get("comment.user.deleted"));
        commentLabel.setText(rating.comment() != null ? rating.comment() : "");
        dateLabel.setText(RelativeTimeFormatter.format(rating.date(), I18n::get, (k, n) -> I18n.format(k, n)));
    }
}
