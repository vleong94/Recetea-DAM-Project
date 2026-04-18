package com.recetea.infrastructure.ui.javafx.features.recipe.controllers;

import com.recetea.core.recipe.application.ports.in.dto.RecipeDetailResponse;
import com.recetea.core.recipe.application.ports.in.dto.SaveRecipeRequest;
import java.util.stream.Collectors;

/**
 * Controlador especializado en la gestión de mutaciones para recetas persistidas.
 * Extiende la funcionalidad base del formulario para implementar la carga profunda
 * de datos, asegurando que tanto los metadatos de cabecera como las colecciones
 * de ingredientes y pasos se sincronicen correctamente para su edición.
 */
public class RecipeUpdateController extends BaseRecipeFormController {

    private int currentRecipeId;

    /**
     * Realiza la hidratación integral de la interfaz a partir de una proyección detallada.
     * Mapea el estado completo de la receta, incluyendo su taxonomía (categoría y dificultad)
     * y sus flujos operativos, hacia los componentes visuales correspondientes para
     * establecer un punto de partida consistente en la edición.
     *
     * @param recipe Objeto de respuesta con la información exhaustiva de la entidad.
     */
    public void loadRecipeData(RecipeDetailResponse recipe) {
        this.currentRecipeId = recipe.id();

        // Hidratación de metadatos y taxonomía dinámica
        // Se suministran los 6 argumentos requeridos para asegurar la integridad visual
        headerComponent.setData(
                recipe.title(),
                recipe.description(),
                recipe.prepTimeMinutes(),
                recipe.servings(),
                recipe.categoryId(),
                recipe.difficultyId()
        );

        // Hidratación de la composición de ingredientes
        // Transforma la respuesta detallada en el formato de petición esperado por la tabla
        ingredientTableComponent.loadExistingIngredients(
                recipe.ingredients().stream()
                        .map(i -> new SaveRecipeRequest.IngredientRequest(
                                i.ingredientId(),
                                i.unitId(),
                                i.quantity(),
                                i.ingredientName(),
                                i.unitName()))
                        .collect(Collectors.toList())
        );

        // Hidratación del flujo secuencial de instrucciones de preparación
        stepTableComponent.loadSteps(recipe.steps());
    }

    /**
     * Ejecuta la persistencia de los cambios realizados sobre la receta.
     * Vincula el identificador técnico de la entidad con el contrato de datos
     * capturado por los componentes visuales, delegando la actualización al Core.
     *
     * @param request Payload validado con el nuevo estado de la receta.
     */
    @Override
    protected void handleSave(SaveRecipeRequest request) {
        context.updateRecipe().execute(currentRecipeId, request);
    }
}