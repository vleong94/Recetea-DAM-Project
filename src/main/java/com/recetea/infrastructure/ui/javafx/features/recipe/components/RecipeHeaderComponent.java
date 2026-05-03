package com.recetea.infrastructure.ui.javafx.features.recipe.components;

import com.recetea.core.recipe.domain.Category;
import com.recetea.core.recipe.domain.Difficulty;
import com.recetea.core.recipe.domain.vo.CategoryId;
import com.recetea.core.recipe.domain.vo.DifficultyId;
import com.recetea.infrastructure.ui.javafx.shared.i18n.I18n;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.util.List;
import java.util.function.UnaryOperator;

/**
 * Recipe-form sub-component covering the parent-row fields (title,
 * description, prep time, servings, category, difficulty). Wraps two
 * catalogue {@link ComboBox}es and four text inputs.
 *
 * <p><b>Numeric input filters.</b> {@code prepTimeField} and
 * {@code servingsField} carry {@link TextFormatter}s that reject any
 * non-digit keystroke before it lands in the model — so a numeric
 * field always parses cleanly when the form submits, no
 * {@code NumberFormatException} risk at save time.
 *
 * <p><b>Read interface.</b> Exposes per-field {@link StringProperty}
 * getters so the parent form can validate and build the
 * {@code SaveRecipeRequest} without re-querying the controls; the
 * component owns no validation rules of its own — those belong to
 * {@code SaveRecipeRequest.validate()}.
 *
 * <p><b>ES — </b>Sub-componente del formulario de receta que cubre
 * los campos de la fila padre (título, descripción, tiempo de
 * preparación, raciones, categoría, dificultad). Envuelve dos
 * {@link ComboBox} de catálogo y cuatro entradas de texto.
 *
 * <p><b>Filtros de entrada numérica.</b> {@code prepTimeField} y
 * {@code servingsField} llevan {@link TextFormatter} que rechazan
 * cualquier tecla no numérica antes de que aterrice en el modelo
 * — así un campo numérico siempre parsea limpiamente cuando el
 * formulario se envía, sin riesgo de
 * {@code NumberFormatException} al guardar.
 *
 * <p><b>Interfaz de lectura.</b> Expone getters
 * {@link StringProperty} por campo para que el formulario padre
 * pueda validar y construir el {@code SaveRecipeRequest} sin
 * volver a consultar los controles; el componente no tiene reglas
 * de validación propias — esas pertenecen a
 * {@code SaveRecipeRequest.validate()}.
 */
public class RecipeHeaderComponent extends VBox {

    @FXML private TextField titleField;
    @FXML private TextField prepTimeField;
    @FXML private TextField servingsField;
    @FXML private TextArea descriptionArea;

    @FXML private ComboBox<Category> categoryComboBox;
    @FXML private ComboBox<Difficulty> difficultyComboBox;

    public RecipeHeaderComponent() {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/recetea/infrastructure/ui/javafx/fxml/features/recipe/components/recipe_header.fxml"));
        loader.setRoot(this);
        loader.setController(this);
        loader.setResources(I18n.bundle());
        try {
            loader.load();
        } catch (IOException e) {
            throw new RuntimeException("Infrastructure failure: could not instantiate RecipeHeaderComponent.", e);
        }
    }

    @FXML
    private void initialize() {
        UnaryOperator<TextFormatter.Change> positiveIntFilter = change -> {
            String text = change.getControlNewText();
            return (text.isEmpty() || text.matches("[1-9][0-9]*")) ? change : null;
        };
        prepTimeField.setTextFormatter(new TextFormatter<>(positiveIntFilter));
        servingsField.setTextFormatter(new TextFormatter<>(positiveIntFilter));
    }

    public void requestTitleFocus() {
        titleField.requestFocus();
    }

    public void initTaxonomy(List<Category> categories, List<Difficulty> difficulties) {
        if (categories != null) categoryComboBox.getItems().setAll(categories);
        if (difficulties != null) difficultyComboBox.getItems().setAll(difficulties);
    }

    public boolean isValid() {
        String title = titleField.getText();
        if (title == null || title.trim().isEmpty()) return false;
        if (categoryComboBox.getValue() == null) return false;
        if (difficultyComboBox.getValue() == null) return false;
        try {
            int prep = Integer.parseInt(prepTimeField.getText().trim());
            int serv = Integer.parseInt(servingsField.getText().trim());
            return prep > 0 && serv > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public String getTitle() { return titleField.getText().trim(); }

    /** Live property exposed for {@code BooleanBinding} aggregation in the form controller. */
    public StringProperty titleProperty() { return titleField.textProperty(); }

    public String getDescription() {
        return descriptionArea.getText() != null ? descriptionArea.getText().trim() : "";
    }

    public int getPrepTime() { return Integer.parseInt(prepTimeField.getText().trim()); }

    public int getServings() { return Integer.parseInt(servingsField.getText().trim()); }

    public CategoryId getSelectedCategoryId() {
        return categoryComboBox.getValue().id();
    }

    public DifficultyId getSelectedDifficultyId() {
        return difficultyComboBox.getValue().id();
    }

    public void setData(String title, String description, int prepTime, int servings,
                        CategoryId categoryId, DifficultyId difficultyId) {
        titleField.setText(title);
        descriptionArea.setText(description);
        prepTimeField.setText(String.valueOf(prepTime));
        servingsField.setText(String.valueOf(servings));

        categoryComboBox.getItems().stream()
                .filter(c -> c.id().equals(categoryId))
                .findFirst()
                .ifPresent(categoryComboBox::setValue);

        difficultyComboBox.getItems().stream()
                .filter(d -> d.id().equals(difficultyId))
                .findFirst()
                .ifPresent(difficultyComboBox::setValue);
    }
}
