package com.recetea.core.recipe.domain;

import com.recetea.core.recipe.domain.RecipeIngredient.RecipeIngredientValidationException;
import com.recetea.core.recipe.domain.vo.IngredientId;
import com.recetea.core.recipe.domain.vo.UnitId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Targets every branch of the compact constructor so PITest mutations are killed:
 *   - null guards on ingredientId / unitId
 *   - quantity null + zero-boundary + negative + smallest-positive
 *   - blank-name and blank-abbreviation guards in the 5-arg deep-load path
 *   - trim semantics on optional name fields
 *   - 3-arg constructor delegates through the canonical compact constructor
 *
 * The exception type is asserted explicitly (not the abstract supertype) so a mutation
 * that swaps the throw expression for a different RuntimeException is caught.
 */
@DisplayName("RecipeIngredient — Compact-constructor validation")
class RecipeIngredientTest {

    private static final IngredientId ING  = new IngredientId(1);
    private static final UnitId       UNIT = new UnitId(1);
    private static final BigDecimal   POSITIVE = BigDecimal.valueOf(100);

    @Nested
    @DisplayName("Quantity")
    class Quantity {

        @Test
        @DisplayName("3-arg: accepts a strictly positive quantity")
        void acceptsPositiveQuantity() {
            RecipeIngredient ri = new RecipeIngredient(ING, UNIT, POSITIVE);

            assertEquals(0, POSITIVE.compareTo(ri.quantity()));
            assertSame(ING, ri.ingredientId());
            assertSame(UNIT, ri.unitId());
            assertNull(ri.ingredientName());
            assertNull(ri.unitAbbreviation());
        }

        @Test
        @DisplayName("rejects null quantity with the validation message")
        void rejectsNullQuantity() {
            RecipeIngredientValidationException ex = assertThrows(
                    RecipeIngredientValidationException.class,
                    () -> new RecipeIngredient(ING, UNIT, null));
            assertEquals("Ingredient quantity must be strictly greater than zero.", ex.getMessage());
        }

        @Test
        @DisplayName("rejects zero quantity (boundary)")
        void rejectsZeroQuantity() {
            assertThrows(RecipeIngredientValidationException.class,
                    () -> new RecipeIngredient(ING, UNIT, BigDecimal.ZERO));
        }

        @Test
        @DisplayName("rejects negative quantity")
        void rejectsNegativeQuantity() {
            assertThrows(RecipeIngredientValidationException.class,
                    () -> new RecipeIngredient(ING, UNIT, new BigDecimal("-0.01")));
        }

        @Test
        @DisplayName("accepts the smallest positive quantity (kills <=0 → <0 mutation)")
        void acceptsSmallestPositiveQuantity() {
            BigDecimal smallest = new BigDecimal("0.01");
            RecipeIngredient ri = new RecipeIngredient(ING, UNIT, smallest);
            assertEquals(0, smallest.compareTo(ri.quantity()));
        }
    }

    @Nested
    @DisplayName("Required-reference null guards")
    class NullGuards {

        @Test
        @DisplayName("rejects null ingredientId with NullPointerException")
        void rejectsNullIngredientId() {
            NullPointerException ex = assertThrows(NullPointerException.class,
                    () -> new RecipeIngredient(null, UNIT, POSITIVE));
            assertEquals("ingredientId is required.", ex.getMessage());
        }

        @Test
        @DisplayName("rejects null unitId with NullPointerException")
        void rejectsNullUnitId() {
            NullPointerException ex = assertThrows(NullPointerException.class,
                    () -> new RecipeIngredient(ING, null, POSITIVE));
            assertEquals("unitId is required.", ex.getMessage());
        }
    }

    @Nested
    @DisplayName("5-arg deep-load path (ingredient name + unit abbreviation)")
    class DeepLoadPath {

