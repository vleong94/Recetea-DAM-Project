package com.recetea.core.recipe.domain;

import java.math.BigDecimal;

/**
 * Value Object inmutable que representa la dosificación exacta de un recurso dentro de una receta.
 * Soporta dos vías de instanciación para alinearse con el patrón CQRS a nivel lógico:
 * instanciación estructural para persistencia e instanciación completa para Deep Load.
 */
public class RecipeIngredient {

    private final int ingredientId;
    private final int unitId;
    private final BigDecimal quantity;

    // Metadatos de lectura (Query Phase). Su nulabilidad está controlada por los constructores.
    private final String ingredientName;
    private final String unitAbbreviation;

    /**
     * Constructor estructural (Command Phase / Operaciones de Escritura).
     * Inicializa el estado mínimo viable requerido por el motor relacional para
     * la inserción o actualización, omitiendo intencionalmente los descriptores de UI.
     *
     * @param ingredientId Identificador principal del recurso en el Data Store.
     * @param unitId Identificador de la escala de medida.
     * @param quantity Magnitud escalar de alta precisión.
     * @throws RecipeIngredientValidationException Si se vulneran las reglas matemáticas de negocio.
     */
    public RecipeIngredient(int ingredientId, int unitId, BigDecimal quantity) {
        validateQuantity(quantity);

        this.ingredientId = ingredientId;
        this.unitId = unitId;
        this.quantity = quantity;
        this.ingredientName = null;
        this.unitAbbreviation = null;
    }

    /**
     * Constructor de hidratación (Query Phase / Operaciones de Lectura).
     * Blinda el estado exigiendo todos los descriptores necesarios tras ejecutar un JOIN,
     * garantizando que la capa de UI reciba un objeto íntegro sin hacer queries secundarias.
     *
     * @param ingredientId Identificador principal del recurso.
     * @param unitId Identificador de la escala de medida.
     * @param quantity Magnitud escalar de alta precisión.
     * @param ingredientName Representación textual del recurso recuperada.
     * @param unitAbbreviation Símbolo de la escala de medida recuperado.
     * @throws RecipeIngredientValidationException Si los descriptores son inválidos.
     */
    public RecipeIngredient(int ingredientId, int unitId, BigDecimal quantity, String ingredientName, String unitAbbreviation) {
        validateQuantity(quantity);

        if (ingredientName == null || ingredientName.trim().isEmpty()) {
            throw new RecipeIngredientValidationException("El descriptor del ingrediente es crítico para la instanciación por Deep Load.");
        }

        if (unitAbbreviation == null || unitAbbreviation.trim().isEmpty()) {
            throw new RecipeIngredientValidationException("La abreviatura de la unidad de medida es crítica para la instanciación por Deep Load.");
        }

        this.ingredientId = ingredientId;
        this.unitId = unitId;
        this.quantity = quantity;
        this.ingredientName = ingredientName.trim();
        this.unitAbbreviation = unitAbbreviation.trim();
    }

    /**
     * Centraliza la invariante matemática de la dosificación.
     */
    private void validateQuantity(BigDecimal quantity) {
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RecipeIngredientValidationException("La magnitud de la cantidad debe ser estrictamente mayor que cero.");
        }
    }

    public int getIngredientId() {
        return ingredientId;
    }

    public int getUnitId() {
        return unitId;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public String getIngredientName() {
        return ingredientName;
    }

    public String getUnitAbbreviation() {
        return unitAbbreviation;
    }

    /**
     * Domain Exception que encapsula las violaciones de estado del Value Object.
     * Aísla las validaciones del Core respecto a excepciones de infraestructura.
     */
    public static class RecipeIngredientValidationException extends RuntimeException {
        public RecipeIngredientValidationException(String message) {
            super(message);
        }
    }
}