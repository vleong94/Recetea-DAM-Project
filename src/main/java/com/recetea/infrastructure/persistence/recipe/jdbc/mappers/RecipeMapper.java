package com.recetea.infrastructure.persistence.recipe.jdbc.mappers;

import com.recetea.core.recipe.domain.Recipe;
import com.recetea.core.recipe.domain.RecipeIngredient;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Data Mapper para el Aggregate Root Recipe.
 * Aísla el Domain Model de la infraestructura física, transformando las
 * tuplas del ResultSet en entidades inmutables y desvinculando la lógica
 * de negocio del esquema relacional.
 */
public class RecipeMapper {

    private RecipeMapper() {
        // Previene la instanciación de la clase utilitaria.
    }

    /**
     * Ejecuta el Shallow Load de la entidad principal Recipe.
     * Recupera el estado escalar del Aggregate Root y delega el ensamblaje
     * de las colecciones internas (Deep Load) a métodos especializados para
     * evitar cuellos de botella de I/O.
     *
     * @param rs Cursor posicionado en el registro activo.
     * @return Entidad Recipe con su Primary Key y estado base inyectados.
     * @throws SQLException En caso de error de lectura o tipos incompatibles.
     */
    public static Recipe mapRow(ResultSet rs) throws SQLException {
        Recipe recipe = new Recipe(
                rs.getInt("user_id"),
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
     * Reconstruye el Value Object RecipeIngredient aplicando la estrategia Deep Load.
     * Exige un ResultSet derivado de un JOIN relacional que exponga los alias
     * 'ing_name' y 'unit_name' (el cual contiene la abreviatura física de la unidad)
     * para satisfacer los requisitos de hidratación de la capa de presentación.
     *
     * @param rs Cursor posicionado en el detalle del ingrediente.
     * @return Value Object RecipeIngredient íntegro para operaciones de Query.
     * @throws SQLException Si el ResultSet carece de los alias requeridos o hay discrepancia de tipos.
     */
    public static RecipeIngredient mapIngredientRow(ResultSet rs) throws SQLException {
        return new RecipeIngredient(
                rs.getInt("ingredient_id"),
                rs.getInt("unit_id"),
                rs.getBigDecimal("quantity"),
                rs.getString("ing_name"),
                rs.getString("unit_name")
        );
    }
}