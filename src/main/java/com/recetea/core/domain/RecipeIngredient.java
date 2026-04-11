package com.recetea.core.domain;

import javafx.beans.property.*;

/**
 * Value Object: Representa un ingrediente dosificado dentro de una receta.
 */
public class RecipeIngredient {

    private final IntegerProperty ingredientId;
    private final IntegerProperty unitId;
    private final DoubleProperty quantity;

    public RecipeIngredient(int ingredientId, int unitId, double quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor que cero.");
        }
        this.ingredientId = new SimpleIntegerProperty(ingredientId);
        this.unitId = new SimpleIntegerProperty(unitId);
        this.quantity = new SimpleDoubleProperty(quantity);
    }

    public int getIngredientId() { return ingredientId.get(); }
    public int getUnitId() { return unitId.get(); }
    public double getQuantity() { return quantity.get(); }
}