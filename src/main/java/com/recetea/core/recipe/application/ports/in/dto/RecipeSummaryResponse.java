package com.recetea.core.recipe.application.ports.in.dto;

/**
 * Representa una vista simplificada de una receta para su visualización en listados o catálogos.
 * Este registro actúa como un objeto de transferencia de datos (DTO) inmutable, asegurando
 * que la capa de presentación no tenga acceso directo a las entidades de negocio.
 * @param id Identificador único de la receta en el sistema.
 * @param title Título descriptivo de la receta.
 * @param prepTimeMinutes Tiempo estimado de preparación expresado en minutos.
 * @param servings Número de raciones o porciones que rinde la receta.
 */
public record RecipeSummaryResponse(
        int id,
        String title,
        int prepTimeMinutes,
        int servings
) {
    /**
     * El uso de un Java Record garantiza que los datos sean inmutables y proporciona
     * automáticamente los métodos de acceso, equals, hashCode y toString.
     */
}