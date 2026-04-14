package com.recetea.infrastructure.ui.javafx.features.recipe.components;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.io.IOException;

/**
 * Componente visual atómico especializado en la captura y validación de metadatos de cabecera.
 * Actúa como una barrera de integridad en la capa de presentación (Inbound Adapter),
 * asegurando que la información base de la receta cumpla con los formatos requeridos
 * antes de su propagación al Core.
 */
public class RecipeHeaderComponent extends VBox {

    @FXML private TextField titleField;
    @FXML private TextField prepTimeField;
    @FXML private TextField servingsField;
    @FXML private TextArea descriptionArea;

    /**
     * Inicializa el componente mediante Auto-Binding.
     * Instancia el grafo de escena asociando el recurso FXML estructural,
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
     * Ejecuta una auditoría de estado sobre los nodos de entrada.
     * Garantiza la presencia de datos obligatorios y la coherencia matemática
     * de los valores numéricos mediante Type Parsing seguro.
     *
     * @return true si el estado visual satisface las restricciones de dominio.
     */
    public boolean isValid() {
        String title = titleField.getText();
        if (title == null || title.trim().isEmpty()) {
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
     * Hidrata el componente con el State de una entidad preexistente.
     * Inyecta los valores inmutables del Core hacia las propiedades mutables de la vista.
     */
    public void setData(String title, String description, int prepTime, int servings) {
        titleField.setText(title);
        descriptionArea.setText(description);
        prepTimeField.setText(String.valueOf(prepTime));
        servingsField.setText(String.valueOf(servings));
    }
}