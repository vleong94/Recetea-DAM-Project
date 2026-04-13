package com.recetea.core.recipe.domain;

/**
 * Entidad de dominio que representa una unidad de medida estándar en el sistema.
 * Mantiene un estado inmutable para garantizar la consistencia de los cálculos
 * y representaciones a lo largo del ciclo de vida de la aplicación.
 */
public class Unit {

    private final int id;
    private final String name;
    private final String abbreviation;

    /**
     * Constructor que inicializa el estado de la entidad.
     * Aplica reglas de validación de negocio para garantizar que la unidad
     * se instancie con información descriptiva obligatoria, evitando la
     * propagación de estados inconsistentes.
     *
     * @param id Identificador persistente de la unidad de medida.
     * @param name Nombre descriptivo de la unidad.
     * @param abbreviation Símbolo o abreviatura técnica.
     * @throws UnitValidationException Si se incumplen las reglas de integridad de los datos.
     */
    public Unit(int id, String name, String abbreviation) {
        if (name == null || name.trim().isEmpty()) {
            throw new UnitValidationException("El nombre de la unidad es un campo obligatorio.");
        }
        if (abbreviation == null || abbreviation.trim().isEmpty()) {
            throw new UnitValidationException("La abreviatura de la unidad es un campo obligatorio.");
        }

        this.id = id;
        this.name = name.trim();
        this.abbreviation = abbreviation.trim();
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getAbbreviation() {
        return abbreviation;
    }

    @Override
    public String toString() {
        return name + " (" + abbreviation + ")";
    }

    /**
     * Excepción de dominio específica para errores de validación en la entidad Unit.
     * Permite a los adaptadores de infraestructura y capas superiores capturar e
     * identificar fallos semánticos de negocio separados de excepciones técnicas.
     */
    public static class UnitValidationException extends RuntimeException {
        public UnitValidationException(String message) {
            super(message);
        }
    }
}