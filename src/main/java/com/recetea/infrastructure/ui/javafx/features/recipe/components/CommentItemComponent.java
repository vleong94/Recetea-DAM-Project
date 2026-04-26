package com.recetea.infrastructure.ui.javafx.features.recipe.components;

import com.recetea.core.recipe.application.ports.in.dto.RecipeDetailResponse;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

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
        try {
            loader.load();
        } catch (IOException e) {
            throw new RuntimeException(
                    "Infrastructure Failure: Imposible instanciar el componente visual CommentItemComponent.", e);
        }
        populate(rating);
    }

    private void populate(RecipeDetailResponse.RatingDetail rating) {
        int score = rating.score();
        starsLabel.setText("★".repeat(score) + "☆".repeat(5 - score));
        usernameLabel.setText(rating.username() != null ? rating.username() : "Usuario eliminado");
        commentLabel.setText(rating.comment() != null ? rating.comment() : "");
        dateLabel.setText(relativeTime(rating.date()));
    }

    private static String relativeTime(LocalDateTime date) {
        if (date == null) return "";
        long minutes = ChronoUnit.MINUTES.between(date, LocalDateTime.now());
        if (minutes < 1)  return "ahora mismo";
        if (minutes < 60) return "hace " + minutes + " min";
        long hours = minutes / 60;
        if (hours < 24)   return "hace " + hours + " " + (hours == 1 ? "hora" : "horas");
        long days = hours / 24;
        if (days < 30)    return "hace " + days + " " + (days == 1 ? "día" : "días");
        long months = days / 30;
        if (months < 12)  return "hace " + months + " " + (months == 1 ? "mes" : "meses");
        long years = months / 12;
        return "hace " + years + " " + (years == 1 ? "año" : "años");
    }
}
