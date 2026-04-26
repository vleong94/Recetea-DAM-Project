package com.recetea.infrastructure.ui.javafx.features.recipe.components;

import com.recetea.core.recipe.application.ports.in.dto.RecipeDetailResponse;
import com.recetea.infrastructure.storage.StorageConfig;
import com.recetea.infrastructure.ui.javafx.shared.i18n.I18n;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MediaUploadComponent extends VBox {

    @FXML private VBox existingSection;
    @FXML private FlowPane existingContainer;
    @FXML private FlowPane pendingContainer;
    @FXML private Label statusLabel;

    private static final long MAX_BYTES = 5L * 1024 * 1024; // 5 MB
    private static final int  THUMB_SIZE = 80;

    private final List<File> pendingFiles = new ArrayList<>();

    public MediaUploadComponent() {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(
                "/com/recetea/infrastructure/ui/javafx/fxml/features/recipe/components/media_upload.fxml"));
        loader.setRoot(this);
        loader.setController(this);
        loader.setResources(I18n.bundle());
        try {
            loader.load();
        } catch (IOException e) {
            throw new RuntimeException("Infrastructure Failure: Imposible instanciar MediaUploadComponent.", e);
        }
    }

    // ── Public API ────────────────────────────────────────────

    /** Populates the "existing images" section — call from update form after loading recipe data. */
    public void loadExistingMedia(List<RecipeDetailResponse.RecipeMediaResponse> mediaItems) {
        existingContainer.getChildren().clear();
        if (mediaItems == null || mediaItems.isEmpty()) {
            existingSection.setVisible(false);
            existingSection.setManaged(false);
            return;
        }

        existingSection.setVisible(true);
        existingSection.setManaged(true);

        var basePath = StorageConfig.getBasePath();
        for (var m : mediaItems) {
            String url = basePath.resolve(m.storageKey()).toUri().toString();
            ImageView iv = new ImageView(new Image(url, THUMB_SIZE, THUMB_SIZE, true, true, true));
            iv.setFitWidth(THUMB_SIZE);
            iv.setFitHeight(THUMB_SIZE);
            iv.setPreserveRatio(true);

            StackPane tile = new StackPane(iv);
            tile.getStyleClass().add("gallery-thumbnail");
            existingContainer.getChildren().add(tile);
        }
    }

    /** Returns the files the user selected but has not yet saved. Unmodifiable. */
    public List<File> getPendingFiles() {
        return Collections.unmodifiableList(pendingFiles);
    }

    /** Called by the form controller after a successful save to reset state. */
    public void clearPending() {
        pendingFiles.clear();
        pendingContainer.getChildren().clear();
        statusLabel.setText("");
    }

    // ── FXML handler ─────────────────────────────────────────

    @FXML
    private void onAddImageClick() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(I18n.get("dialog.selectImage.title"));
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(I18n.get("dialog.filter.images"), "*.jpg", "*.jpeg", "*.png"));

        File file = chooser.showOpenDialog(getScene().getWindow());
        if (file == null) return;

        // ── Validation (file-stat I/O, still fast) ──────────
        String lowerName = file.getName().toLowerCase();
        if (!lowerName.endsWith(".jpg") && !lowerName.endsWith(".jpeg") && !lowerName.endsWith(".png")) {
            Platform.runLater(() -> statusLabel.setText(I18n.get("media.upload.error.invalidType")));
            return;
        }
        if (file.length() > MAX_BYTES) {
            Platform.runLater(() -> statusLabel.setText(I18n.get("media.upload.error.tooLarge")));
            return;
        }

        pendingFiles.add(file);

        // Build a preview tile and update the UI after the I/O validation.
        Image preview = new Image(file.toURI().toString(), THUMB_SIZE, THUMB_SIZE, true, true, true);
        Platform.runLater(() -> {
            addPendingTile(file, preview);
            statusLabel.setText(I18n.format("media.upload.status.pending", pendingFiles.size()));
        });
    }

    // ── Private helpers ───────────────────────────────────────

    private void addPendingTile(File file, Image preview) {
        ImageView iv = new ImageView(preview);
        iv.setFitWidth(THUMB_SIZE);
        iv.setFitHeight(THUMB_SIZE);
        iv.setPreserveRatio(true);

        Button removeBtn = new Button("×");
        removeBtn.getStyleClass().add("media-remove-btn");
        StackPane.setAlignment(removeBtn, Pos.TOP_RIGHT);

        StackPane tile = new StackPane(iv, removeBtn);
        tile.getStyleClass().add("gallery-thumbnail");

        removeBtn.setOnAction(e -> {
            pendingFiles.remove(file);
            pendingContainer.getChildren().remove(tile);
            statusLabel.setText(pendingFiles.isEmpty() ? "" : I18n.format("media.upload.status.pending", pendingFiles.size()));
        });

        pendingContainer.getChildren().add(tile);
    }
}
