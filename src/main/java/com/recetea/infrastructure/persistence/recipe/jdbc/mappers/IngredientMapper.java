package com.recetea.infrastructure.persistence.recipe.jdbc.mappers;

import com.recetea.core.recipe.domain.Ingredient;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Data Mapper para la entidad Ingredient.
 * Transforma registros relacionales en objetos de dominio inmutables,
 * aislando el esquema de la base de datos de los repositorios y la lógica de negocio.
 */
public class IngredientMapper {

    private IngredientMapper() {
        // Clase utilitaria. Evita la instanciación.
    }

    /**
     * Construye una entidad Ingredient a partir de un registro SQL.
     * Extrae los atributos del ResultSet y los inyecta directamente en el constructor
     * para garantizar la integridad estructural y la inmutabilidad del objeto.
     *
     * @param rs ResultSet posicionado en la fila actual.
     * @return Entidad Ingredient instanciada con su identidad y estado completos.
     * @throws SQLException Si falla el acceso a las columnas.
     */
    public static Ingredient mapRow(ResultSet rs) throws SQLException {
        return new Ingredient(
                rs.getInt("id_ingredient"),
                rs.getInt("ing_category_id"),
                rs.getString("name")
        );
    }
}