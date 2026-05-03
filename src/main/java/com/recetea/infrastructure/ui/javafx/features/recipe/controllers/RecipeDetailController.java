package com.recetea.infrastructure.ui.javafx.features.recipe.controllers;

import com.recetea.core.recipe.application.ports.in.dto.RecipeDetailResponse;
import com.recetea.core.recipe.application.ports.in.dto.RecipeIngredientResponse;
import com.recetea.core.recipe.domain.vo.RecipeId;
import com.recetea.infrastructure.ui.javafx.features.recipe.RecipeCommandProvider;
import com.recetea.infrastructure.ui.javafx.features.recipe.RecipeQueryProvider;
import com.recetea.infrastructure.ui.javafx.features.recipe.components.CommentItemComponent;
import com.recetea.infrastructure.ui.javafx.features.recipe.components.MediaGalleryComponent;
import com.recetea.infrastructure.ui.javafx.features.recipe.components.RatingComponent;
import com.recetea.infrastructure.ui.javafx.shared.i18n.I18n;
import com.recetea.core.shared.application.ports.in.INavigationPort;
import com.recetea.infrastructure.ui.javafx.shared.notification.NotificationService;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;
import javafx.stage.FileChooser;

import java.io.File;
import java.nio.file.Files;
import java.util.concurrent.ExecutorService;

/**
 * Controller for the read-only recipe detail page. Hydrates a
 * {@link RecipeDetailResponse} via {@code IGetRecipeByIdUseCase} on
 * navigation entry, then renders header, ingredients chip-strip,
 * step list, media gallery, rating component, and ratings list.
 *
 * <p><b>Ownership-driven UI.</b> The detail page exposes destructive
 * actions (edit, delete, export, attach media) only when the session
 * user owns the recipe. Ownership is checked once when the response
 * lands; non-owners see the relevant buttons hidden and the
 * "you don't own this recipe" placeholder rendered in the action bar.
 *
 * <p><b>Rating gate.</b> The {@code alreadyRatedByCurrentUser} flag on
 * the response controls the {@link RatingComponent} state — already-
 * rated users see a disabled control. The DB-level vote-uniqueness
 * constraint is the authoritative guarantee; this flag is UX-only.
 *
 * <p><b>Async work.</b> PDF generation, XML export, and media
 * download all dispatch through the virtual-thread {@link ExecutorService}
 * via {@link Task}, with success/error handlers running on the FX thread.
 * The user can navigate away mid-task; the {@link Task} completes
 * harmlessly because the success handler checks the controller's
 * still-mounted state before mutating UI.
 *
 * <p><b>ES — </b>Controlador para la página de detalle de receta de
 * sólo lectura. Hidrata un {@link RecipeDetailResponse} vía
 * {@code IGetRecipeByIdUseCase} al entrar en la navegación, y
 * luego renderiza la cabecera, la tira de chips de ingredientes,
 * la lista de pasos, la galería multimedia, el componente de
 * valoración y la lista de valoraciones.
 *
 * <p><b>UI guiada por propiedad.</b> La página de detalle expone
 * acciones destructivas (editar, eliminar, exportar, adjuntar
 * multimedia) sólo cuando el usuario de sesión es propietario de
 * la receta. La propiedad se comprueba una vez al llegar la
 * respuesta; los no-propietarios ven los botones correspondientes
 * ocultos y el placeholder "no eres dueño de esta receta"
 * renderizado en la barra de acciones.
 *
 * <p><b>Gate de valoración.</b> El flag
 * {@code alreadyRatedByCurrentUser} de la respuesta controla el
 * estado del {@link RatingComponent} — los usuarios que ya han
 * valorado ven un control deshabilitado. La restricción de
 * unicidad de voto a nivel de BD es la garantía autoritativa;
 * este flag es sólo UX.
 *
 * <p><b>Trabajo asíncrono.</b> La generación de PDF, la
 * exportación XML y la descarga de multimedia se despachan todas
 * a través del {@link ExecutorService} de virtual threads vía
 * {@link Task}, con los handlers de éxito/error corriendo en el
 * hilo FX. El usuario puede navegar fuera durante la tarea; la
 * {@link Task} termina sin daño porque el handler de éxito
 * comprueba si el controlador sigue montado antes de mutar la UI.
 */
public class RecipeDetailController {

    @FXML private Label titleLabel;
    @FXML private Label prepTimeLabel;
    @FXML private Label servingsLabel;
    @FXML private Label descriptionLabel;
    @FXML private Label categoryLabel;
    @FXML private Label difficultyLabel;
    @FXML private Label scoreLabel;
    @FXML private Label authorLabel;
    @FXML private Label ownershipMessage;

    @FXML private ScrollPane mainContentScroll;
    @FXML private FlowPane   ingredientsContainer;
    @FXML private VBox       stepsContainer;

