package com.recetea.infrastructure.ui.javafx.shared.components;

import com.recetea.core.recipe.application.ports.in.dto.RecipeSummaryResponse;
import javafx.scene.control.TableCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class ThumbnailTableCell extends TableCell<RecipeSummaryResponse, String> {

    private static final int MAX_CACHE_SIZE = 100;
    private static final int SIZE = 50;

    // Bounded LRU: evicts the least-recently-accessed entry once the cap is reached.
    private static final Map<String, Image> CACHE = Collections.synchronizedMap(
            new LinkedHashMap<>(MAX_CACHE_SIZE + 1, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, Image> eldest) {
                    return size() > MAX_CACHE_SIZE;
                }
            }
    );

    private final ImageView imageView;
    private final Rectangle placeholder;
    private final Path basePath;

    public ThumbnailTableCell(Path basePath) {
        this.basePath = basePath;

        imageView = new ImageView();
        imageView.setFitWidth(SIZE);
        imageView.setFitHeight(SIZE);
        imageView.setPreserveRatio(true);

        placeholder = new Rectangle(SIZE, SIZE, Color.LIGHTGRAY);
    }

    @Override
    protected void updateItem(String storageKey, boolean empty) {
        super.updateItem(storageKey, empty);

        if (empty || storageKey == null) {
            setGraphic(placeholder);
            return;
        }

        Image cached = CACHE.get(storageKey);
        if (cached != null && !cached.isError()) {
            imageView.setImage(cached);
            setGraphic(imageView);
            return;
        }

        // Show placeholder while background load is in-flight.
        setGraphic(placeholder);

        String fileUrl = basePath.resolve(storageKey).toUri().toString();
        // backgroundLoading=true: I/O happens on a JavaFX background thread; no FX thread blocking.
        Image image = new Image(fileUrl, SIZE, SIZE, true, true, true);
        CACHE.put(storageKey, image);

        // Swap placeholder → imageView once fully loaded, but only if the cell still holds this key.
        image.progressProperty().addListener((obs, prev, progress) -> {
            if (progress.doubleValue() >= 1.0 && !image.isError()
                    && storageKey.equals(getItem())) {
                imageView.setImage(image);
                setGraphic(imageView);
            }
        });

        // Evict broken entries so a retry can happen if the file appears later.
        image.errorProperty().addListener((obs, prev, error) -> {
            if (error) CACHE.remove(storageKey);
        });
    }
}
