package com.recetea.core.recipe.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Entidad de dominio que actúa como raíz del agregado (Aggregate Root) para las recetas.
 * Representa el estado y comportamiento central del negocio, garantizando la consistencia
 * transaccional de la receta y sus componentes de forma aislada a la infraestructura.
 * Aplica reglas estrictas de validación para evitar estados en memoria inconsistentes.
 */
public class Recipe {

    private Integer id;
    private int userId;
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
     * Constructor principal que inicializa el estado del Aggregate Root.
     * Ejecuta validaciones de negocio esenciales (Invariants) para asegurar la integridad
     * de los datos desde el momento exacto de su instanciación, garantizando simetría
     * con las restricciones persistentes del sistema.
     *
     * @throws RecipeValidationException Si se vulneran las reglas de integridad general.
     * @throws InvalidRecipeMetricException Si las métricas de capacidad o tiempo son ilógicas.
     */
    public Recipe(int userId, int categoryId, int difficultyId, String title, String description, int prepTime, int servings) {
        if (title == null || title.trim().isEmpty()) {
            throw new RecipeValidationException("El título de la receta es un campo obligatorio.");
        }
        if (prepTime <= 0) {
            throw new InvalidRecipeMetricException("El tiempo de preparación debe ser una magnitud estrictamente positiva.");
        }
        if (servings <= 0) {
            throw new InvalidRecipeMetricException("El rendimiento en raciones debe ser mayor que cero.");
        }

        this.userId = userId;
        this.categoryId = categoryId;
        this.difficultyId = difficultyId;
        this.title = title.trim();
        this.description = description != null ? description.trim() : "";
        this.preparationTimeMinutes = prepTime;
        this.servings = servings;
        this.ingredients = new ArrayList<>();
    }

    // --- LÓGICA DE DOMINIO ---

    /**
     * Incorpora un nuevo ingrediente a la composición de la receta.
     * Actúa como punto de control para la colección interna, evitando inserciones nulas.
     */
    public void addIngredient(RecipeIngredient ingredient) {
        if (ingredient != null) {
            this.ingredients.add(ingredient);
        }
    }

    /**
     * Sobrescribe la colección completa de ingredientes.
     * Se utiliza principalmente en procesos de reconciliación de estado o hidratación profunda.
     */
    public void setIngredients(List<RecipeIngredient> ingredients) {
        this.ingredients = ingredients != null ? new ArrayList<>(ingredients) : new ArrayList<>();
    }

    /**
     * Purga la composición actual de la receta eliminando todos sus ingredientes.
     * Mantiene el control del estado interno garantizando que la colección se modifique
     * exclusivamente a través de las operaciones autorizadas por el Aggregate Root.
     */
    public void clearIngredients() {
        this.ingredients.clear();
    }

    // --- ACCESORES Y MUTADORES CON INVARIANTES ---

    public Integer getId() { return id; }

    /**
     * Asigna la identidad persistente a la entidad.
     * Incorpora un bloqueo estructural: una vez que el Data Store o el sistema
     * hidrata la identidad, esta se vuelve inmutable para prevenir la corrupción
     * o secuestro del registro en memoria.
     *
     * @throws IllegalStateException Si se intenta sobrescribir un ID existente.
     */
    public void setId(Integer id) {
        if (this.id != null) {
            throw new IllegalStateException("Integrity Error: La identidad de la receta es inmutable una vez asignada.");
        }
        this.id = id;
    }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public int getCategoryId() { return categoryId; }
    public void setCategoryId(int categoryId) { this.categoryId = categoryId; }

    public int getDifficultyId() { return difficultyId; }
    public void setDifficultyId(int difficultyId) { this.difficultyId = difficultyId; }

    public String getTitle() { return title; }

    /**
     * Modifica el título de la receta aplicando las reglas de validación de negocio.
     *
     * @throws RecipeValidationException Si el nuevo título es nulo o vacío.
     */
    public void setTitle(String title) {
        if (title == null || title.trim().isEmpty()) {
            throw new RecipeValidationException("El título de la receta es un campo obligatorio.");
        }
        this.title = title.trim();
    }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getPreparationTimeMinutes() { return preparationTimeMinutes; }

    /**
     * Ajusta el tiempo de preparación garantizando la integridad matemática del dominio.
     */
    public void setPreparationTimeMinutes(int preparationTimeMinutes) {
        if (preparationTimeMinutes <= 0) {
            throw new InvalidRecipeMetricException("El tiempo de preparación debe ser una magnitud estrictamente positiva.");
        }
        this.preparationTimeMinutes = preparationTimeMinutes;
    }

    public int getServings() { return servings; }

    /**
     * Modifica el rendimiento de la receta garantizando que el valor sea operativo.
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
     * que eludan las reglas del Aggregate Root.
     */
    public List<RecipeIngredient> getIngredients() {
        return Collections.unmodifiableList(ingredients);
    }

    // --- EXCEPCIONES DE DOMINIO ---

    /**
     * Excepción de dominio base para encapsular violaciones estructurales
     * dentro del Aggregate Root.
     */
    public static class RecipeValidationException extends RuntimeException {
        public RecipeValidationException(String message) {
            super(message);
        }
    }

    /**
     * Excepción especializada para fallos de integridad matemática o métrica
     * en la configuración de la receta (tiempos, capacidades, raciones).
     */
    public static class InvalidRecipeMetricException extends RecipeValidationException {
        public InvalidRecipeMetricException(String message) {
            super(message);
        }
    }
}