package com.recetea.infrastructure.ui.javafx.features.recipe.controllers;

import com.recetea.core.recipe.application.ports.in.dto.SaveRecipeRequest;
import com.recetea.core.recipe.domain.vo.RecipeId;

/**
 * Especialización del controlador de formulario para la creación de recetas.
 * Extiende la infraestructura de la clase base para capturar el estado visual
 * (cabecera, ingredientes y pasos) y delega la persistencia del nuevo
 * Aggregate Root al caso de uso correspondiente de la capa de aplicación.
 */
public class RecipeCreateController extends BaseRecipeFormController {

    /**
     * Ejecuta la transacción de alta en el sistema.
     * Recibe el payload estructurado desde la clase base, asumiendo que
     * la validación de integridad (campos obligatorios, secuencia de pasos y
     * métricas) ya ha sido superada.
     *
     * @param request Contenedor inmutable (DTO) con los datos consolidados de la receta.
     */
    @Override
    protected RecipeId handleSave(SaveRecipeRequest request) {
        return context.createRecipe().execute(request);
    }
}