package com.recetea.core.shared.domain;

import com.recetea.core.recipe.domain.InvalidRecipeDataException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ValidationResult — utilidad de validación acumulativa")
class ValidationResultTest {

    // --- Factories ---

    @Test
    void valid_isValid_andCarriesValue() {
        var result = ValidationResult.valid("hola");
        assertTrue(result.isValid());
        assertInstanceOf(ValidationResult.Valid.class, result);
        assertEquals("hola", ((ValidationResult.Valid<String>) result).value());
        assertTrue(result.errors().isEmpty());
    }

    @Test
    void invalid_withSingleError_isNotValid() {
        var result = ValidationResult.invalid("campo requerido");
        assertFalse(result.isValid());
        assertEquals(List.of("campo requerido"), result.errors());
    }

    @Test
    void invalid_withMultipleErrors_preservesOrder() {
        var result = ValidationResult.invalid(List.of("err1", "err2", "err3"));
        assertFalse(result.isValid());
        assertEquals(List.of("err1", "err2", "err3"), result.errors());
    }

    @Test
    void invalid_withEmptyList_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> ValidationResult.invalid(List.of()));
    }

    // --- of() and check() ---

    @Test
    void of_returnsValid_whenConditionTrue() {
        var r = ValidationResult.of("abc", s -> !s.isBlank(), "no puede estar vacío");
        assertTrue(r.isValid());
    }

    @Test
    void of_returnsInvalid_whenConditionFalse() {
        var r = ValidationResult.of("", s -> !s.isBlank(), "no puede estar vacío");
        assertFalse(r.isValid());
        assertEquals("no puede estar vacío", r.errors().getFirst());
    }

    @Test
    void check_returnsValid_onTrue() {
        assertTrue(ValidationResult.check(true, "err").isValid());
    }

    @Test
    void check_returnsInvalid_onFalse() {
        var r = ValidationResult.check(false, "falló");
        assertFalse(r.isValid());
        assertEquals("falló", r.errors().getFirst());
    }

    // --- and() ---

    @Nested
    @DisplayName("and() — concatenación sin cortocircuito")
    class AndTests {

        @Test
        void bothValid_returnsFirstValid() {
            var a = ValidationResult.valid(42);
            var b = ValidationResult.valid("ok");
            var result = a.and(b);
            assertTrue(result.isValid());
        }

        @Test
        void firstValid_secondInvalid_accumulatesError() {
            var a = ValidationResult.valid(42);
            var b = ValidationResult.invalid("error B");
            var result = a.and(b);
            assertFalse(result.isValid());
            assertEquals(List.of("error B"), result.errors());
        }

        @Test
        void firstInvalid_secondValid_accumulatesError() {
            var a = ValidationResult.invalid("error A");
            var b = ValidationResult.valid("ok");
            var result = a.and(b);
            assertFalse(result.isValid());
            assertEquals(List.of("error A"), result.errors());
        }

        @Test
        void bothInvalid_combinesAllErrors() {
            var a = ValidationResult.invalid("error A");
            var b = ValidationResult.invalid("error B");
            var result = a.and(b);
            assertFalse(result.isValid());
            assertEquals(List.of("error A", "error B"), result.errors());
        }

        @Test
        void fluentChain_accumulatesThreeErrors() {
            var result = Validation.validate(false, "err1")
                    .and(Validation.validate(false, "err2"))
                    .and(Validation.validate(false, "err3"));
            assertFalse(result.isValid());
            assertEquals(List.of("err1", "err2", "err3"), result.errors());
        }

        @Test
        void fluentChain_allValid_returnsValid() {
            var result = Validation.validate(true, "err1")
                    .and(Validation.validate(true, "err2"))
                    .and(Validation.validate(true, "err3"));
            assertTrue(result.isValid());
        }
    }

    // --- combine() ---

    @Nested
    @DisplayName("combine() — agregación sin cortocircuito")
    class CombineTests {

        @Test
        void allValid_returnsValid() {
            var result = ValidationResult.combine(
                    ValidationResult.valid(1),
                    ValidationResult.valid("x"),
                    ValidationResult.valid(null)
            );
            assertTrue(result.isValid());
        }

        @Test
        void someInvalid_aggregatesAllErrors() {
            var result = ValidationResult.combine(
                    ValidationResult.valid(1),
                    ValidationResult.invalid("err A"),
                    ValidationResult.valid("ok"),
                    ValidationResult.invalid("err B")
            );
            assertFalse(result.isValid());
            assertEquals(List.of("err A", "err B"), result.errors());
        }

        @Test
        void viaValidationClass_delegatesToCombine() {
            var result = Validation.combine(
                    ValidationResult.invalid("x"),
                    ValidationResult.invalid("y")
            );
            assertEquals(List.of("x", "y"), result.errors());
        }
    }

    // --- getOrThrow() ---

    @Test
    void getOrThrow_onValid_returnsValue() {
        var result = ValidationResult.valid("valor");
        assertEquals("valor", result.getOrThrow(InvalidRecipeDataException::new));
    }

    @Test
    void getOrThrow_onInvalid_throwsMappedException() {
        var result = ValidationResult.invalid(List.of("err1", "err2"));
        var ex = assertThrows(InvalidRecipeDataException.class,
                () -> result.getOrThrow(InvalidRecipeDataException::new));
        assertEquals(List.of("err1", "err2"), ex.getErrors());
    }

    // --- map() ---

    @Test
    void map_onValid_transformsValue() {
        var result = ValidationResult.valid(3).map(n -> n * 2);
        assertTrue(result.isValid());
        assertEquals(6, ((ValidationResult.Valid<Integer>) result).value());
    }

    @Test
    void map_onInvalid_passesThrough() {
        ValidationResult<Integer> result = ValidationResult.invalid("fallo");
        var mapped = result.map(n -> n * 2);
        assertFalse(mapped.isValid());
        assertEquals(List.of("fallo"), mapped.errors());
    }

    // --- InvalidRecipeDataException.from() integration ---

    @Test
    void invalidRecipeDataException_from_wrapsErrors() {
        var validation = ValidationResult.combine(
                ValidationResult.invalid("El título es obligatorio"),
                ValidationResult.invalid("Se requiere al menos un paso")
        );
        var ex = InvalidRecipeDataException.from(validation);
        assertEquals(List.of("El título es obligatorio", "Se requiere al menos un paso"), ex.getErrors());
        assertTrue(ex.getMessage().contains("2 error(es)"));
    }

    @Test
    void invalidRecipeDataException_from_onValid_returnsEmptyErrorList() {
        var validation = ValidationResult.valid(null);
        var ex = InvalidRecipeDataException.from(validation);
        assertTrue(ex.getErrors().isEmpty());
    }
}
