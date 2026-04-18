package com.recetea.core.recipe.application.ports.in.dto;

/**
 * Proyección estructural simplificada de una receta diseñada para su exposición en catálogos y listados.
 * Implementa el patrón Data Transfer Object (DTO) mediante un registro inmutable, garantizando
 * el aislamiento estricto entre el estado de las entidades del Domain y la capa de presentación.
 * * Centraliza los metadatos esenciales junto con las proyecciones en texto plano de la taxonomía,
 * eliminando la necesidad de múltiples consultas a la base de datos (N+1) durante la renderización visual.
 *
 * @param id Identificador numérico único de la receta.
 * @param title Nombre o identificador semántico de la receta.
 * @param categoryName Descriptor textual de la clasificación taxonómica a la que pertenece.
 * @param difficultyName Descriptor textual del nivel de complejidad operativa.
 * @param prepTimeMinutes Métrica de tiempo de ejecución estimado expresado en minutos.
 * @param servings Métrica de rendimiento en raciones o porciones.
 */
public record RecipeSummaryResponse(
        int id,
        String title,
        String categoryName,
        String difficultyName,
        int prepTimeMinutes,
        int servings
) {}