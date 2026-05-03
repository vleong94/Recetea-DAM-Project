package com.recetea.infrastructure.ui.javafx.features.recipe.components;

import com.recetea.core.recipe.application.ports.in.dto.RecipeSummaryResponse;
import com.recetea.core.recipe.domain.vo.RecipeId;
import com.recetea.infrastructure.ui.javafx.shared.media.MediaUriResolver;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;
import javafx.util.Duration;

import java.io.IOException;
import java.util.function.Consumer;

/**
 * Single card tile in the dashboard / profile galleries. Renders the
 * cover image, title, rating + favorite toggle, and is fully clickable
 * — the {@link Consumer} passed at construction receives the
 * {@link RecipeId} on click for navigation.
 *
 * <p><b>Skeleton fade-in.</b> The cover image is loaded asynchronously;
 * a {@code skeleton-pulse} {@link Region} is painted in its place
 * until the {@link Image} resolves, then replaced with the decoded
 * thumbnail via a {@link Timeline}. Failed loads keep the skeleton
 * visible — the card never renders a JavaFX broken-image icon.
 *
 * <p>Image dimensions ({@link #IMAGE_W} × {@link #IMAGE_H}) are fixed
 * so the {@link FlowPane} layout in the dashboard doesn't reflow when
 * cards swap between skeleton and image states.
 *
 * <p><b>ES — </b>Tarjeta individual en las galerías del dashboard /
 * perfil. Renderiza la imagen de portada, el título, la valoración
 * y el toggle de favorito, y es completamente clickeable — el
 * {@link Consumer} pasado en la construcción recibe el
 * {@link RecipeId} en el click para la navegación.
 *
 * <p><b>Fade-in del esqueleto.</b> La imagen de portada se carga
 * de forma asíncrona; un {@link Region} con clase
 * {@code skeleton-pulse} se pinta en su lugar hasta que la
 * {@link Image} resuelve, y luego se reemplaza con la miniatura
 * decodificada vía un {@link Timeline}. Las cargas fallidas
 * mantienen el esqueleto visible — la tarjeta nunca renderiza el
 * icono de imagen rota de JavaFX.
 *
 * <p>Las dimensiones de la imagen ({@link #IMAGE_W} ×
 * {@link #IMAGE_H}) son fijas para que el layout {@link FlowPane}
 * del dashboard no se reorganice cuando las tarjetas alternan
 * entre estados de esqueleto e imagen.
 */
public class RecipeCardComponent extends VBox {

    private static final int IMAGE_W = 240;
    private static final int IMAGE_H = 160;

    @FXML private StackPane    imagePane;
    @FXML private Region       imageSkeleton;
    @FXML private ImageView    imageView;
    @FXML private ToggleButton btnFavorite;
    @FXML private SVGPath      favoriteStar;
    @FXML private Label        titleLabel;
    @FXML private Label        categoryLabel;
    @FXML private Label        prepTimeLabel;
    @FXML private Label        scoreLabel;

    private Timeline skeletonAnimation;

    public RecipeCardComponent(RecipeSummaryResponse recipe,
                               Consumer<RecipeId> onCardClick,
                               String basePath,
                               boolean initiallyFavorite,
                               Consumer<RecipeId> onToggleFavorite) {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(
                "/com/recetea/infrastructure/ui/javafx/fxml/features/recipe/components/recipe_card.fxml"));
        loader.setRoot(this);
        loader.setController(this);
        try {
            loader.load();
        } catch (IOException e) {
            throw new RuntimeException("Infrastructure failure: could not instantiate RecipeCardComponent.", e);
        }

        populate(recipe, basePath);
        wireFavoriteToggle(recipe.id(), initiallyFavorite, onToggleFavorite);

        if (onCardClick != null) {
            // Click guard: clicks that originated on the favorite toggle don't bubble up
            // as "open recipe detail" intent. Walk up the target chain to detect.
            setOnMouseClicked(e -> {
                if (eventOriginatedFromFavoriteToggle(e.getTarget())) return;
                onCardClick.accept(recipe.id());
            });
        }
    }

    private void wireFavoriteToggle(RecipeId recipeId, boolean initiallyFavorite,
                                    Consumer<RecipeId> onToggleFavorite) {
        btnFavorite.setSelected(initiallyFavorite);
        refreshStarFill();
        btnFavorite.selectedProperty().addListener((obs, was, isNow) -> {
            refreshStarFill();
            if (onToggleFavorite != null) onToggleFavorite.accept(recipeId);
        });
    }

    /** Star is gold when selected, muted-grey outline when unselected. */
    private void refreshStarFill() {
        favoriteStar.setStyle(btnFavorite.isSelected()
                ? "-fx-fill: app-star-active;"
                : "-fx-fill: app-star-empty;");
    }

    private boolean eventOriginatedFromFavoriteToggle(Object target) {
        if (!(target instanceof Node node)) return false;
        for (Node n = node; n != null && n != this; n = n.getParent()) {
            if (n == btnFavorite) return true;
        }
        return false;
    }

    private void populate(RecipeSummaryResponse recipe, String basePath) {
        titleLabel.setText(recipe.title());
        categoryLabel.setText(recipe.categoryName() != null ? recipe.categoryName() : "");
        prepTimeLabel.setText("⏱ " + recipe.prepTimeMinutes() + " min");
        scoreLabel.setText(formatScore(recipe));

        String fileUrl = MediaUriResolver.resolve(basePath, recipe.mainMediaStorageKey());
        if (fileUrl != null) {
            loadImage(fileUrl);
        } else {
            applyFallback();
        }
    }

    private void loadImage(String fileUrl) {
        startSkeletonPulse();
        Image img = new Image(fileUrl, IMAGE_W, IMAGE_H, true, true, true);

        if (img.getProgress() >= 1.0 && !img.isError()) {
            applyImage(img);
            return;
        }

        img.progressProperty().addListener((obs, prev, progress) -> {
            if (progress.doubleValue() >= 1.0 && !img.isError()) applyImage(img);
        });
        img.errorProperty().addListener((obs, prev, error) -> {
            if (error) applyFallback();
        });
    }

    private void applyImage(Image img) {
        stopSkeletonPulse();
        imageSkeleton.setVisible(false);
        imageView.setImage(img);
        imageView.setVisible(true);
    }

    private void applyFallback() {
        stopSkeletonPulse();
        Image fallback = MediaUriResolver.placeholder();
        if (fallback != null) {
            imageSkeleton.setVisible(false);
            imageView.setImage(fallback);
            imageView.setVisible(true);
        } else {
            imageSkeleton.getStyleClass().add("recipe-card-no-image");
        }
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
