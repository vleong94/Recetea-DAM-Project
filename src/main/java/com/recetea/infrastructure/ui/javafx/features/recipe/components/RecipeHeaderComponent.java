package com.recetea.infrastructure.ui.javafx.features.recipe.components;

import com.recetea.core.recipe.domain.Category;
import com.recetea.core.recipe.domain.Difficulty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.util.List;

/**
 * Componente visual atómico especializado en la captura y validación de metadatos de cabecera
 * y taxonomía de dominio. Actúa como un Inbound Adapter en la capa de UI, garantizando que
 * la información base y las dependencias taxonómicas (Category y Difficulty) cumplan con
 * las reglas de negocio antes de su propagación al Core.
 */
public class RecipeHeaderComponent extends VBox {

    @FXML private TextField titleField;
    @FXML private TextField prepTimeField;
    @FXML private TextField servingsField;
    @FXML private TextArea descriptionArea;

    @FXML private ComboBox<Category> categoryComboBox;
    @FXML private ComboBox<Difficulty> difficultyComboBox;

    /**
     * Inicializa el componente mediante Auto-Binding.
     * Instancia el grafo de escena vinculando el recurso FXML estructural,
     * encapsulando el layout y comportamiento dentro de esta misma clase.
     */
    public RecipeHeaderComponent() {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/recetea/infrastructure/ui/javafx/fxml/features/recipe/components/recipe_header.fxml"));
        loader.setRoot(this);
        loader.setController(this);
        try {
            loader.load();
        } catch (IOException e) {
            throw new RuntimeException("Infrastructure Failure: Imposible instanciar el componente visual RecipeHeaderComponent.", e);
        }
    }

    /**
     * Hidrata los selectores de taxonomía dinámica con los datos maestros del sistema.
     * Desacopla la vista de la carga de datos, delegando el suministro de las colecciones
     * al Controller orquestador para mantener la cohesión del componente.
     *
     * @param categories Colección inmutable de clasificaciones disponibles.
     * @param difficulties Colección inmutable de niveles de complejidad técnicos.
     */
    public void initTaxonomy(List<Category> categories, List<Difficulty> difficulties) {
        if (categories != null) {
            categoryComboBox.getItems().setAll(categories);
        }
        if (difficulties != null) {
            difficultyComboBox.getItems().setAll(difficulties);
        }
    }

    /**
     * Ejecuta una auditoría de State sobre los nodos de entrada.
     * Garantiza la presencia de datos obligatorios, la selección taxonómica efectiva
     * y la coherencia matemática de los numéricos mediante Type Parsing seguro (Fail-Fast).
     *
     * @return true si el estado visual satisface los requerimientos para el DTO.
     */
    public boolean isValid() {
        String title = titleField.getText();
        if (title == null || title.trim().isEmpty()) {
            return false;
        }

        if (categoryComboBox.getValue() == null) {
            return false;
        }

        if (difficultyComboBox.getValue() == null) {
            return false;
        }

        try {
            int prep = Integer.parseInt(prepTimeField.getText().trim());
            int serv = Integer.parseInt(servingsField.getText().trim());
            return prep > 0 && serv > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Extrae el descriptor principal sanitizado.
     */
    public String getTitle() {
        return titleField.getText().trim();
    }

    /**
     * Extrae el bloque de texto descriptivo, garantizando un retorno no nulo.
     */
    public String getDescription() {
        return descriptionArea.getText() != null ? descriptionArea.getText().trim() : "";
    }

    /**
     * Extrae y convierte la métrica temporal al tipo primitivo requerido.
     */
    public int getPrepTime() {
        return Integer.parseInt(prepTimeField.getText().trim());
    }

    /**
     * Extrae y convierte la métrica de raciones al tipo primitivo requerido.
     */
    public int getServings() {
        return Integer.parseInt(servingsField.getText().trim());
    }

    /**
     * Recupera el identificador único de la entidad Category seleccionada en la UI.
     */
    public int getSelectedCategoryId() {
        return categoryComboBox.getValue().getId().value();
    }

    /**
     * Recupera el identificador único de la entidad Difficulty seleccionada en la UI.
     */
    public int getSelectedDifficultyId() {
        return difficultyComboBox.getValue().getId().value();
    }

    /**
     * Hidrata el componente con el State de una entidad preexistente.
     * Sincroniza los valores inmutables del Core hacia las propiedades mutables de la vista,
     * iterando sobre las colecciones maestras para establecer el foco visual correcto en los ComboBox.
     */
    public void setData(String title, String description, int prepTime, int servings, int categoryId, int difficultyId) {
        titleField.setText(title);
        descriptionArea.setText(description);
        prepTimeField.setText(String.valueOf(prepTime));
        servingsField.setText(String.valueOf(servings));

        categoryComboBox.getItems().stream()
                .filter(c -> c.getId().value() == categoryId)
                .findFirst()
                .ifPresent(categoryComboBox::setValue);

        difficultyComboBox.getItems().stream()
                .filter(d -> d.getId().value() == difficultyId)
                .findFirst()
                .ifPresent(difficultyComboBox::setValue);
    }
}