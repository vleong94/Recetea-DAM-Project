package com.recetea.core.recipe.domain;

/**
 * Representa una instrucción atómica y secuencial dentro del proceso de preparación de una receta.
 * Definido como un record inmutable para garantizar la integridad de los datos durante su
 * transporte y procesamiento en el Domain Model.
 * * Esta entidad asegura que cada acción operativa posea un orden lógico y un contenido descriptivo,
 * validando estructuralmente sus invariantes en el momento de la instanciación.
 *
 * @param stepOrder Valor entero que define la posición cronológica de la instrucción.
 * @param instruction Texto detallado que describe la acción técnica a ejecutar.
 */
public record RecipeStep(int stepOrder, String instruction) {

    /**
     * Constructor compacto que implementa las reglas de validación de negocio.
     * Garantiza que el objeto no sea instanciado en un estado inconsistente (Fail-Fast).
     * * @throws InvalidRecipeStepException Si el orden es inválido o la instrucción está ausente.
     */
    public RecipeStep {
        if (stepOrder <= 0) {
            throw new InvalidRecipeStepException("El orden secuencial del paso debe ser un valor positivo.");
        }

        if (instruction == null || instruction.trim().isEmpty()) {
            throw new InvalidRecipeStepException("La instrucción operativa no puede ser nula o estar vacía.");
        }

        // Sanitización de la entrada para eliminar espacios redundantes.
        instruction = instruction.trim();
    }

    /**
     * Excepción de dominio especializada para capturar violaciones de integridad
     * en la definición de los pasos de preparación.
     */
    public static class InvalidRecipeStepException extends RuntimeException {
        public InvalidRecipeStepException(String message) {
            super(message);
        }
    }
}