package com.recetea.core.recipe.domain;

public record RecipeStep(int stepOrder, String instruction) {

    public RecipeStep {
        if (stepOrder <= 0) {
            throw new InvalidRecipeStepException("El orden secuencial del paso debe ser un valor positivo.");
        }

        if (instruction == null || instruction.trim().isEmpty()) {
            throw new InvalidRecipeStepException("La instrucción operativa no puede ser nula o estar vacía.");
        }

        // Normalise whitespace before storing.
        instruction = instruction.trim();
    }

    public static class InvalidRecipeStepException extends RuntimeException {
        public InvalidRecipeStepException(String message) {
            super(message);
        }
    }
}
