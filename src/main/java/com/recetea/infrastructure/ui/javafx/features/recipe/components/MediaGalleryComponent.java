package com.recetea.infrastructure.ui.javafx.features.recipe.components;

import com.recetea.core.recipe.application.ports.in.dto.RecipeDetailResponse;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import com.recetea.infrastructure.ui.javafx.shared.i18n.I18n;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class MediaGalleryComponent extends VBox {

    @FXML private StackPane mainContainer;
    @FXML private ImageView mainImageView;
    @FXML private Region    mainSkeleton;
    @FXML private Label     noMediaLabel;
    @FXML private HBox      thumbnailStrip;

    private static final int THUMB_SIZE = 70;
    private static final int MAIN_W     = 500;
    private static final int MAIN_H     = 300;

    private Timeline skeletonAnimation;

    public MediaGalleryComponent() {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(
                "/com/recetea/infrastructure/ui/javafx/fxml/features/recipe/components/media_gallery.fxml"));
        loader.setRoot(this);
        loader.setController(this);
        loader.setResources(I18n.bundle());
        try {
            loader.load();
        } catch (IOException e) {
            throw new RuntimeException("Infrastructure Failure: Imposible instanciar MediaGalleryComponent.", e);
        }
    }

    public void setMedia(List<RecipeDetailResponse.RecipeMediaResponse> mediaItems, Path basePath) {
        stopSkeletonPulse();
        thumbnailStrip.getChildren().clear();

        if (mediaItems == null || mediaItems.isEmpty()) {
            showPlaceholder();
            return;
        }

        noMediaLabel.setVisible(false);
        mainImageView.setVisible(true);

        RecipeDetailResponse.RecipeMediaResponse initialMain = mediaItems.stream()
                .filter(RecipeDetailResponse.RecipeMediaResponse::isMain)
                .findFirst()
                .orElse(mediaItems.get(0));

        loadMain(basePath.resolve(initialMain.storageKey()).toUri().toString());

        for (RecipeDetailResponse.RecipeMediaResponse m : mediaItems) {
            String fileUrl = basePath.resolve(m.storageKey()).toUri().toString();

            Region thumbSkeleton = new Region();
            thumbSkeleton.getStyleClass().add("skeleton-pulse");
            thumbSkeleton.setMinSize(THUMB_SIZE, THUMB_SIZE);
            thumbSkeleton.setMaxSize(THUMB_SIZE, THUMB_SIZE);

            StackPane tile = new StackPane(thumbSkeleton);
            tile.getStyleClass().add("gallery-thumbnail");
            if (m.isMain()) tile.getStyleClass().add("gallery-thumbnail-selected");

            tile.setOnMouseClicked(e -> {
                selectThumbnail(tile);
                loadMain(fileUrl);
            });

            thumbnailStrip.getChildren().add(tile);

            Image thumbImg = new Image(fileUrl, THUMB_SIZE, THUMB_SIZE, true, true, true);
            if (thumbImg.getProgress() >= 1.0 && !thumbImg.isError()) {
                replaceSkeletonWithThumb(tile, thumbImg);
            } else {
                thumbImg.progressProperty().addListener((obs, prev, p) -> {
                    if (p.doubleValue() >= 1.0 && !thumbImg.isError())
                        replaceSkeletonWithThumb(tile, thumbImg);
                });
            }
        }
    }

    private void replaceSkeletonWithThumb(StackPane tile, Image img) {
        ImageView iv = new ImageView(img);
        iv.setFitWidth(THUMB_SIZE);
        iv.setFitHeight(THUMB_SIZE);
        iv.setPreserveRatio(true);
        tile.getChildren().setAll(iv);
    }

    private void loadMain(String fileUrl) {
        mainImageView.setImage(null);
        mainImageView.setVisible(false);
        mainSkeleton.setVisible(true);
        startSkeletonPulse();

        Image img = new Image(fileUrl, MAIN_W, MAIN_H, true, true, true);

        if (img.getProgress() >= 1.0 && !img.isError()) {
            applyMainImage(img);
            return;
        }

        img.progressProperty().addListener((obs, prev, progress) -> {
            if (progress.doubleValue() >= 1.0 && !img.isError()) applyMainImage(img);
        });
        img.errorProperty().addListener((obs, prev, error) -> {
            if (error) {
                mainSkeleton.setVisible(false);
                stopSkeletonPulse();
            }
        });
    }

    private void applyMainImage(Image img) {
        mainSkeleton.setVisible(false);
        stopSkeletonPulse();
        mainImageView.setImage(img);
        mainImageView.setVisible(true);
    }

    private void selectThumbnail(StackPane selected) {
        thumbnailStrip.getChildren().forEach(n ->
                n.getStyleClass().remove("gallery-thumbnail-selected"));
        selected.getStyleClass().add("gallery-thumbnail-selected");
    }

    private void showPlaceholder() {
        mainSkeleton.setVisible(false);
        mainImageView.setImage(null);
        mainImageView.setVisible(false);
        noMediaLabel.setVisible(true);
        thumbnailStrip.getChildren().clear();
    }

    private void startSkeletonPulse() {
        stopSkeletonPulse();
        skeletonAnimation = new Timeline(
                new KeyFrame(Duration.ZERO,       new KeyValue(mainSkeleton.opacityProperty(), 0.45)),
                new KeyFrame(Duration.millis(750), new KeyValue(mainSkeleton.opacityProperty(), 0.90))
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
        mainSkeleton.setOpacity(1.0);
    }
}