        @Test
        @DisplayName("accepts non-null name and abbreviation; preserves them verbatim")
        void acceptsValidNameAndAbbreviation() {
            RecipeIngredient ri = new RecipeIngredient(ING, UNIT, POSITIVE, "Flour", "g");

            assertEquals("Flour", ri.ingredientName());
            assertEquals("g",     ri.unitAbbreviation());
        }

        @Test
        @DisplayName("trims surrounding whitespace on name and abbreviation (kills no-op trim mutation)")
        void trimsNameAndAbbreviation() {
            RecipeIngredient ri = new RecipeIngredient(ING, UNIT, POSITIVE, "  Flour  ", "  g  ");

            assertEquals("Flour", ri.ingredientName());
            assertEquals("g",     ri.unitAbbreviation());
        }

        @Test
        @DisplayName("rejects blank ingredient name with the deep-load validation message")
        void rejectsBlankName() {
            RecipeIngredientValidationException ex = assertThrows(
                    RecipeIngredientValidationException.class,
                    () -> new RecipeIngredient(ING, UNIT, POSITIVE, "   ", "g"));
            assertEquals("Ingredient name is required for deep-load instantiation.", ex.getMessage());
        }

        @Test
        @DisplayName("rejects blank unit abbreviation with the deep-load validation message")
        void rejectsBlankAbbreviation() {
            RecipeIngredientValidationException ex = assertThrows(
                    RecipeIngredientValidationException.class,
                    () -> new RecipeIngredient(ING, UNIT, POSITIVE, "Flour", "   "));
            assertEquals("Unit abbreviation is required for deep-load instantiation.", ex.getMessage());
        }

        @Test
        @DisplayName("null name and abbreviation pass through unchanged (3-arg path equivalence)")
        void nullsPassThrough() {
            RecipeIngredient ri = new RecipeIngredient(ING, UNIT, POSITIVE, null, null);

            assertNull(ri.ingredientName());
            assertNull(ri.unitAbbreviation());
        }

        @Test
        @DisplayName("RecipeIngredientValidationException extends InvalidIngredientException")
        void exceptionTypeChain() {
            RecipeIngredientValidationException ex =
                    new RecipeIngredientValidationException("test");
            assertInstanceOf(InvalidIngredientException.class, ex);
            assertEquals("test", ex.getMessage());
        }
    }

    @Nested
    @DisplayName("Record-generated equals / hashCode / toString")
    class RecordIdentity {

        @Test
        @DisplayName("equals + hashCode honour value-semantics on every component")
        void equalsAndHashCode() {
            RecipeIngredient a = new RecipeIngredient(ING, UNIT, POSITIVE);
            RecipeIngredient b = new RecipeIngredient(ING, UNIT, POSITIVE);
            RecipeIngredient differentIngredient =
                    new RecipeIngredient(new IngredientId(2), UNIT, POSITIVE);
            RecipeIngredient differentUnit =
                    new RecipeIngredient(ING, new UnitId(2), POSITIVE);
            RecipeIngredient differentQuantity =
                    new RecipeIngredient(ING, UNIT, BigDecimal.valueOf(50));

            assertEquals(a, b);
            assertEquals(a.hashCode(), b.hashCode());
            assertNotEquals(a, differentIngredient);
            assertNotEquals(a, differentUnit);
            assertNotEquals(a, differentQuantity);
            assertNotEquals(a, null);
            assertNotEquals(a, "not a RecipeIngredient");

            // Distinct components must produce distinct hashes — kills the "return 0" mutation.
            assertNotEquals(a.hashCode(), differentIngredient.hashCode());
            assertNotEquals(a.hashCode(), differentQuantity.hashCode());
        }

        @Test
        @DisplayName("toString exposes the canonical record format")
        void toStringIncludesFields() {
            RecipeIngredient ri = new RecipeIngredient(ING, UNIT, POSITIVE);
            String s = ri.toString();

            assertNotNull(s);
            assertFalse(s.isEmpty());
            assertTrue(s.contains("RecipeIngredient"),
                    "toString should include the record type name");
            assertTrue(s.contains("100"),
                    "toString should expose the quantity component");
        }
    }
}
