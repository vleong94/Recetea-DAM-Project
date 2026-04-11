package com.recetea.infrastructure.ui;

import com.recetea.core.ports.in.CreateRecipeCommand;
import com.recetea.core.ports.in.ICreateRecipeUseCase;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import java.util.Collections;

/**
 * UI Controller: Gestiona los eventos de la vista FXML.
 * Delega toda la lógica de negocio al Inbound Port (UseCase).
 */
public class CreateRecipeController {

    @FXML private TextField titleField;
    @FXML private TextArea descriptionArea;
    @FXML private TextField prepTimeField;
    @FXML private TextField servingsField;

    // Dependencia del puerto de entrada
    private ICreateRecipeUseCase createRecipeUseCase;

    // Método de inyección manual (en el futuro lo hará un Dependency Injection Container)
    public void setCreateRecipeUseCase(ICreateRecipeUseCase useCase) {
        this.createRecipeUseCase = useCase;
    }

    @FXML
    public void onSaveButtonClick() {
        try {
            // 1. Recolección y validación básica de la UI
            String title = titleField.getText();
            String desc = descriptionArea.getText();
            int prepTime = Integer.parseInt(prepTimeField.getText());
            int servings = Integer.parseInt(servingsField.getText());

            // 2. Empaquetado en el DTO (Command)
            CreateRecipeCommand command = new CreateRecipeCommand(
                    1, 1, 1, // IDs maestro fijos para este MVP
                    title, desc, prepTime, servings,
                    Collections.emptyList() // Sin ingredientes para esta prueba rápida
            );

            // 3. Delegación al orquestador
            int newId = createRecipeUseCase.execute(command);

            showSuccess("¡Éxito!", "Receta guardada correctamente en base de datos con ID: " + newId);
            clearForm();

        } catch (NumberFormatException e) {
            showError("Error de validación", "El tiempo y las raciones deben ser números enteros.");
        } catch (Exception e) {
            showError("Error Crítico", e.getMessage());
        }
    }

    private void showSuccess(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void clearForm() {
        titleField.clear();
        descriptionArea.clear();
        prepTimeField.clear();
        servingsField.clear();
    }
}