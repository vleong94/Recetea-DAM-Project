package com.recetea.infrastructure.persistence.jbdc;

import com.recetea.core.domain.Recipe;
import com.recetea.core.domain.RecipeIngredient;
import com.recetea.core.ports.out.IRecipeRepository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Infrastructure Layer: Adaptador JDBC para la entidad Recipe.
 * Se encarga de la persistencia física en PostgreSQL.
 * Recibe la configuración por constructor para garantizar el desacoplamiento.
 */
public class JdbcRecipeRepository implements IRecipeRepository {

    private final String url;
    private final String user;
    private final String password;

    /**
     * Constructor para Inyección de Dependencias.
     * Permite que el Composition Root (Main) decida el origen de las credenciales.
     */
    public JdbcRecipeRepository(String url, String user, String password) {
        this.url = url;
        this.user = user;
        this.password = password;
    }

    /**
     * Centraliza la obtención de la conexión para evitar código redundante.
     */
    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    @Override
    public void save(Recipe recipe) {
        String insertRecipe = "INSERT INTO recipes (user_id, category_id, difficulty_id, title, description, prep_time_min, servings) VALUES (?, ?, ?, ?, ?, ?, ?)";
        String insertIng = "INSERT INTO recipe_ingredients (recipe_id, ingredient_id, unit_id, quantity) VALUES (?, ?, ?, ?)";

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false); // Operación transaccional: Todo o nada
            try {
                // 1. Guardar la cabecera de la receta
                try (PreparedStatement pstmt = conn.prepareStatement(insertRecipe, Statement.RETURN_GENERATED_KEYS)) {
                    pstmt.setInt(1, recipe.getUserId());
                    pstmt.setInt(2, recipe.getCategoryId());
                    pstmt.setInt(3, recipe.getDifficultyId());
                    pstmt.setString(4, recipe.getTitle());
                    pstmt.setString(5, recipe.getDescription());
                    pstmt.setInt(6, recipe.getPreparationTimeMinutes());
                    pstmt.setInt(7, recipe.getServings());
                    pstmt.executeUpdate();

                    try (ResultSet keys = pstmt.getGeneratedKeys()) {
                        if (keys.next()) recipe.setId(keys.getInt(1)); // Hidratamos el ID generado
                    }
                }

                // 2. Guardar el detalle de ingredientes (Batch)
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
            throw new RuntimeException("Error en la persistencia transaccional de la receta", e);
        }
    }

    @Override
    public Optional<Recipe> findById(int id) {
        String sqlRecipe = "SELECT * FROM recipes WHERE id_recipe = ?";

        // JOIN para hidratar nombres descriptivos (UX) en una sola consulta
        String sqlIngredients = """
            SELECT ri.ingredient_id, ri.unit_id, ri.quantity, i.name AS ing_name, u.abbreviation AS unit_name
            FROM recipe_ingredients ri
            JOIN ingredients i ON ri.ingredient_id = i.id_ingredient
            JOIN unit_measures u ON ri.unit_id = u.id_unit
            WHERE ri.recipe_id = ?
        """;

        try (Connection conn = getConnection()) {
            Recipe recipe = null;
            try (PreparedStatement pstmt = conn.prepareStatement(sqlRecipe)) {
                pstmt.setInt(1, id);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        recipe = new Recipe(rs.getInt("user_id"), rs.getInt("category_id"), rs.getInt("difficulty_id"),
                                rs.getString("title"), rs.getString("description"), rs.getInt("prep_time_min"), rs.getInt("servings"));
                        recipe.setId(rs.getInt("id_recipe"));
                    }
                }
            }

            if (recipe != null) {
                try (PreparedStatement pstmt = conn.prepareStatement(sqlIngredients)) {
                    pstmt.setInt(1, id);
                    try (ResultSet rs = pstmt.executeQuery()) {
                        while (rs.next()) {
                            // Constructor de 5 parámetros de RecipeIngredient
                            recipe.addIngredient(new RecipeIngredient(
                                    rs.getInt("ingredient_id"),
                                    rs.getInt("unit_id"),
                                    rs.getBigDecimal("quantity"),
                                    rs.getString("ing_name"),
                                    rs.getString("unit_name")
                            ));
                        }
                    }
                }
            }
            return Optional.ofNullable(recipe);
        } catch (SQLException e) {
            throw new RuntimeException("Error al recuperar los detalles de la receta", e);
        }
    }

    @Override
    public void update(Recipe recipe) {
        String updateSql = "UPDATE recipes SET title=?, description=?, prep_time_min=?, servings=? WHERE id_recipe=?";
        String delIng = "DELETE FROM recipe_ingredients WHERE recipe_id=?";
        String insIng = "INSERT INTO recipe_ingredients (recipe_id, ingredient_id, unit_id, quantity) VALUES (?,?,?,?)";

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement pstmt = conn.prepareStatement(updateSql)) {
                    pstmt.setString(1, recipe.getTitle());
                    pstmt.setString(2, recipe.getDescription());
                    pstmt.setInt(3, recipe.getPreparationTimeMinutes());
                    pstmt.setInt(4, recipe.getServings());
                    pstmt.setInt(5, recipe.getId());
                    pstmt.executeUpdate();
                }
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
            throw new RuntimeException("Error en la actualización transaccional", e);
        }
    }

    @Override
    public List<Recipe> findAll() {
        List<Recipe> recipes = new ArrayList<>();
        String sql = "SELECT * FROM recipes ORDER BY id_recipe DESC";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                Recipe r = new Recipe(rs.getInt("user_id"), rs.getInt("category_id"), rs.getInt("difficulty_id"),
                        rs.getString("title"), rs.getString("description"), rs.getInt("prep_time_min"), rs.getInt("servings"));
                r.setId(rs.getInt("id_recipe"));
                recipes.add(r);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al recuperar el catálogo de recetas", e);
        }
        return recipes;
    }

    @Override
    public void delete(int id) {
        String sql = "DELETE FROM recipes WHERE id_recipe = ?";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al eliminar la receta", e);
        }
    }
}