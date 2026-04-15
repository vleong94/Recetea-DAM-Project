package com.recetea.infrastructure.ui.javafx.features.recipe.controllers;

import com.recetea.core.recipe.application.ports.in.dto.SaveRecipeRequest;
import com.recetea.infrastructure.ui.javafx.features.recipe.RecipeContext;
import com.recetea.infrastructure.ui.javafx.features.recipe.components.IngredientTableComponent;
import com.recetea.infrastructure.ui.javafx.features.recipe.components.RecipeHeaderComponent;
import com.recetea.infrastructure.ui.javafx.shared.navigation.NavigationService;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import java.util.List;

/**
 * Clase base abstracta que define la estructura y el comportamiento común de los formularios de recetas.
 * Centraliza la gestión de los componentes visuales de cabecera y tabla de ingredientes,
 * proporcionando una infraestructura compartida para la validación de datos y la gestión de la navegación.
 * Emplea un diseño de especialización para separar las responsabilidades de creación y actualización.
 */
public abstract class BaseRecipeFormController {

    @FXML protected RecipeHeaderComponent headerComponent;
    @FXML protected IngredientTableComponent ingredientTableComponent;

    protected RecipeContext context;
    protected NavigationService nav;

    /**
     * Inicializa las dependencias y prepara los subcomponentes del formulario.
     * Configura el acceso al contexto de negocio y propaga las dependencias necesarias
     * a los componentes internos para su correcto funcionamiento operativo.
     */
    public void init(RecipeContext context, NavigationService nav) {
        this.context = context;
        this.nav = nav;
        this.ingredientTableComponent.init(context);
    }

    /**
     * Orquesta el proceso de guardado validando el estado de la interfaz antes de la persistencia.
     * Recopila la información de los componentes, verifica la integridad de los datos
     * y delega la ejecución del caso de uso correspondiente a la implementación concreta.
     */
    @FXML
    public void onSaveButtonClick() {
        if (!headerComponent.isValid()) {
            showError("Error de Validación", "La información de la receta es incompleta o inválida.");
            return;
        }

        List<SaveRecipeRequest.IngredientRequest> ingredients = ingredientTableComponent.getIngredients();
        if (ingredients.isEmpty()) {
            showError("Error de Validación", "Es obligatorio incluir al menos un ingrediente.");
            return;
        }

        try {
            SaveRecipeRequest request = new SaveRecipeRequest(
                    getAuthenticatedUserId(),
                    1, // Categoría por defecto
                    1, // Dificultad por defecto
                    headerComponent.getTitle(),
                    headerComponent.getDescription(),
                    headerComponent.getPrepTime(),
                    headerComponent.getServings(),
                    ingredients
            );

            handleSave(request);
            nav.toDashboard();
        } catch (Exception e) {
            showError("Fallo en la Operación", "No se pudo procesar la solicitud: " + e.getMessage());
        }
    }

    /**
     * Método abstracto que define la acción de persistencia a ejecutar.
     * Las subclases deben implementar este método para llamar al caso de uso
     * de creación o actualización según su responsabilidad específica.
     */
    protected abstract void handleSave(SaveRecipeRequest request);

    /**
     * Provee el identificador del autor para la receta.
     * Actúa como punto de integración para la gestión de usuarios y sesiones.
     */
    protected int getAuthenticatedUserId() {
        return 1;
    }

    /**
     * Cancela la operación actual y redirige al usuario al panel principal.
     */
    @FXML
    public void onBackButtonClick() {
        nav.toDashboard();
    }

    /**
     * Centraliza la visualización de mensajes de error críticos mediante cuadros de diálogo.
     */
    protected void showError(String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}