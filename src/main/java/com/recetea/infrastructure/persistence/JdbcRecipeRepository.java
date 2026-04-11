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
    public Optional<Recipe> findById(int id) { return Optional.empty(); }
    @Override
    public List<Recipe> findAll() { return new ArrayList<>(); }
    @Override
    public void delete(int id) {}
}