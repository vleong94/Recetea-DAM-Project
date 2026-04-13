package com.recetea.core.recipe.application.ports.in.dto;

/**
 * Define el contrato de datos para la representación de una unidad de medida (ej. gramos, litros, cucharadas)
 * dentro del catálogo global del sistema. Facilita el transporte de datos inmutables hacia
 * la interfaz de usuario para la construcción de selectores y componentes visuales.
 *
 * @param id Identificador único y persistente de la unidad de medida.
 * @param name Nombre descriptivo o símbolo de la unidad para su visualización.
 */
public record UnitResponse(
        int id,
        String name
) {
    /**
     * Al igual que el resto de los objetos de transferencia de datos (DTOs) del sistema,
     * el uso de un Java Record garantiza la inmutabilidad. Una vez que la unidad es recuperada
     * y empaquetada por el caso de uso, su estado no puede ser alterado, asegurando la
     * integridad del contrato de interfaz.
     */
}