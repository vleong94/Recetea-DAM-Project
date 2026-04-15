package com.recetea.infrastructure.persistence.recipe.jdbc.mappers;

import com.recetea.core.recipe.domain.Recipe;
import com.recetea.core.recipe.domain.RecipeIngredient;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Data Mapper especializado en la transformación de registros relacionales a objetos del dominio.
 * Actúa como un traductor en la capa de infraestructura, permitiendo que el motor de persistencia
 * JDBC hidrate el Aggregate Root (Recipe) sin que el dominio conozca detalles del esquema SQL.
 * Garantiza el cumplimiento del agnosticismo tecnológico al centralizar la extracción de datos.
 */
public class RecipeMapper {

    private RecipeMapper() {
        // Clase utilitaria. Se restringe la instanciación para mantener la cohesión.
    }

    /**
     * Realiza el mapeo superficial (Shallow Load) de la cabecera de una receta.
     * Transforma las columnas de la tabla 'recipes' en una instancia válida de la entidad.
     */
    public static Recipe mapRow(ResultSet rs) throws SQLException {
        Recipe.AuthorId authorId = new Recipe.AuthorId(rs.getInt("user_id"));

        Recipe recipe = new Recipe(
                authorId,
                rs.getInt("category_id"),
                rs.getInt("difficulty_id"),
                rs.getString("title"),
                rs.getString("description"),
                rs.getInt("prep_time_min"),
                rs.getInt("servings")
        );

        recipe.setId(rs.getInt("id_recipe"));

        return recipe;
    }

    /**
     * Ejecuta el mapeo de los componentes de la receta mediante la estrategia de carga profunda.
     * Reconstruye el Value Object RecipeIngredient a partir de proyecciones relacionales.
     * Utiliza el alias de la abreviatura (unit_abbr) para cumplir con el contrato de
     * integridad del dominio y los requisitos de validación de los tests de integración.
     *
     * @param rs ResultSet con los datos del ingrediente y sus alias maestros.
     * @return Objeto RecipeIngredient inmutable con metadatos de visualización integrados.
     * @throws SQLException Si los alias esperados no están presentes en la consulta SQL.
     */
    public static RecipeIngredient mapIngredientRow(ResultSet rs) throws SQLException {
        return new RecipeIngredient(
                rs.getInt("ingredient_id"),
                rs.getInt("unit_id"),
                rs.getBigDecimal("quantity"),
                rs.getString("ing_name"),
                rs.getString("unit_abbr")
        );
    }
}