    @FXML private MediaGalleryComponent mediaGallery;
    @FXML private RatingComponent ratingComponent;
    @FXML private Separator       ratingSectionSpacer;
    @FXML private ToggleButton favoriteButton;
    @FXML private SVGPath      favoriteStar;
    @FXML private Button btnEdit;
    @FXML private VBox  reviewsList;
    @FXML private Label noReviewsMessage;

    private RecipeQueryProvider   queryProvider;
    private RecipeCommandProvider commandProvider;
    private INavigationPort       nav;
    private ExecutorService       executor;
    private RecipeId              currentRecipeId;
    private RecipeDetailResponse  currentRecipe;
    private String                currentTitle = "";

    /** Storage root for media URI resolution; injected via {@link #init}. */
    private String storageBasePath;

    public void init(RecipeQueryProvider queryProvider, RecipeCommandProvider commandProvider,
                     INavigationPort nav, ExecutorService executor, String storageBasePath) {
        this.queryProvider   = queryProvider;
        this.commandProvider = commandProvider;
        this.nav             = nav;
        this.executor        = executor;
        this.storageBasePath = storageBasePath;

        // Single visual driver: text + style class + star fill all derive from selectedProperty.
        favoriteButton.selectedProperty().addListener((obs, was, isNow) -> applyFavoriteStyle(isNow));
        applyFavoriteStyle(false);

        favoriteButton.setOnAction(e -> {
            commandProvider.toggleFavorite().execute(currentRecipeId);
            refreshFavoriteButton(currentRecipeId);
        });
    }

    /**
     * Mirror of selectedProperty into the toggle's three visual surfaces:
     * <ul>
     *   <li>text — i18n key {@code recipe.detail.button.inFavorites} vs {@code recipe.detail.button.addFavorite}</li>
     *   <li>style class — {@code warning} (yellow background) vs {@code secondary-button}
     *       (bordered neutral)</li>
     *   <li>star fill — dark default fg (visible on yellow) vs muted grey (visible on neutral)</li>
     * </ul>
     */
    private void applyFavoriteStyle(boolean isFav) {
        favoriteButton.setText(I18n.get(isFav
                ? "recipe.detail.button.inFavorites"
                : "recipe.detail.button.addFavorite"));
        favoriteButton.getStyleClass().removeAll("secondary-button", "warning");
        favoriteButton.getStyleClass().add(isFav ? "warning" : "secondary-button");
        favoriteStar.setStyle(isFav
                ? "-fx-fill: -color-fg-default;"
                : "-fx-fill: app-star-empty;");
    }

    public void loadRecipeDetails(RecipeId recipeId) {
        this.currentRecipeId = recipeId;
        mainContentScroll.setVvalue(0);
        ratingComponent.setRecipeContext(commandProvider, recipeId, () -> loadRecipeDetails(currentRecipeId));

        Task<DetailData> task = new Task<>() {
            @Override
            protected DetailData call() {
                boolean isFav = commandProvider.isFavorite().execute(recipeId);
                RecipeDetailResponse recipe = queryProvider.getRecipeById().execute(recipeId).orElse(null);
                return new DetailData(isFav, recipe);
            }
        };
        task.setOnSucceeded(e -> {
            DetailData data = task.getValue();
            // Listener on selectedProperty handles text + style + star fill.
            favoriteButton.setSelected(data.isFav());
            if (data.recipe() != null) {
                populateView(data.recipe());
            } else {
                NotificationService.error(titleLabel, I18n.get("recipe.detail.error.notFound"));
            }
        });
        task.setOnFailed(e -> Thread.getDefaultUncaughtExceptionHandler()
                .uncaughtException(Thread.currentThread(), task.getException()));
        executor.execute(task);
    }

