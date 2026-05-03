package com.recetea.infrastructure.reports.openpdf;

import com.recetea.core.recipe.domain.Category;
import com.recetea.core.recipe.domain.Difficulty;
import com.recetea.core.recipe.domain.Recipe;
import com.recetea.core.recipe.domain.RecipeIngredient;
import com.recetea.core.recipe.domain.RecipeStep;
import com.recetea.core.recipe.domain.vo.CategoryId;
import com.recetea.core.recipe.domain.vo.DifficultyId;
import com.recetea.core.recipe.domain.vo.IngredientId;
import com.recetea.core.recipe.domain.vo.PreparationTime;
import com.recetea.core.recipe.domain.vo.RecipeId;
import com.recetea.core.recipe.domain.vo.Servings;
import com.recetea.core.recipe.domain.vo.UnitId;
import com.recetea.core.user.domain.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("OpenPDF Adapters — PDF Report Generation")
class OpenPdfAdapterTest {

    private OpenPdfRecipeAdapter recipeAdapter;

    @BeforeEach
    void setUp() {
        recipeAdapter = new OpenPdfRecipeAdapter();
    }

    @Test
    @DisplayName("generateTechnicalSheet: Should produce a non-empty PDF")
    void technicalSheet_ShouldProduceNonEmptyPdf() {
        byte[] out = recipeAdapter.generateTechnicalSheet(buildRecipe(), "chef_maria");

        assertTrue(out.length > 0, "The PDF must not be empty");
        assertPdfMagicBytes(out, "generateTechnicalSheet");
    }

    @Test
    @DisplayName("generateTechnicalSheet: Should not throw with a complete recipe")
    void technicalSheet_ShouldNotThrow_WithFullRecipe() {
        assertDoesNotThrow(() -> recipeAdapter.generateTechnicalSheet(buildRecipe(), "chef_maria"));
    }

    @Test
    @DisplayName("generateTechnicalSheet: Should fall back to placeholder when authorUsername is null")
    void technicalSheet_ShouldHandleNullAuthorUsername() {
        byte[] out = recipeAdapter.generateTechnicalSheet(buildRecipe(), null);

        assertTrue(out.length > 0);
        assertPdfMagicBytes(out, "generateTechnicalSheet null author");
    }

    private Recipe buildRecipe() {
        return new Recipe(
                new RecipeId(1), new UserId(1),
                new Category(new CategoryId(2), "Desserts"),
                new Difficulty(new DifficultyId(1), "Easy"),
                "Apple Pie",
                "A delicious homemade pie with cinnamon.",
                new PreparationTime(45),
                new Servings(6),
                java.math.BigDecimal.ZERO, 0)
                .syncIngredients(List.of(
                        new RecipeIngredient(new IngredientId(1), new UnitId(1),
                                BigDecimal.valueOf(250), "Flour", "g"),
                        new RecipeIngredient(new IngredientId(2), new UnitId(2),
                                BigDecimal.valueOf(3),   "Apple", "u"),
                        new RecipeIngredient(new IngredientId(3), new UnitId(3),
                                BigDecimal.valueOf(100), "Sugar", "g")
                ))
                .syncSteps(List.of(
                        new RecipeStep(1, "Peel and slice the apples thinly."),
                        new RecipeStep(2, "Mix flour with sugar and add cold butter."),
                        new RecipeStep(3, "Place the dough in the mould and arrange the apples on top."),
                        new RecipeStep(4, "Bake at 180 °C for 35 minutes.")
                ));
    }

    private void assertPdfMagicBytes(byte[] bytes, String context) {
        assertTrue(bytes.length >= 4,
                context + ": PDF must have at least 4 bytes");
        // PDF files always begin with the %PDF- signature (0x25 0x50 0x44 0x46)
        assertEquals(0x25, bytes[0] & 0xFF, context + ": byte[0] must be 0x25 ('%')");
        assertEquals(0x50, bytes[1] & 0xFF, context + ": byte[1] must be 0x50 ('P')");
        assertEquals(0x44, bytes[2] & 0xFF, context + ": byte[2] must be 0x44 ('D')");
        assertEquals(0x46, bytes[3] & 0xFF, context + ": byte[3] must be 0x46 ('F')");
    }
}
