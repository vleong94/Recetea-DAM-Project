package com.recetea.infrastructure.persistence;

import com.recetea.core.domain.Recipe;
import com.recetea.core.domain.RecipeIngredient;
import com.recetea.core.ports.IRecipeRepository;

import java.io.InputStream;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

public class JdbcRecipeRepository implements IRecipeRepository {

    private static final String DB_URL;
    private static final String DB_USER;
    private static final String DB_PASSWORD;

    static {
        Properties props = new Properties();
        try (InputStream input = JdbcRecipeRepository.class.getClassLoader().getResourceAsStream("application-local.properties")) {
            if (input == null) {
                throw new RuntimeException("CRÍTICO: No se encontró application-local.properties");
            }
            props.load(input);
            DB_URL = props.getProperty("db.url");
            DB_USER = props.getProperty("db.user");
            DB_PASSWORD = props.getProperty("db.password");
        } catch (Exception e) {
            throw new RuntimeException("Error en inicialización", e);
        }
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }

    @Override
    public void save(Recipe recipe) {
        String insertRecipeQuery = "INSERT INTO recipes (user_id, category_id, difficulty_id, title, description, prep_time_min, servings) VALUES (?, ?, ?, ?, ?, ?, ?)";
        String insertIngredientQuery = "INSERT INTO recipe_ingredients (recipe_id, ingredient_id, unit_id, quantity) VALUES (?, ?, ?, ?)";

        try (Connection conn = getConnection()) {
            try {
                // 1. INICIO DE TRANSACCIÓN ACID
                conn.setAutoCommit(false);

                // 2. GUARDAR RECETA (Cabecera)
                try (PreparedStatement pstmtRecipe = conn.prepareStatement(insertRecipeQuery, Statement.RETURN_GENERATED_KEYS)) {
                    pstmtRecipe.setInt(1, recipe.getUserId());
                    pstmtRecipe.setInt(2, recipe.getCategoryId());
                    pstmtRecipe.setInt(3, recipe.getDifficultyId());
                    pstmtRecipe.setString(4, recipe.getTitle());
                    pstmtRecipe.setString(5, recipe.getDescription());
                    pstmtRecipe.setInt(6, recipe.getPreparationTimeMinutes());
                    pstmtRecipe.setInt(7, recipe.getServings());

                    if (pstmtRecipe.executeUpdate() == 0) {
                        throw new SQLException("Fallo al insertar la receta cabecera.");
                    }

                    try (ResultSet generatedKeys = pstmtRecipe.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            recipe.setId(generatedKeys.getInt(1));
                        } else {
                            throw new SQLException("Fallo al recuperar el ID de la receta.");
                        }
                    }
                }

                // 3. GUARDAR INGREDIENTES (Batch Processing)
                if (!recipe.getIngredients().isEmpty()) {
                    try (PreparedStatement pstmtIng = conn.prepareStatement(insertIngredientQuery)) {
                        for (RecipeIngredient ri : recipe.getIngredients()) {
                            pstmtIng.setInt(1, recipe.getId()); // El ID que acabamos de generar arriba
                            pstmtIng.setInt(2, ri.getIngredientId());
                            pstmtIng.setInt(3, ri.getUnitId());
                            pstmtIng.setDouble(4, ri.getQuantity());
                            pstmtIng.addBatch(); // Añadimos al lote en RAM
                        }
                        pstmtIng.executeBatch(); // Disparamos todos los inserts de golpe
                    }
                }

                // 4. ÉXITO: Confirmamos los cambios en disco
                conn.commit();

            } catch (SQLException ex) {
                // 5. FRACASO: Detectamos error, destruimos la operación incompleta
                conn.rollback();
                throw new RuntimeException("OPERACIÓN ABORTADA (Rollback ejecutado): " + ex.getMessage(), ex);
            } finally {
                // 6. LIMPIEZA: Devolvemos la conexión a su estado original
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error crítico de base de datos.", e);
        }
    }

    @Override
    public Optional<Recipe> findById(int id) {
        Recipe recipe = null;

        // Query 1: Extraer la cabecera
        String recipeQuery = "SELECT id_recipe, user_id, category_id, difficulty_id, title, description, prep_time_min, servings FROM recipes WHERE id_recipe = ?";
        // Query 2: Extraer la tabla de relaciones
        String ingredientsQuery = "SELECT ingredient_id, unit_id, quantity FROM recipe_ingredients WHERE recipe_id = ?";

        try (Connection conn = getConnection()) {

            // FASE 1: Instanciar el Aggregate Root
            try (PreparedStatement pstmt = conn.prepareStatement(recipeQuery)) {
                pstmt.setInt(1, id);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        recipe = new Recipe(
                                rs.getInt("user_id"),
                                rs.getInt("category_id"),
                                rs.getInt("difficulty_id"),
                                rs.getString("title"),
                                rs.getString("description"),
                                rs.getInt("prep_time_min"),
                                rs.getInt("servings")
                        );
                        recipe.setId(rs.getInt("id_recipe"));
                    }
                }
            }

            // FASE 2: Hidratar la entidad con sus Sub-entidades (Si la receta existe)
            if (recipe != null) {
                try (PreparedStatement pstmt = conn.prepareStatement(ingredientsQuery)) {
                    pstmt.setInt(1, id);
                    try (ResultSet rs = pstmt.executeQuery()) {
                        while (rs.next()) {
                            // Inyectamos cada ingrediente en la memoria del objeto Recipe
                            recipe.addIngredient(new com.recetea.core.domain.RecipeIngredient(
                                    rs.getInt("ingredient_id"),
                                    rs.getInt("unit_id"),
                                    rs.getDouble("quantity")
                            ));
                        }
                    }
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error crítico de hidratación al extraer la receta ID: " + id, e);
        }

        return Optional.ofNullable(recipe);
    }

    @Override
    public List<Recipe> findAll() {
        List<Recipe> recipes = new ArrayList<>();
        // Query de extracción. Ordenamos por los más recientes.
        String query = "SELECT id_recipe, user_id, category_id, difficulty_id, title, description, prep_time_min, servings FROM recipes ORDER BY created_at DESC";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {

            // Iteración del ResultSet: Transformamos cada fila SQL en un objeto Java
            while (rs.next()) {
                Recipe recipe = new Recipe(
                        rs.getInt("user_id"),
                        rs.getInt("category_id"),
                        rs.getInt("difficulty_id"),
                        rs.getString("title"),
                        rs.getString("description"),
                        rs.getInt("prep_time_min"),
                        rs.getInt("servings")
                );
                // Inyectamos el ID que viene de la base de datos
                recipe.setId(rs.getInt("id_recipe"));
                recipes.add(recipe);
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error crítico al extraer el catálogo de recetas desde PostgreSQL", e);
        }

        return recipes;
    }

    @Override
    public void delete(int id) {}
}