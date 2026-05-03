package com.recetea.infrastructure.ui.javafx.features.recipe.components;

import com.recetea.core.recipe.application.ports.in.dto.RecipeDetailResponse;
import com.recetea.infrastructure.concurrency.ConcurrencyProvider;
import com.recetea.infrastructure.ui.javafx.shared.i18n.I18n;
import com.recetea.infrastructure.ui.javafx.shared.media.MediaUriResolver;
import javafx.concurrent.Task;
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
import java.util.concurrent.ExecutorService;

/**
 * Recipe-form sub-component for attaching images to a recipe. Two
 * sections — already-attached media (read from the loaded aggregate)
 * and pending uploads (selected via a {@link FileChooser}, persisted
 * after the parent recipe is saved).
 *
 * <p><b>5 MB cap enforced on the FX thread.</b> {@link #MAX_BYTES} is
 * checked via {@code file.length()} <em>before</em> spawning the
 * virtual thread that decodes the image. Oversized files never reach
 * the executor — keeps the heavy decode path bounded and gives the
 * user immediate feedback.
 *
 * <p><b>Async thumbnail decode.</b> Each pending file is decoded on
 * the virtual-thread {@link ExecutorService} via {@link Task}; the
 * resulting {@link Image} is shown on the FX thread via
 * {@code setOnSucceeded}. Decode failure surfaces as an inline status
 * label, not an exception dialog.
 *
 * <p><b>ES — </b>Sub-componente del formulario de receta para
 * adjuntar imágenes a una receta. Dos secciones — multimedia ya
 * adjunto (leído del agregado cargado) y subidas pendientes
 * (seleccionadas vía {@link FileChooser}, persistidas después de
 * guardar la receta padre).
 *
 * <p><b>Tope de 5 MB aplicado en el hilo FX.</b> {@link #MAX_BYTES}
 * se comprueba vía {@code file.length()} <em>antes</em> de lanzar
 * el virtual thread que decodifica la imagen. Los archivos de
 * tamaño excesivo nunca llegan al executor — mantiene acotada la
 * ruta de decodificado pesado y da feedback inmediato al usuario.
 *
 * <p><b>Decodificado asíncrono de miniaturas.</b> Cada archivo
 * pendiente se decodifica en el {@link ExecutorService} de
 * virtual threads vía {@link Task}; la {@link Image} resultante
 * se muestra en el hilo FX vía {@code setOnSucceeded}. El fallo
 * de decodificación se manifiesta como una etiqueta de estado
 * inline, no como un diálogo de excepción.
 */
public class MediaUploadComponent extends VBox {

    @FXML private VBox existingSection;
    @FXML private FlowPane existingContainer;
    @FXML private FlowPane pendingContainer;
    @FXML private Label statusLabel;

    private static final long MAX_BYTES  = 5L * 1024 * 1024; // 5 MB
    private static final int  THUMB_SIZE = 80;

    private final List<File>    pendingFiles = new ArrayList<>();
    private final ExecutorService executor;

    /**
     * Storage root injected by the form controller via {@link #init(String)}. Either a
     * local filesystem path (rendered as {@code file:} URI) or an HTTPS public-read URL
     * (concatenated with the storage key) — {@link MediaUriResolver} handles both shapes.
     */
    private String storageBasePath;

    public MediaUploadComponent() {
        this.executor = new ConcurrencyProvider().executor();
        FXMLLoader loader = new FXMLLoader(getClass().getResource(
                "/com/recetea/infrastructure/ui/javafx/fxml/features/recipe/components/media_upload.fxml"));
        loader.setRoot(this);
        loader.setController(this);
        loader.setResources(I18n.bundle());
        try {
            loader.load();
        } catch (IOException e) {
            throw new RuntimeException("Failed to instantiate MediaUploadComponent: FXML load failed.", e);
        }
    }

    // ── Public API ────────────────────────────────────────────

    /**
     * Wires the storage root in. Called by the form controller before any media is loaded —
     * decouples the component from {@code StorageConfig}'s static accessor.
     */
    public void init(String storageBasePath) {
        this.storageBasePath = storageBasePath;
    }

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

        for (var m : mediaItems) {
            String url = MediaUriResolver.resolve(storageBasePath, m.storageKey());
            ImageView iv = new ImageView();
            iv.setFitWidth(THUMB_SIZE);
            iv.setFitHeight(THUMB_SIZE);
            iv.setPreserveRatio(true);

            if (url != null) {
                // backgroundLoading=true so a remote (Supabase) fetch never blocks the FX thread.
                Image img = new Image(url, THUMB_SIZE, THUMB_SIZE, true, true, true);
                iv.setImage(img);
                img.errorProperty().addListener((obs, prev, error) -> {
                    if (error) {
                        Image fallback = MediaUriResolver.placeholder();
                        if (fallback != null) iv.setImage(fallback);
                    }
                });
            } else {
                Image fallback = MediaUriResolver.placeholder();
                if (fallback != null) iv.setImage(fallback);
            }

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

        // Extension check stays on the FX thread — string-only, zero I/O.
        String lowerName = file.getName().toLowerCase();
        if (!lowerName.endsWith(".jpg") && !lowerName.endsWith(".jpeg") && !lowerName.endsWith(".png")) {
            statusLabel.setText(I18n.get("media.upload.error.invalidType"));
            return;
        }

        // Strict size check on the FX thread BEFORE spawning a virtual thread to decode.
        // file.length() is a fast metadata stat (no payload read); rejecting an oversized
        // file here avoids paying for a JavaFX Image decoder allocation we'd discard anyway.
        if (file.length() > MAX_BYTES) {
            statusLabel.setText(I18n.get("media.upload.error.tooLarge"));
            return;
        }

        // Only after both gates pass, offload the synchronous image decode to a virtual thread.
        Task<Image> task = new Task<>() {
            @Override
            protected Image call() {
                // Already off the FX thread: load synchronously (backgroundLoading = false).
                return new Image(file.toURI().toString(), THUMB_SIZE, THUMB_SIZE, true, true, false);
            }
        };

        // setOnSucceeded is dispatched on the FX thread by the Task infrastructure.
        task.setOnSucceeded(e -> {
            Image preview = task.getValue();
            pendingFiles.add(file);
            addPendingTile(file, preview);
            statusLabel.setText(I18n.format("media.upload.status.pending", pendingFiles.size()));
        });

        task.setOnFailed(e -> Thread.getDefaultUncaughtExceptionHandler()
                .uncaughtException(Thread.currentThread(), task.getException()));

        executor.execute(task);
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
            statusLabel.setText(pendingFiles.isEmpty()
                    ? ""
                    : I18n.format("media.upload.status.pending", pendingFiles.size()));
        });

        pendingContainer.getChildren().add(tile);
    }
}