    private void refreshFavoriteButton(RecipeId recipeId) {
        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() {
                return commandProvider.isFavorite().execute(recipeId);
            }
        };
        task.setOnSucceeded(e -> favoriteButton.setSelected(task.getValue()));
        task.setOnFailed(e -> Thread.getDefaultUncaughtExceptionHandler()
                .uncaughtException(Thread.currentThread(), task.getException()));
        executor.execute(task);
    }

    private void populateView(RecipeDetailResponse recipe) {
        currentRecipe = recipe;
        currentTitle = recipe.title();
        titleLabel.setText(recipe.title());
        prepTimeLabel.setText(String.format("%d min", recipe.prepTimeMinutes()));
        servingsLabel.setText(String.valueOf(recipe.servings()));
        descriptionLabel.setText(recipe.description());
        categoryLabel.setText(recipe.categoryName());
        difficultyLabel.setText(recipe.difficultyName());
        scoreLabel.setText(I18n.format("recipe.detail.meta.scoreFormat",
                recipe.averageScore().setScale(1, java.math.RoundingMode.HALF_UP),
                recipe.totalRatings()));
        authorLabel.setText(recipe.authorUsername() != null
                ? recipe.authorUsername()
                : I18n.get("recipe.detail.author.unknown"));

        mediaGallery.setMedia(recipe.media(), storageBasePath);

        buildIngredientChips(recipe);
        buildStepCards(recipe);

        if (recipe.alreadyRatedByCurrentUser()) {
            ratingComponent.disableWithStatus(I18n.get("rating.disabled.alreadyRated"));
        }
        boolean isAuthor = commandProvider.sessionService().getCurrentUserId()
                .filter(id -> id.equals(recipe.userId()))
                .isPresent();
        btnEdit.setVisible(isAuthor);
        btnEdit.setManaged(isAuthor);
        // "Esta receta es tuya" — managed mirrors visibility so the row collapses for non-owners.
        ownershipMessage.setVisible(isAuthor);
        ownershipMessage.setManaged(isAuthor);
        // Authors don't vote on their own recipe — collapse the interactive rating component
        // and its preceding spacer entirely. The averageScore label and reviews list stay
        // visible so the author can still see how the community values their work.
        ratingComponent.setVisible(!isAuthor);
        ratingComponent.setManaged(!isAuthor);
        ratingSectionSpacer.setVisible(!isAuthor);
        ratingSectionSpacer.setManaged(!isAuthor);

        reviewsList.getChildren().clear();
        recipe.ratings().forEach(r -> reviewsList.getChildren().add(new CommentItemComponent(r)));
        // Empty-state swap — list collapses, the "no reviews yet" placeholder takes its place
        // (i18n key {@code recipe.detail.reviews.empty}).
        // Triggered automatically on rating submission via the loadRecipeDetails callback in
        // RatingComponent.setRecipeContext, so the message disappears as soon as the first
        // review lands.
        boolean hasReviews = !recipe.ratings().isEmpty();
        reviewsList.setVisible(hasReviews);
        reviewsList.setManaged(hasReviews);
        noReviewsMessage.setVisible(!hasReviews);
        noReviewsMessage.setManaged(!hasReviews);
    }

    private void buildIngredientChips(RecipeDetailResponse recipe) {
        ingredientsContainer.getChildren().clear();
        for (RecipeIngredientResponse ing : recipe.ingredients()) {
            String qty  = ing.quantity().stripTrailingZeros().toPlainString();
            Label  chip = new Label(qty + " " + ing.unitName() + "  ·  " + ing.ingredientName());
            chip.getStyleClass().add("ingredient-chip");
            ingredientsContainer.getChildren().add(chip);
        }
    }

    private void buildStepCards(RecipeDetailResponse recipe) {
        stepsContainer.getChildren().clear();
        for (RecipeDetailResponse.RecipeStepResponse step : recipe.steps()) {
            VBox card = new VBox(6);
            card.getStyleClass().add("step-detail-card");

            Label num = new Label(I18n.format("recipe.detail.step.label", step.stepOrder()));
            num.getStyleClass().add("step-detail-number");

            Label instr = new Label(step.instruction());
            instr.setWrapText(true);
            instr.getStyleClass().add("step-detail-instruction");

            card.getChildren().addAll(num, instr);
            stepsContainer.getChildren().add(card);
        }
    }

    @FXML
    public void onGeneratePdfClick() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(I18n.get("dialog.savePdf.title"));
        chooser.setInitialFileName(sanitizeFilename(safeFilenameBase()) + ".pdf");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(I18n.get("dialog.filter.pdf"), "*.pdf"));

        File file = chooser.showSaveDialog(titleLabel.getScene().getWindow());
        if (file == null) return;

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                byte[] bytes = commandProvider.generateTechnicalSheet().execute(currentRecipeId);
                Files.write(file.toPath(), bytes);
                return null;
            }
        };
        task.setOnSucceeded(e ->
                NotificationService.success(titleLabel,
                        I18n.format("recipe.detail.notification.pdfSaved", file.getName())));
        task.setOnFailed(e -> Thread.getDefaultUncaughtExceptionHandler()
                .uncaughtException(Thread.currentThread(), task.getException()));
        executor.execute(task);
    }

    @FXML
    public void onExportButtonClick() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(I18n.get("dialog.exportXml.title"));
        chooser.setInitialFileName(sanitizeFilename(safeFilenameBase()) + ".xml");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(I18n.get("dialog.filter.xml"), "*.xml"));

        File file = chooser.showSaveDialog(titleLabel.getScene().getWindow());
        if (file == null) return;

        commandProvider.exportRecipe().execute(currentRecipeId, file);
        NotificationService.success(titleLabel, I18n.format("recipe.detail.notification.xmlExported", file.getName()));
    }

    private String safeFilenameBase() {
        return currentTitle.isBlank() ? I18n.get("recipe.detail.defaultFilename") : currentTitle;
    }

    private static String sanitizeFilename(String title) {
        return title.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
    }

    @FXML
    public void onEditClick() {
        if (currentRecipe != null) nav.toRecipeUpdate(currentRecipe);
    }

    @FXML
    public void onBackButtonClick() {
        nav.toDashboard();
    }

    private record DetailData(boolean isFav, RecipeDetailResponse recipe) {}
}
