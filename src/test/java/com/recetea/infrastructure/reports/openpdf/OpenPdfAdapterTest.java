package com.recetea.infrastructure.reports.openpdf;

import com.recetea.core.recipe.application.ports.in.dto.RecipeSummaryResponse;
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

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("OpenPDF Adapters — Generación de informes PDF")
class OpenPdfAdapterTest {

    private OpenPdfRecipeAdapter recipeAdapter;
    private OpenPdfStatsAdapter  statsAdapter;

    @BeforeEach
    void setUp() {
        recipeAdapter = new OpenPdfRecipeAdapter();
        statsAdapter  = new OpenPdfStatsAdapter();
    }

    // ── OpenPdfRecipeAdapter ──────────────────────────────────────────────────

    @Test
    @DisplayName("generateTechnicalSheet debe producir un PDF no vacío")
    void technicalSheet_ShouldProduceNonEmptyPdf() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        recipeAdapter.generateTechnicalSheet(buildRecipe(), out);

        assertTrue(out.size() > 0, "El PDF no debe estar vacío");
        assertPdfMagicBytes(out, "generateTechnicalSheet");
    }

    @Test
    @DisplayName("generateTechnicalSheet no debe lanzar excepción con receta completa")
    void technicalSheet_ShouldNotThrow_WithFullRecipe() {
        assertDoesNotThrow(() ->
                recipeAdapter.generateTechnicalSheet(buildRecipe(), new ByteArrayOutputStream()));
    }

    @Test
    @DisplayName("generateTechnicalSheet debe producir PDF válido para receta sin valoraciones")
    void technicalSheet_ShouldWork_WhenNoRatings() {
        Recipe recipe = buildRecipe();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        recipeAdapter.generateTechnicalSheet(recipe, out);

        assertTrue(out.size() > 0);
        assertPdfMagicBytes(out, "generateTechnicalSheet sin valoraciones");
    }

    // ── OpenPdfStatsAdapter ───────────────────────────────────────────────────

    @Test
    @DisplayName("generateGlobalInventoryReport debe producir un PDF no vacío")
    void inventoryReport_ShouldProduceNonEmptyPdf() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        statsAdapter.generateGlobalInventoryReport(buildSummaries(), out);

        assertTrue(out.size() > 0, "El PDF no debe estar vacío");
        assertPdfMagicBytes(out, "generateGlobalInventoryReport");
    }

    @Test
    @DisplayName("generateGlobalInventoryReport no debe lanzar excepción con lista vacía")
    void inventoryReport_ShouldNotThrow_WithEmptyList() {
        assertDoesNotThrow(() ->
                statsAdapter.generateGlobalInventoryReport(List.of(), new ByteArrayOutputStream()));
    }

    @Test
    @DisplayName("generateGlobalInventoryReport debe producir PDF válido con múltiples recetas")
    void inventoryReport_ShouldWork_WithMultipleSummaries() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        statsAdapter.generateGlobalInventoryReport(buildSummaries(), out);

        assertTrue(out.size() > 0);
        assertPdfMagicBytes(out, "generateGlobalInventoryReport múltiples entradas");
    }

    // ── Builders ──────────────────────────────────────────────────────────────

    private Recipe buildRecipe() {
        Recipe recipe = new Recipe(
                new UserId(1),
                new Category(new CategoryId(2), "Postres"),
                new Difficulty(new DifficultyId(1), "Fácil"),
                "Tarta de Manzana",
                "Una deliciosa tarta casera con canela.",
                new PreparationTime(45),
                new Servings(6)
        );
        recipe.setId(new RecipeId(1));
        recipe.syncIngredients(List.of(
                new RecipeIngredient(new IngredientId(1), new UnitId(1),
                        BigDecimal.valueOf(250), "Harina", "g"),
                new RecipeIngredient(new IngredientId(2), new UnitId(2),
                        BigDecimal.valueOf(3),   "Manzana", "uds"),
                new RecipeIngredient(new IngredientId(3), new UnitId(3),
                        BigDecimal.valueOf(100), "Azúcar", "g")
        ));
        recipe.syncSteps(List.of(
                new RecipeStep(1, "Pelar y cortar las manzanas en láminas finas."),
                new RecipeStep(2, "Mezclar la harina con el azúcar y añadir mantequilla fría."),
                new RecipeStep(3, "Colocar la masa en el molde y agregar las manzanas encima."),
                new RecipeStep(4, "Hornear a 180 °C durante 35 minutos.")
        ));
        return recipe;
    }

    private List<RecipeSummaryResponse> buildSummaries() {
        return List.of(
                new RecipeSummaryResponse(
                        new RecipeId(1), "Tarta de Manzana", "Postres",   "Fácil",  45, 6,
                        new BigDecimal("4.5"), 12, null, new UserId(1), "chef_maria"),
                new RecipeSummaryResponse(
                        new RecipeId(2), "Gazpacho",         "Sopas",     "Fácil",  15, 4,
                        new BigDecimal("4.2"),  8, null, new UserId(2), "cocina_pablo"),
                new RecipeSummaryResponse(
                        new RecipeId(3), "Paella Valenciana","Arroces",   "Media",  60, 4,
                        new BigDecimal("4.8"), 25, null, new UserId(1), "chef_maria"),
                new RecipeSummaryResponse(
                        new RecipeId(4), "Tortilla Española","Huevos",    "Fácil",  20, 4,
                        BigDecimal.ZERO,       0,  null, new UserId(3), null)
        );
    }

    // ── Assertion helper ──────────────────────────────────────────────────────

    private void assertPdfMagicBytes(ByteArrayOutputStream out, String context) {
        byte[] bytes = out.toByteArray();
        assertTrue(bytes.length >= 4,
                context + ": el PDF debe tener al menos 4 bytes");
        // PDF files always begin with the %PDF- signature (0x25 0x50 0x44 0x46)
        assertEquals(0x25, bytes[0] & 0xFF, context + ": byte[0] debe ser 0x25 ('%')");
        assertEquals(0x50, bytes[1] & 0xFF, context + ": byte[1] debe ser 0x50 ('P')");
        assertEquals(0x44, bytes[2] & 0xFF, context + ": byte[2] debe ser 0x44 ('D')");
        assertEquals(0x46, bytes[3] & 0xFF, context + ": byte[3] debe ser 0x46 ('F')");
    }
}
