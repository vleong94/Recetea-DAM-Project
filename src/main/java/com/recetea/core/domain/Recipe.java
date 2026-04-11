package com.recetea.core.domain;

import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.util.List;

/**
 * Aggregate Root: Recipe
 * Refactorizado para alinearse con el Modelo Relacional estricto e integrar
 * la composición de ingredientes.
 */
public class Recipe {

    // El ID ahora es numérico y nulo por defecto (hasta que la base de datos lo asigne)
    private Integer id;

    // Relaciones (Foreign Keys en DB)
    private final IntegerProperty userId;
    private final IntegerProperty categoryId;
    private final IntegerProperty difficultyId;

    // Metadatos
    private final StringProperty title;
    private final StringProperty description;
    private final IntegerProperty preparationTimeMinutes;
    private final IntegerProperty servings;

    // Composición relacional
    private final ListProperty<RecipeIngredient> ingredients;

    public Recipe(int userId, int categoryId, int difficultyId, String title, String description, int prepTime, int servings) {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("El título de la receta no puede estar vacío");
        }

        this.userId = new SimpleIntegerProperty(userId);
        this.categoryId = new SimpleIntegerProperty(categoryId);
        this.difficultyId = new SimpleIntegerProperty(difficultyId);
        this.title = new SimpleStringProperty(title.trim());
        this.description = new SimpleStringProperty(description != null ? description.trim() : "");
        this.preparationTimeMinutes = new SimpleIntegerProperty(prepTime);
        this.servings = new SimpleIntegerProperty(servings);
        this.ingredients = new SimpleListProperty<>(FXCollections.observableArrayList());
    }

    // --- COMPORTAMIENTO DE DOMINIO ---

    public void addIngredient(RecipeIngredient ingredient) {
        if (ingredient != null) {
            this.ingredients.add(ingredient);
        }
    }

    /**
     * Permite actualizar la lista completa de ingredientes.
     * Vital para el proceso de reconciliación en el Update.
     */
    public void setIngredients(List<RecipeIngredient> ingredients) {
        if (ingredients != null) {
            this.ingredients.get().setAll(ingredients);
        }
    }

    // --- GETTERS, SETTERS & PROPERTIES ---

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; } // Se llamará cuando PostgreSQL nos devuelva el ID autogenerado

    public int getUserId() { return userId.get(); }
    public int getCategoryId() { return categoryId.get(); }
    public int getDifficultyId() { return difficultyId.get(); }

    public String getTitle() { return title.get(); }
    public String getDescription() { return description.get(); }
    public int getPreparationTimeMinutes() { return preparationTimeMinutes.get(); }
    public int getServings() { return servings.get(); }
    public ObservableList<RecipeIngredient> getIngredients() { return ingredients.get(); }
}