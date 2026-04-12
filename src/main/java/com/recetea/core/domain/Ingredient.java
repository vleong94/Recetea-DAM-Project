package com.recetea.core.domain;

/**
 * Entidad de Dominio: Representa un ingrediente del catálogo global.
 * Sincronizado con la tabla 'ingredients' del esquema SQL.
 */
public class Ingredient {
    private final int id;
    private final int categoryId; // Sincronizado con ing_category_id
    private final String name;

    public Ingredient(int id, int categoryId, String name) {
        this.id = id;
        this.categoryId = categoryId;
        this.name = name;
    }

    public int getId() { return id; }
    public int getCategoryId() { return categoryId; }
    public String getName() { return name; }

    /**
     * El ComboBox de JavaFX utiliza automáticamente este método para
     * mostrar el texto en el desplegable.
     */
    @Override
    public String toString() {
        return name;
    }
}