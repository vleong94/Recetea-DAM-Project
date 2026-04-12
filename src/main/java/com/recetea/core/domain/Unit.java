package com.recetea.core.domain;

/**
 * Entidad de Dominio: Representa una unidad de medida.
 * Sincronizado con la tabla 'unit_measures' del esquema SQL.
 */
public class Unit {
    private final int id;
    private final String name;
    private final String abbreviation; // Sincronizado con unit_measures.abbreviation

    public Unit(int id, String name, String abbreviation) {
        this.id = id;
        this.name = name;
        this.abbreviation = abbreviation;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getAbbreviation() { return abbreviation; }

    /**
     * Formato visual para los ComboBox de la UI.
     * Ejemplo: "Gramos (g)", "Mililitros (ml)"
     */
    @Override
    public String toString() {
        return name + " (" + abbreviation + ")";
    }
}