package com.recetea.core.recipe.domain;

import com.recetea.core.recipe.domain.vo.IngredientId;
import com.recetea.core.recipe.domain.vo.UnitId;

import java.math.BigDecimal;

public class RecipeIngredient {

    private final IngredientId ingredientId;
    private final UnitId unitId;
    private final BigDecimal quantity;

    private final String ingredientName;
    private final String unitAbbreviation;

    public RecipeIngredient(IngredientId ingredientId, UnitId unitId, BigDecimal quantity) {
        validateQuantity(quantity);
        this.ingredientId = ingredientId;
        this.unitId = unitId;
        this.quantity = quantity;
        this.ingredientName = null;
        this.unitAbbreviation = null;
    }

    public RecipeIngredient(IngredientId ingredientId, UnitId unitId, BigDecimal quantity,
                            String ingredientName, String unitAbbreviation) {
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

    private void validateQuantity(BigDecimal quantity) {
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RecipeIngredientValidationException("La magnitud de la cantidad debe ser estrictamente mayor que cero.");
        }
    }

    public IngredientId getIngredientId() { return ingredientId; }
    public UnitId getUnitId() { return unitId; }
    public BigDecimal getQuantity() { return quantity; }
    public String getIngredientName() { return ingredientName; }
    public String getUnitAbbreviation() { return unitAbbreviation; }

    public static class RecipeIngredientValidationException extends InvalidIngredientException {
        public RecipeIngredientValidationException(String message) {
            super(message);
        }
    }
}
