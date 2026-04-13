package com.recetea.infrastructure.ui.javafx.components;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.io.IOException;

/**
 * Componente visual atómico responsable de la captura y validación primaria
 * de los metadatos generales de una receta.
 * Implementa el patrón de encapsulamiento aislando los controles de entrada de texto
 * (TextField, TextArea) de los controladores de vista superiores.
 */
public class RecipeHeaderComponent extends VBox {

    @FXML private TextField titleField;
    @FXML private TextField prepTimeField;
    @FXML private TextField servingsField;
    @FXML private TextArea descriptionArea;

    /**
     * Inicializa el componente vinculando de forma autónoma su lógica con el FXML.
     * Se configura a sí mismo como raíz (Root) y controlador (Controller) del grafo de escena.
     */
    public RecipeHeaderComponent() {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/recetea/infrastructure/ui/javafx/fxml/components/recipe_header.fxml"));
        loader.setRoot(this);
        loader.setController(this);
        try {
            loader.load();
        } catch (IOException e) {
            throw new RuntimeException("Fallo de infraestructura crítico: No se pudo instanciar recipe_header.fxml", e);
        }
    }

    /**
     * Verifica la integridad estructural de los datos ingresados en el formulario.
     * Evalúa la presencia de texto obligatorio y la validez de los formatos numéricos.
     *
     * @return true si todos los campos cumplen con los criterios, false en caso contrario.
     */
    public boolean isValid() {
        if (titleField.getText() == null || titleField.getText().trim().isEmpty()) {
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

    public String getTitle() {
        return titleField.getText().trim();
    }

    public String getDescription() {
        return descriptionArea.getText() != null ? descriptionArea.getText().trim() : "";
    }

    public int getPrepTime() {
        return Integer.parseInt(prepTimeField.getText().trim());
    }

    public int getServings() {
        return Integer.parseInt(servingsField.getText().trim());
    }

    /**
     * Hidrata el estado de los controles visuales mediante los valores proporcionados.
     * Operación diseñada para el flujo de edición y renderizado inicial.
     */
    public void setData(String title, String description, int prepTime, int servings) {
        titleField.setText(title);
        descriptionArea.setText(description);
        prepTimeField.setText(String.valueOf(prepTime));
        servingsField.setText(String.valueOf(servings));
    }
}