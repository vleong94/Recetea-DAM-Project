package com.recetea.infrastructure.ui.javafx.features.recipe.controllers;

import com.recetea.core.recipe.application.ports.in.dto.SaveRecipeRequest;
import com.recetea.core.recipe.domain.Category;
import com.recetea.core.recipe.domain.Difficulty;
import com.recetea.core.recipe.domain.vo.RecipeId;
import com.recetea.core.shared.domain.ValidationResult;
import com.recetea.infrastructure.ui.javafx.features.recipe.RecipeCommandProvider;
import com.recetea.infrastructure.ui.javafx.features.recipe.components.IngredientTableComponent;
import com.recetea.infrastructure.ui.javafx.features.recipe.components.MediaUploadComponent;
import com.recetea.infrastructure.ui.javafx.features.recipe.components.RecipeHeaderComponent;
import com.recetea.infrastructure.ui.javafx.features.recipe.components.StepTableComponent;
import com.recetea.infrastructure.ui.javafx.shared.i18n.I18n;
import com.recetea.core.shared.application.ports.in.INavigationPort;
import com.recetea.infrastructure.ui.javafx.shared.notification.NotificationService;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Shared scaffolding for the recipe form. Both create and update flows
 * load the same {@code recipe_form.fxml}, so the layout binding +
 * sub-component wiring + save orchestration lives here, while the
 * subclasses ({@link RecipeCreateController}, {@link RecipeUpdateController})
 * implement only the two abstract hooks:
 *
 * <ul>
 *   <li>{@link #setupMode()} — called first in {@code init(...)}; sets
 *       the locale-aware title/button text.</li>
 *   <li>{@link #handleSave(SaveRecipeRequest)} — invoked by
 *       {@code onSaveButtonClick()} after structural validation; returns
 *       the recipe id (newly assigned for create, the existing id for
 *       update). The id is then handed to {@code attachPendingMedia(...)}
 *       so the form's just-uploaded files are persisted against it.</li>
 * </ul>
 *
 * <p><b>Programmatic controller injection.</b> The form FXML does NOT
 * declare {@code fx:controller} — the controller object is set
 * programmatically via {@code FXMLLoader.setController(this)} before
 * {@code load()}. This is what lets {@link RecipeCreateController} and
 * {@link RecipeUpdateController} share the same FXML while presenting
 * different controllers; {@code @FXML} field injection still works
 * because the loader injects into whichever controller it sees set.
 *
 * <p><b>ES — </b>Scaffolding compartido para el formulario de
 * receta. Tanto los flujos de creación como de actualización
 * cargan el mismo {@code recipe_form.fxml}, así que el binding de
 * layout, el cableado de sub-componentes y la orquestación del
 * save viven aquí, mientras que las subclases
 * ({@link RecipeCreateController}, {@link RecipeUpdateController})
 * implementan sólo los dos hooks abstractos:
 *
 * <ul>
 *   <li>{@link #setupMode()} — se llama primero en {@code init(...)};
 *       fija el título / texto de botón consciente del locale.</li>
 *   <li>{@link #handleSave(SaveRecipeRequest)} — lo invoca
 *       {@code onSaveButtonClick()} tras la validación estructural;
 *       devuelve el id de la receta (recién asignado en creación,
 *       el id existente en actualización). El id se entrega luego
 *       a {@code attachPendingMedia(...)} para que los archivos
 *       recién subidos del formulario se persistan contra él.</li>
 * </ul>
 *
 * <p><b>Inyección programática del controlador.</b> El FXML del
 * formulario NO declara {@code fx:controller} — el controlador se
 * fija programáticamente vía
 * {@code FXMLLoader.setController(this)} antes de {@code load()}.
 * Eso permite a {@link RecipeCreateController} y
 * {@link RecipeUpdateController} compartir el mismo FXML mientras
 * presentan controladores distintos; la inyección de campos
 * {@code @FXML} sigue funcionando porque el loader inyecta en el
 * controlador que vea fijado.
 */
public abstract class BaseRecipeFormController {

    @FXML protected Label formTitleLabel;
    @FXML protected Button submitButton;
    @FXML protected RecipeHeaderComponent headerComponent;
    @FXML protected IngredientTableComponent ingredientTableComponent;
    @FXML protected StepTableComponent stepTableComponent;
    @FXML protected MediaUploadComponent mediaUploadComponent;

    protected RecipeCommandProvider context;
    protected INavigationPort nav;

    public void init(RecipeCommandProvider context, INavigationPort nav, String storageBasePath) {
        this.context = context;
        this.nav = nav;
        mediaUploadComponent.init(storageBasePath);
        setupMode();

        List<Category> categories = context.getAllCategories().execute();
        List<Difficulty> difficulties = context.getAllDifficulties().execute();
        headerComponent.initTaxonomy(categories, difficulties);
        ingredientTableComponent.init(
                context.getAllIngredients().execute(),
                context.getAllUnits().execute());

        // Single source of truth for "is the form save-ready". Aggregates the three
        // hard requirements: non-blank title, ≥1 ingredient, ≥1 step. The submit
        // button's disable state tracks this binding live, so the user can't even
        // attempt to save an obviously-incomplete form. The deeper SaveRecipeRequest
        // validation in onSaveButtonClick stays as defence-in-depth (catches things
        // like negative prep time or duplicate ingredients).
        BooleanBinding isFormValid = Bindings.createBooleanBinding(
                () -> {
                    String title = headerComponent.titleProperty().getValueSafe();
                    return !title.isBlank()
                            && !ingredientTableComponent.getIngredientItems().isEmpty()
                            && !stepTableComponent.getStepItems().isEmpty();
                },
                headerComponent.titleProperty(),
                ingredientTableComponent.getIngredientItems(),
                stepTableComponent.getStepItems());
        submitButton.disableProperty().bind(isFormValid.not());

        Platform.runLater(headerComponent::requestTitleFocus);
    }

    @FXML
    public void onSaveButtonClick() {
        if (!headerComponent.isValid()) {
            showError(I18n.get("form.error.header.required"));
            return;
        }

        List<SaveRecipeRequest.IngredientRequest> ingredients = ingredientTableComponent.getIngredients();
        if (ingredients.isEmpty()) {
            showError(I18n.get("form.error.noIngredients"));
            return;
        }

        List<SaveRecipeRequest.StepRequest> steps = stepTableComponent.getStepRequests();
        if (steps.isEmpty()) {
            showError(I18n.get("form.error.noSteps"));
            return;
        }

        SaveRecipeRequest request = new SaveRecipeRequest(
                headerComponent.getSelectedCategoryId(),
                headerComponent.getSelectedDifficultyId(),
                headerComponent.getTitle(),
                headerComponent.getDescription(),
                headerComponent.getPrepTime(),
                headerComponent.getServings(),
                ingredients,
                steps
        );

        ValidationResult<Void> validation = request.validate();
        if (!validation.isValid()) {
            String message = validation.errors().stream()
                    .map(BaseRecipeFormController::resolveErrorMessage)
                    .collect(Collectors.joining("\n"));
            showError(message);
            return;
        }

        RecipeId savedId = handleSave(request);
        attachPendingMedia(savedId);
        nav.toDashboard();
    }

    /** Errors prefixed with {@code "error."} are i18n keys; everything else is a literal user-facing message. */
    private static String resolveErrorMessage(String error) {
        return error != null && error.startsWith("error.") ? I18n.get(error) : error;
    }

    /** Persists the recipe and returns its identity (new or existing). */
    protected abstract RecipeId handleSave(SaveRecipeRequest request);

    /** Sets mode-specific title and submit button text. Called once at init time. */
    protected abstract void setupMode();

    @FXML
    public void onBackButtonClick() {
        nav.toDashboard();
    }

    protected void showError(String message) {
        NotificationService.warning(headerComponent, message);
    }

    // ── Private ───────────────────────────────────────────────

    private void attachPendingMedia(RecipeId recipeId) {
        List<File> pending = mediaUploadComponent.getPendingFiles();
        if (pending.isEmpty()) return;
        for (File file : pending) {
            try (FileInputStream fis = new FileInputStream(file)) {
                context.attachMedia().execute(recipeId, fis, file.getName());
            } catch (IOException e) {
                throw new RuntimeException(
                        I18n.format("media.error.fileRead", file.getName()), e);
            }
        }
        mediaUploadComponent.clearPending();
    }
}
