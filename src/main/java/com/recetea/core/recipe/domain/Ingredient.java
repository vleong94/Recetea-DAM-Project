package com.recetea.core.recipe.domain;

/**
 * Entidad de dominio que representa un ingrediente dentro del catálogo del sistema.
 * Define la estructura fundamental de un recurso alimenticio, garantizando su
 * inmutabilidad para preservar la integridad de los datos durante el ciclo de vida
 * de la aplicación.
 */
public class Ingredient {

    private final int id;
    private final int categoryId;
    private final String name;

    /**
     * Constructor que inicializa el estado de la entidad.
     * Ejecuta validaciones de negocio para asegurar que el objeto nazca en un
     * estado válido, evitando la propagación de datos corruptos o incompletos.
     *
     * @param id Identificador persistente del ingrediente.
     * @param categoryId Vínculo con la categoría taxonómica.
     * @param name Nombre descriptivo del recurso.
     * @throws IngredientValidationException Si las reglas de negocio son violadas.
     */
    public Ingredient(int id, int categoryId, String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IngredientValidationException("El nombre del ingrediente es un campo obligatorio.");
        }
        this.id = id;
        this.categoryId = categoryId;
        this.name = name.trim();
    }

    public int getId() {
        return id;
    }

    public int getCategoryId() {
        return categoryId;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }

    /**
     * Excepción de dominio específica para errores en la entidad Ingredient.
     * Permite a las capas superiores identificar y gestionar fallos de validación
     * de forma semántica y diferenciada de los errores técnicos.
     */
    public static class IngredientValidationException extends RuntimeException {
        public IngredientValidationException(String message) {
            super(message);
        }
    }
}