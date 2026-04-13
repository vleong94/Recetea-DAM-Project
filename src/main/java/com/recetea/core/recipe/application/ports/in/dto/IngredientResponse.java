package com.recetea.core.recipe.application.ports.in.dto;

/**
 * Define el contrato de datos para la representación de un ingrediente dentro del catálogo global.
 * Se utiliza principalmente para transferir información básica hacia los selectores de la interfaz
 * de usuario, asegurando que la capa de presentación trabaje con datos planos e inmutables.
 *
 * @param id Identificador único y persistente del ingrediente.
 * @param name Nombre descriptivo o etiqueta del ingrediente para su visualización.
 */
public record IngredientResponse(
        int id,
        String name
) {
    /**
     * El uso de la estructura record garantiza que los datos del ingrediente no sean alterados
     * una vez han salido del núcleo de la aplicación, manteniendo la integridad de la información
     * durante su transporte hacia la interfaz de usuario.
     */
}