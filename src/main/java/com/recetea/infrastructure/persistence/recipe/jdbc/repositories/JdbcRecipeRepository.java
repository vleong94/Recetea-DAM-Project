package com.recetea.infrastructure.persistence.recipe.jdbc.repositories;

import com.recetea.core.recipe.domain.Recipe;
import com.recetea.core.recipe.domain.RecipeIngredient;
import com.recetea.core.recipe.application.ports.out.recipe.IRecipeRepository;
import com.recetea.infrastructure.persistence.recipe.jdbc.mappers.RecipeMapper;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Outbound Adapter (JDBC) para el Aggregate Root Recipe.
 * Gestiona la persistencia garantizando la atomicidad de las operaciones
 * mediante control transaccional (Commit/Rollback) y delega el Data Mapping.
 */
public class JdbcRecipeRepository implements IRecipeRepository {

    private final DataSource dataSource;

    /**
     * Inyecta el DataSource para aislar la gestión del Connection Pool
     * de la lógica de acceso a datos.
     */
    public JdbcRecipeRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void save(Recipe recipe) {
        String insertRecipe = "INSERT INTO recipes (user_id, category_id, difficulty_id, title, description, prep_time_min, servings) VALUES (?, ?, ?, ?, ?, ?, ?)";
        String insertIng = "INSERT INTO recipe_ingredients (recipe_id, ingredient_id, unit_id, quantity) VALUES (?, ?, ?, ?)";

        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false); // Inicia la Transaction

            try {
                try (PreparedStatement pstmt = conn.prepareStatement(insertRecipe, Statement.RETURN_GENERATED_KEYS)) {
                    pstmt.setInt(1, recipe.getUserId());
                    pstmt.setInt(2, recipe.getCategoryId());
                    pstmt.setInt(3, recipe.getDifficultyId());
                    pstmt.setString(4, recipe.getTitle());
                    pstmt.setString(5, recipe.getDescription());
                    pstmt.setInt(6, recipe.getPreparationTimeMinutes());
                    pstmt.setInt(7, recipe.getServings());
                    pstmt.executeUpdate();

                    // Recupera el ID auto-generado por el motor SQL y lo inyecta en el Aggregate Root
                    try (ResultSet keys = pstmt.getGeneratedKeys()) {
                        if (keys.next()) recipe.setId(keys.getInt(1));
                    }
                }

                // Inserta la colección de Value Objects mediante Batch Processing para optimizar I/O
                try (PreparedStatement pstmt = conn.prepareStatement(insertIng)) {
                    for (RecipeIngredient ri : recipe.getIngredients()) {
                        pstmt.setInt(1, recipe.getId());
                        pstmt.setInt(2, ri.getIngredientId());
                        pstmt.setInt(3, ri.getUnitId());
                        pstmt.setBigDecimal(4, ri.getQuantity());
                        pstmt.addBatch();
                    }
                    pstmt.executeBatch();
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Fallo de infraestructura en inserción transaccional de Recipe", e);
        }
    }

    @Override
    public Optional<Recipe> findById(int id) {
        String sqlRecipe = "SELECT * FROM recipes WHERE id_recipe = ?";
        String sqlIngredients = """
            SELECT ri.ingredient_id, ri.unit_id, ri.quantity, i.name AS ing_name, u.abbreviation AS unit_name
            FROM recipe_ingredients ri
            JOIN ingredients i ON ri.ingredient_id = i.id_ingredient
            JOIN unit_measures u ON ri.unit_id = u.id_unit
            WHERE ri.recipe_id = ?
        """;

        try (Connection conn = dataSource.getConnection()) {
            Recipe recipe = null;

            // Shallow Load: Recupera el Aggregate Root principal
            try (PreparedStatement pstmt = conn.prepareStatement(sqlRecipe)) {
                pstmt.setInt(1, id);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        recipe = RecipeMapper.mapRow(rs);
                    }
                }
            }

            // Deep Load: Hidrata la colección interna ejecutando un JOIN relacional
            if (recipe != null) {
                try (PreparedStatement pstmt = conn.prepareStatement(sqlIngredients)) {
                    pstmt.setInt(1, id);
                    try (ResultSet rs = pstmt.executeQuery()) {
                        while (rs.next()) {
                            recipe.addIngredient(RecipeMapper.mapIngredientRow(rs));
                        }
                    }
                }
            }
            return Optional.ofNullable(recipe);
        } catch (SQLException e) {
            throw new RuntimeException("Fallo de I/O al hidratar el Aggregate Root Recipe", e);
        }
    }

    @Override
    public void update(Recipe recipe) {
        String updateSql = "UPDATE recipes SET title=?, description=?, prep_time_min=?, servings=? WHERE id_recipe=?";
        String delIng = "DELETE FROM recipe_ingredients WHERE recipe_id=?";
        String insIng = "INSERT INTO recipe_ingredients (recipe_id, ingredient_id, unit_id, quantity) VALUES (?,?,?,?)";

        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);

            try {
                // Actualiza la entidad principal
                try (PreparedStatement pstmt = conn.prepareStatement(updateSql)) {
                    pstmt.setString(1, recipe.getTitle());
                    pstmt.setString(2, recipe.getDescription());
                    pstmt.setInt(3, recipe.getPreparationTimeMinutes());
                    pstmt.setInt(4, recipe.getServings());
                    pstmt.setInt(5, recipe.getId());
                    pstmt.executeUpdate();
                }

                // Estrategia Wipe & Replace para la sincronización de la colección
                try (PreparedStatement pstmt = conn.prepareStatement(delIng)) {
                    pstmt.setInt(1, recipe.getId());
                    pstmt.executeUpdate();
                }

                try (PreparedStatement pstmt = conn.prepareStatement(insIng)) {
                    for (RecipeIngredient ri : recipe.getIngredients()) {
                        pstmt.setInt(1, recipe.getId());
                        pstmt.setInt(2, ri.getIngredientId());
                        pstmt.setInt(3, ri.getUnitId());
                        pstmt.setBigDecimal(4, ri.getQuantity());
                        pstmt.addBatch();
                    }
                    pstmt.executeBatch();
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Fallo de infraestructura en actualización transaccional de Recipe", e);
        }
    }

    @Override
    public List<Recipe> findAll() {
        List<Recipe> recipes = new ArrayList<>();
        String sql = "SELECT * FROM recipes ORDER BY id_recipe DESC";

        // Retorna un Shallow Load del catálogo (omite las dependencias para evitar cuellos de botella de I/O)
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                recipes.add(RecipeMapper.mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Fallo de I/O al recuperar catálogo general de Recipes", e);
        }
        return recipes;
    }

    @Override
    public void delete(int id) {
        String delIng = "DELETE FROM recipe_ingredients WHERE recipe_id = ?";
        String delRecipe = "DELETE FROM recipes WHERE id_recipe = ?";

        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);

            try {
                // Elimina dependencias primero para purgar Constraint Violations
                try (PreparedStatement pstmt = conn.prepareStatement(delIng)) {
                    pstmt.setInt(1, id);
                    pstmt.executeUpdate();
                }

                try (PreparedStatement pstmt = conn.prepareStatement(delRecipe)) {
                    pstmt.setInt(1, id);
                    pstmt.executeUpdate();
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Fallo de infraestructura al ejecutar Delete transaccional en Recipe", e);
        }
    }
}