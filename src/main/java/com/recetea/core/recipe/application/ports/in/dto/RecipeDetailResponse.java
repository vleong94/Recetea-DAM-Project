package com.recetea.core.recipe.application.ports.in.dto;

import java.util.List;

/**
 * Representa la información exhaustiva de una receta para su visualización detallada o edición.
 * Este registro agrupa todos los atributos de la receta y su lista de ingredientes hidratados,
 * funcionando como un contenedor de datos inmutable que desacopla el núcleo del sistema
 * de las plataformas de presentación.
 *
 * @param id Identificador único de la receta.
 * @param userId Identificador del autor de la receta.
 * @param categoryId Identificador de la categoría a la que pertenece.
 * @param difficultyId Identificador del nivel de dificultad asignado.
 * @param title Título de la receta.
 * @param description Descripción detallada o pasos de la receta.
 * @param prepTimeMinutes Tiempo de preparación total en minutos.
 * @param servings Cantidad de raciones que rinde la preparación.
 * @param ingredients Colección de ingredientes con sus cantidades y unidades correspondientes.
 */
public record RecipeDetailResponse(
        int id,
        int userId,
        int categoryId,
        int difficultyId,
        String title,
        String description,
        int prepTimeMinutes,
        int servings,
        List<RecipeIngredientResponse> ingredients
) {
    /**
     * La estructura de Java Record asegura la inmutabilidad de la respuesta, facilitando
     * un transporte de datos seguro entre las capas de aplicación e infraestructura.
     */
}