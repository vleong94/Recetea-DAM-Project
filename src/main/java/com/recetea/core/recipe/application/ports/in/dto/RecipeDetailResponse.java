package com.recetea.core.recipe.application.ports.in.dto;

import java.util.List;

/**
 * Representa la proyección detallada de una receta para su consumo en la capa de presentación.
 * Actúa como un Outbound DTO (Data Transfer Object) inmutable que encapsula la totalidad
 * del estado persistido de la entidad, incluyendo sus colecciones dependientes (ingredientes y pasos).
 * Aisla el modelo de dominio garantizando que la vista no interactúe directamente con el Aggregate Root.
 *
 * @param id Identificador único de persistencia de la receta.
 * @param userId Identificador del autor de la receta.
 * @param categoryId Identificador de la clasificación taxonómica.
 * @param difficultyId Identificador del nivel de complejidad asignado.
 * @param title Título principal de la receta.
 * @param description Descripción general o resumen de la receta.
 * @param prepTimeMinutes Tiempo total requerido para la preparación en minutos.
 * @param servings Rendimiento de la receta en cantidad de porciones.
 * @param ingredients Colección inmutable con la composición detallada de ingredientes.
 * @param steps Colección inmutable con el flujo secuencial de instrucciones.
 */
public record RecipeDetailResponse(
        int id,
        int userId,
        int categoryId,
        String categoryName,
        int difficultyId,
        String difficultyName,
        String title,
        String description,
        int prepTimeMinutes,
        int servings,
        List<RecipeIngredientResponse> ingredients,
        List<RecipeStepResponse> steps
) {
    /**
     * Define el subcontrato de datos para la proyección de cada bloque operativo de preparación.
     * Asegura la transmisión íntegra del orden lógico y el contenido de la acción hacia la interfaz gráfica.
     *
     * @param stepOrder Posición secuencial inmutable de la instrucción en el flujo.
     * @param instruction Contenido textual descriptivo de la acción a ejecutar.
     */
    public record RecipeStepResponse(
            int stepOrder,
            String instruction
    ) {}
}