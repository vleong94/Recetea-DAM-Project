package com.recetea.infrastructure.ui.javafx.features.recipe.controllers;

import com.recetea.core.recipe.application.ports.in.dto.SaveRecipeRequest;

/**
 * Especialización del controlador de formulario encargada de la creación de nuevas recetas.
 * Hereda la infraestructura visual y de validación de la clase base, implementando
 * exclusivamente la lógica de persistencia para el registro de nuevas entidades en el sistema.
 * Este componente garantiza un estado inicial limpio y volátil para la captura de datos.
 */
public class RecipeCreateController extends BaseRecipeFormController {

    /**
     * Implementa la lógica de guardado específica para el flujo de creación.
     * Invoca el caso de uso correspondiente dentro del núcleo de la aplicación,
     * transformando el contrato de datos en una nueva entrada persistente.
     *
     * @param request Estructura de datos inmutable con la información de la nueva receta.
     */
    @Override
    protected void handleSave(SaveRecipeRequest request) {
        context.createRecipe().execute(request);
    }
}