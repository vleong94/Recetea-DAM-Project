package com.recetea.infrastructure.ui.javafx.features.recipe.controllers;

import com.recetea.core.recipe.application.ports.in.dto.RecipeDetailResponse;
import com.recetea.core.recipe.application.ports.in.dto.SaveRecipeRequest;
import java.util.stream.Collectors;

/**
 * Especialización del controlador de formulario orientada exclusivamente a la mutación
 * de estado de recetas existentes.
 * * Hereda la infraestructura de validación de la clase base y añade la capacidad de
 * hidratar la vista con datos preexistentes. Aísla la complejidad de la actualización,
 * garantizando que las operaciones de modificación conserven la referencia de identidad
 * de la entidad objetivo.
 */
public class RecipeUpdateController extends BaseRecipeFormController {

    /**
     * Identificador único de la entidad en edición.
     * Es imperativo mantener este estado en memoria para garantizar que la capa
     * de persistencia aplique los cambios sobre el registro correcto.
     */
    private int currentRecipeId;

    /**
     * Hidrata los componentes visuales heredados a partir de una proyección inmutable de la receta.
     * Establece el estado inicial del formulario mapeando el modelo de lectura (Response)
     * a las estructuras de entrada esperadas por las tablas y campos de texto.
     *
     * @param recipe Objeto de transferencia de datos con el estado persistido actual.
     */
    public void loadRecipeData(RecipeDetailResponse recipe) {
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
                                i.ingredientId(),
                                i.unitId(),
                                i.quantity(),
                                i.ingredientName(),
                                i.unitName()))
                        .collect(Collectors.toList())
        );
    }

    /**
     * Ejecuta la actualización del estado.
     * Intercepta la orden de guardado validada por la clase base y vincula el identificador
     * de la receta en memoria con el nuevo contrato de datos, delegando la transacción
     * al caso de uso de actualización.
     *
     * @param request Estructura de datos validada con las mutaciones ingresadas por el usuario.
     */
    @Override
    protected void handleSave(SaveRecipeRequest request) {
        context.updateRecipe().execute(currentRecipeId, request);
    }
}