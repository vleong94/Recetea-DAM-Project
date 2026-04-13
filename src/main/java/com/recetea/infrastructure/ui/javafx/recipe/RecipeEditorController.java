package com.recetea.infrastructure.ui.javafx.recipe;

import com.recetea.core.recipe.application.ports.in.dto.RecipeDetailResponse;
import com.recetea.core.recipe.application.ports.in.dto.SaveRecipeRequest;
import com.recetea.infrastructure.ui.javafx.components.IngredientTableComponent;
import com.recetea.infrastructure.ui.javafx.components.RecipeHeaderComponent;
import com.recetea.infrastructure.ui.javafx.shared.NavigationService;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import java.util.ArrayList;
import java.util.stream.Collectors;

/**
 * Orquestador principal de la feature de Edición de Recetas.
 * Implementa el patrón de mediador puro (Pure Orchestrator) al carecer de conocimiento
 * sobre los controles visuales individuales. Coordina exclusivamente la comunicación
 * entre los componentes atómicos (Header y Table) y los puertos del Core (Use Cases),
 * operando estrictamente con Data Transfer Objects (DTOs).
 */
public class RecipeEditorController {

    @FXML private RecipeHeaderComponent headerComponent;
    @FXML private IngredientTableComponent ingredientTableComponent;

    private RecipeContext context;
    private NavigationService nav;
    private int currentRecipeId;
    private boolean isEditMode = false;

    /**
     * Establece el entorno de ejecución inyectando las dependencias transversales.
     * Propaga la inicialización a los componentes hijos que requieren acceso a los catálogos.
     */
    public void init(RecipeContext context, NavigationService nav) {
        this.context = context;
        this.nav = nav;

        ingredientTableComponent.setCatalogs(
                context.getAllIngredients().execute(),
                context.getAllUnits().execute()
        );
    }

    /**
     * Coordina el flujo de persistencia.
     * Delega la validación primaria a los componentes y ensambla el Command de mutación
     * para transferir el estado de la UI hacia el dominio mediante un DTO inmutable.
     */
    @FXML
    public void onSaveButtonClick() {
        if (!headerComponent.isValid()) {
            showError("Validación de Input", "Verifique que los campos obligatorios y numéricos sean correctos.");
            return;
        }

        try {
            SaveRecipeRequest cmd = new SaveRecipeRequest(
                    1, 1, 1,
                    headerComponent.getTitle(),
                    headerComponent.getDescription(),
                    headerComponent.getPrepTime(),
                    headerComponent.getServings(),
                    new ArrayList<>(ingredientTableComponent.getIngredients())
            );

            if (isEditMode) {
                context.updateRecipe().execute(currentRecipeId, cmd);
            } else {
                context.createRecipe().execute(cmd);
            }

            nav.toDashboard();
        } catch (Exception e) {
            showError("Fallo de Transacción", "No fue posible procesar la entidad en el Core: " + e.getMessage());
        }
    }

    @FXML
    public void onBackButtonClick() {
        nav.toDashboard();
    }

    /**
     * Hidrata los nodos hijos con el estado de un registro preexistente extraído del Core.
     * Desacopla el ruteo de datos delegando la hidratación específica a cada componente responsable,
     * traduciendo las respuestas inmutables del dominio hacia los formatos de entrada requeridos.
     */
    public void loadRecipeData(RecipeDetailResponse recipe) {
        this.isEditMode = true;
        this.currentRecipeId = recipe.id();

        headerComponent.setData(
                recipe.title(),
                recipe.description(),
                recipe.prepTimeMinutes(),
                recipe.servings()
        );

        ingredientTableComponent.loadExistingIngredients(
                recipe.ingredients().stream()
                        .map(i -> new SaveRecipeRequest.IngredientRequest(
                                i.ingredientId(), i.unitId(), i.quantity(),
                                i.ingredientName(), i.unitName()))
                        .collect(Collectors.toList())
        );
    }

    /**
     * Notifica a la interfaz gráfica las violaciones estructurales de los contratos de entrada.
     */
    private void showError(String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}