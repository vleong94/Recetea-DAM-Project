package com.recetea.infrastructure.ui.javafx.features.recipe.components;

import com.recetea.core.recipe.application.ports.in.dto.RecipeSummaryResponse;
import com.recetea.core.recipe.domain.vo.RecipeId;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.control.Label;
import javafx.util.Duration;

import java.io.IOException;
import java.nio.file.Path;
import java.util.function.Consumer;

public class RecipeCardComponent extends VBox {

    private static final int IMAGE_W = 240;
    private static final int IMAGE_H = 160;

    @FXML private StackPane imagePane;
    @FXML private Region    imageSkeleton;
    @FXML private ImageView imageView;
    @FXML private Label     titleLabel;
    @FXML private Label     categoryLabel;
    @FXML private Label     prepTimeLabel;
    @FXML private Label     scoreLabel;

    private Timeline skeletonAnimation;

    public RecipeCardComponent(RecipeSummaryResponse recipe,
                               Consumer<RecipeId> onCardClick,
                               Path basePath) {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(
                "/com/recetea/infrastructure/ui/javafx/fxml/features/recipe/components/recipe_card.fxml"));
        loader.setRoot(this);
        loader.setController(this);
        try {
            loader.load();
        } catch (IOException e) {
            throw new RuntimeException("Infrastructure Failure: Imposible instanciar RecipeCardComponent.", e);
        }

        populate(recipe, basePath);

        if (onCardClick != null) {
            setOnMouseClicked(e -> onCardClick.accept(recipe.id()));
        }
    }

    private void populate(RecipeSummaryResponse recipe, Path basePath) {
        titleLabel.setText(recipe.title());
        categoryLabel.setText(recipe.categoryName() != null ? recipe.categoryName() : "");
        prepTimeLabel.setText("⏱ " + recipe.prepTimeMinutes() + " min");
        scoreLabel.setText(formatScore(recipe));

        if (recipe.mainMediaStorageKey() != null && basePath != null) {
            loadImage(basePath, recipe.mainMediaStorageKey());
        } else {
            imageSkeleton.getStyleClass().add("recipe-card-no-image");
            stopSkeletonPulse();
        }
    }

    private void loadImage(Path basePath, String storageKey) {
        startSkeletonPulse();
        String fileUrl = basePath.resolve(storageKey).toUri().toString();
        Image img = new Image(fileUrl, IMAGE_W, IMAGE_H, true, true, true);

        if (img.getProgress() >= 1.0 && !img.isError()) {
            applyImage(img);
            return;
        }

        img.progressProperty().addListener((obs, prev, progress) -> {
            if (progress.doubleValue() >= 1.0 && !img.isError()) applyImage(img);
        });
        img.errorProperty().addListener((obs, prev, error) -> {
            if (error) {
                stopSkeletonPulse();
                imageSkeleton.getStyleClass().add("recipe-card-no-image");
            }
        });
    }

    private void applyImage(Image img) {
        stopSkeletonPulse();
        imageSkeleton.setVisible(false);
        imageView.setImage(img);
        imageView.setVisible(true);
    }

    private void startSkeletonPulse() {
        stopSkeletonPulse();
        skeletonAnimation = new Timeline(
                new KeyFrame(Duration.ZERO,        new KeyValue(imageSkeleton.opacityProperty(), 0.45)),
                new KeyFrame(Duration.millis(700),  new KeyValue(imageSkeleton.opacityProperty(), 0.90))
        );
        skeletonAnimation.setAutoReverse(true);
        skeletonAnimation.setCycleCount(Animation.INDEFINITE);
        skeletonAnimation.play();
    }

    private void stopSkeletonPulse() {
        if (skeletonAnimation != null) {
            skeletonAnimation.stop();
            skeletonAnimation = null;
        }
        imageSkeleton.setOpacity(1.0);
    }

    private static String formatScore(RecipeSummaryResponse recipe) {
        if (recipe.totalRatings() == 0) return "★ —";
        return String.format("★ %.1f", recipe.averageScore().doubleValue());
    }
}
