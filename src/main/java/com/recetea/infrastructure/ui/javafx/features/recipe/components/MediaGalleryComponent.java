package com.recetea.infrastructure.ui.javafx.features.recipe.components;

import com.recetea.core.recipe.application.ports.in.dto.RecipeDetailResponse;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class MediaGalleryComponent extends VBox {

    @FXML private StackPane mainContainer;
    @FXML private ImageView mainImageView;
    @FXML private Label noMediaLabel;
    @FXML private HBox thumbnailStrip;

    private static final int THUMB_SIZE  = 70;
    private static final int MAIN_W      = 500;
    private static final int MAIN_H      = 300;

    public MediaGalleryComponent() {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(
                "/com/recetea/infrastructure/ui/javafx/fxml/features/recipe/components/media_gallery.fxml"));
        loader.setRoot(this);
        loader.setController(this);
        try {
            loader.load();
        } catch (IOException e) {
            throw new RuntimeException("Infrastructure Failure: Imposible instanciar MediaGalleryComponent.", e);
        }
    }

    public void setMedia(List<RecipeDetailResponse.RecipeMediaResponse> mediaItems, Path basePath) {
        thumbnailStrip.getChildren().clear();

        if (mediaItems == null || mediaItems.isEmpty()) {
            showPlaceholder();
            return;
        }

        noMediaLabel.setVisible(false);
        mainImageView.setVisible(true);

        // Display the main image (or the first one if none is marked as main).
        RecipeDetailResponse.RecipeMediaResponse initialMain = mediaItems.stream()
                .filter(RecipeDetailResponse.RecipeMediaResponse::isMain)
                .findFirst()
                .orElse(mediaItems.get(0));

        loadMain(basePath.resolve(initialMain.storageKey()).toUri().toString());

        for (RecipeDetailResponse.RecipeMediaResponse m : mediaItems) {
            String fileUrl = basePath.resolve(m.storageKey()).toUri().toString();

            ImageView thumb = new ImageView(
                    new Image(fileUrl, THUMB_SIZE, THUMB_SIZE, true, true, true));
            thumb.setFitWidth(THUMB_SIZE);
            thumb.setFitHeight(THUMB_SIZE);
            thumb.setPreserveRatio(true);

            StackPane tile = new StackPane(thumb);
            tile.getStyleClass().add("gallery-thumbnail");
            if (m.isMain()) tile.getStyleClass().add("gallery-thumbnail-selected");

            tile.setOnMouseClicked(event -> {
                selectThumbnail(tile);
                loadMain(fileUrl);
            });

            thumbnailStrip.getChildren().add(tile);
        }
    }

    private void loadMain(String fileUrl) {
        mainImageView.setImage(
                new Image(fileUrl, MAIN_W, MAIN_H, true, true, true));
    }

    private void selectThumbnail(StackPane selected) {
        thumbnailStrip.getChildren().forEach(n ->
                n.getStyleClass().remove("gallery-thumbnail-selected"));
        selected.getStyleClass().add("gallery-thumbnail-selected");
    }

    private void showPlaceholder() {
        mainImageView.setImage(null);
        mainImageView.setVisible(false);
        noMediaLabel.setVisible(true);
        thumbnailStrip.getChildren().clear();
    }
}
