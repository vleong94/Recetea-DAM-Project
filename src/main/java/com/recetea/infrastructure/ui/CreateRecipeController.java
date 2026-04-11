package com.recetea.infrastructure.ui;

import com.recetea.core.ports.in.CreateRecipeCommand;
import com.recetea.core.ports.in.ICreateRecipeUseCase;
import com.recetea.core.ports.in.IGetAllRecipesUseCase;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Collections;

public class CreateRecipeController {

    @FXML private TextField titleField;
    @FXML private TextArea descriptionArea;
    @FXML private TextField prepTimeField;
    @FXML private TextField servingsField;

    // Dependencias (Puertos de entrada)
    private ICreateRecipeUseCase createRecipeUseCase;
    private IGetAllRecipesUseCase getAllRecipesUseCase;

    // Inyección de ambas dependencias
    public void setUseCases(ICreateRecipeUseCase create, IGetAllRecipesUseCase getAll) {
        this.createRecipeUseCase = create;
        this.getAllRecipesUseCase = getAll;
    }

    @FXML
    public void onSaveButtonClick() {
        try {
            String title = titleField.getText();
            String desc = descriptionArea.getText();
            int prepTime = Integer.parseInt(prepTimeField.getText());
            int servings = Integer.parseInt(servingsField.getText());

            CreateRecipeCommand command = new CreateRecipeCommand(
                    1, 1, 1, title, desc, prepTime, servings, Collections.emptyList()
            );

            int newId = createRecipeUseCase.execute(command);
            showSuccess("¡Éxito!", "Receta guardada con ID: " + newId);

            // Auto-Routing: Volvemos al Dashboard automáticamente tras guardar
            returnToDashboard();

        } catch (NumberFormatException e) {
            showError("Error de validación", "El tiempo y las raciones deben ser números enteros.");
        } catch (Exception e) {
            showError("Error Crítico", e.getMessage());
        }
    }

    @FXML
    public void onBackButtonClick() {
        returnToDashboard();
    }

    private void returnToDashboard() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/recetea/infrastructure/ui/dashboard.fxml"));
            Parent root = loader.load();

            // Re-inyectamos las dependencias en el Dashboard
            DashboardController controller = loader.getController();
            controller.setGetAllRecipesUseCase(this.getAllRecipesUseCase);
            controller.setCreateRecipeUseCase(this.createRecipeUseCase);
            controller.loadData();

            // Usamos cualquier nodo visual (ej. titleField) para obtener el Stage actual
            Stage stage = (Stage) titleField.getScene().getWindow();
            stage.setScene(new Scene(root, 700, 500));
            stage.setTitle("Recetea - Dashboard Principal");

        } catch (IOException e) {
            showError("Error de UI", "No se pudo cargar el Dashboard.");
            e.printStackTrace();
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
}