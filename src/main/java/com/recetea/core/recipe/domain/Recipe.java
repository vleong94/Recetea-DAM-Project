package com.recetea.core.recipe.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Entidad de dominio que actúa como raíz del agregado (Aggregate Root) para las recetas.
 * Representa el estado y comportamiento central del negocio, garantizando la consistencia
 * transaccional de la receta y sus componentes de forma aislada a la infraestructura.
 * Implementa una abstracción de la identidad del autor mediante un objeto de valor (Value Object)
 * para asegurar el desacoplamiento con el módulo de gestión de usuarios.
 */
public class Recipe {

    private Integer id;
    private AuthorId authorId;
    private int categoryId;
    private int difficultyId;
    private String title;
    private String description;
    private int preparationTimeMinutes;
    private int servings;

    /**
     * Colección interna de ingredientes que componen la receta.
     * Su gestión se encapsula dentro de esta entidad para proteger los invariantes del agregado.
     */
    private List<RecipeIngredient> ingredients;

    /**
     * Value Object inmutable que representa el identificador único del autor.
     * Encapsula la identidad para permitir que el dominio de recetas sea agnóstico
     * a la implementación detallada de los perfiles de usuario.
     */
    public record AuthorId(int value) {
        public AuthorId {
            if (value <= 0) {
                throw new IllegalArgumentException("El identificador del autor debe ser un valor positivo.");
            }
        }
    }

    /**
     * Constructor principal que inicializa el estado del Aggregate Root.
     * Ejecuta validaciones de negocio esenciales (Invariants) para asegurar la integridad
     * de los datos desde el momento exacto de su instanciación, garantizando simetría
     * con las restricciones persistentes del sistema.
     *
     * @throws RecipeValidationException Si se vulneran las reglas de integridad general.
     * @throws InvalidRecipeMetricException Si las métricas de capacidad o tiempo son inconsistentes.
     */
    public Recipe(AuthorId authorId, int categoryId, int difficultyId, String title,
                  String description, int preparationTimeMinutes, int servings) {
        validateInvariants(title, preparationTimeMinutes, servings);

        this.authorId = authorId;
        this.categoryId = categoryId;
        this.difficultyId = difficultyId;
        this.title = title.trim();
        this.description = description != null ? description.trim() : "";
        this.preparationTimeMinutes = preparationTimeMinutes;
        this.servings = servings;
        this.ingredients = new ArrayList<>();
    }

    /**
     * Verifica la validez de los atributos críticos de la receta antes de la asignación.
     */
    private void validateInvariants(String title, int time, int servings) {
        if (title == null || title.trim().isEmpty()) {
            throw new RecipeValidationException("El título de la receta es un campo obligatorio.");
        }
        if (time <= 0) {
            throw new InvalidRecipeMetricException("El tiempo de preparación debe ser mayor que cero.");
        }
        if (servings <= 0) {
            throw new InvalidRecipeMetricException("El rendimiento en raciones debe ser mayor que cero.");
        }
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public AuthorId getAuthorId() {
        return authorId;
    }

    public void setAuthorId(AuthorId authorId) {
        this.authorId = authorId;
    }

    public int getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(int categoryId) {
        this.categoryId = categoryId;
    }

    public int getDifficultyId() {
        return difficultyId;
    }

    public void setDifficultyId(int difficultyId) {
        this.difficultyId = difficultyId;
    }

    public String getTitle() {
        return title;
    }

    /**
     * Modifica el título aplicando validaciones de integridad y limpieza de espacios.
     */
    public void setTitle(String title) {
        if (title == null || title.trim().isEmpty()) {
            throw new RecipeValidationException("El título no puede ser nulo o vacío.");
        }
        this.title = title.trim();
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description != null ? description.trim() : "";
    }

    public int getPreparationTimeMinutes() {
        return preparationTimeMinutes;
    }

    /**
     * Actualiza el tiempo de preparación garantizando una métrica positiva.
     */
    public void setPreparationTimeMinutes(int preparationTimeMinutes) {
        if (preparationTimeMinutes <= 0) {
            throw new InvalidRecipeMetricException("El tiempo de preparación debe ser mayor que cero.");
        }
        this.preparationTimeMinutes = preparationTimeMinutes;
    }

    public int getServings() {
        return servings;
    }

    /**
     * Modifica el rendimiento de la receta bajo reglas estrictas de métrica.
     */
    public void setServings(int servings) {
        if (servings <= 0) {
            throw new InvalidRecipeMetricException("El rendimiento en raciones debe ser mayor que cero.");
        }
        this.servings = servings;
    }

    /**
     * Expone la composición de la receta de forma segura.
     * Retorna una vista inmutable de la colección para prevenir modificaciones externas
     * que eludan las reglas de validación del Aggregate Root.
     */
    public List<RecipeIngredient> getIngredients() {
        return Collections.unmodifiableList(ingredients);
    }

    /**
     * Añade un nuevo componente a la receta de forma atómica.
     */
    public void addIngredient(RecipeIngredient ingredient) {
        if (ingredient != null) {
            this.ingredients.add(ingredient);
        }
    }

    /**
     * Actualiza la colección completa de ingredientes, asegurando la consistencia interna.
     */
    public void setIngredients(List<RecipeIngredient> ingredients) {
        this.ingredients = new ArrayList<>(ingredients != null ? ingredients : Collections.emptyList());
    }

    /**
     * Elimina todos los elementos de la composición de la receta.
     */
    public void clearIngredients() {
        this.ingredients.clear();
    }

    // --- EXCEPCIONES DE DOMINIO ---

    /**
     * Excepción base para encapsular violaciones de reglas de negocio en la receta.
     */
    public static class RecipeValidationException extends RuntimeException {
        public RecipeValidationException(String message) {
            super(message);
        }
    }

    /**
     * Excepción especializada para errores en métricas temporales o de capacidad.
     */
    public static class InvalidRecipeMetricException extends RecipeValidationException {
        public InvalidRecipeMetricException(String message) {
            super(message);
        }
    }
}