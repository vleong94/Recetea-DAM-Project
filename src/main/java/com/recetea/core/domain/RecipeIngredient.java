package com.recetea.core.domain;

import java.math.BigDecimal;

/**
 * Value Object: Representa un ingrediente dosificado dentro de una receta.
 * Refactorizado para incluir nombres descriptivos que permiten una visualización
 * clara en la UI, manteniendo la precisión técnica de BigDecimal.
 */
public class RecipeIngredient {

    private final int ingredientId;
    private final int unitId;
    private final BigDecimal quantity;

    // --- CAMPOS PARA UX (Visualización sin IDs) ---
    private final String ingredientName;
    private final String unitName;

    public RecipeIngredient(int ingredientId, int unitId, BigDecimal quantity, String ingredientName, String unitName) {
        // Validación de reglas de negocio: La cantidad siempre debe ser positiva y existir
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor que cero y no nula.");
        }

        this.ingredientId = ingredientId;
        this.unitId = unitId;
        this.quantity = quantity;
        this.ingredientName = ingredientName;
        this.unitName = unitName;
    }

    // Getters de integridad técnica (Para la DB)
    public int getIngredientId() { return ingredientId; }
    public int getUnitId() { return unitId; }
    public BigDecimal getQuantity() { return quantity; }

    // Getters de visualización (Para las columnas de la TableView)
    public String getIngredientName() { return ingredientName; }
    public String getUnitName() { return unitName; }

    /**
     * Nota de Arquitectura:
     * El Dominio se mantiene puro (sin JavaFX Properties).
     * El mapeo a la UI se realiza en el Controller usando ReadOnlyObjectWrapper.
     */
}