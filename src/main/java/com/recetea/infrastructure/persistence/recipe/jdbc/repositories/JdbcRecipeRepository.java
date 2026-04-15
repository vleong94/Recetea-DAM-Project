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
 * Adaptador de salida basado en JDBC para la gestión de persistencia de recetas.
 * Implementa el contrato del repositorio mediante el uso de transacciones manuales
 * y el patrón Data Mapper, asegurando el aislamiento del modelo de dominio.
 * Utiliza sentencias SQL preparadas y centralizadas para mitigar riesgos de
 * inyección y facilitar el mantenimiento evolutivo del esquema.
 */
public class JdbcRecipeRepository implements IRecipeRepository {

    private final DataSource dataSource;

    // --- Definición de Sentencias SQL Estáticas ---
    private static final String INSERT_RECIPE =
            "INSERT INTO recipes (user_id, category_id, difficulty_id, title, description, prep_time_min, servings) VALUES (?, ?, ?, ?, ?, ?, ?)";

    private static final String INSERT_INGREDIENT =
            "INSERT INTO recipe_ingredients (recipe_id, ingredient_id, unit_id, quantity) VALUES (?, ?, ?, ?)";

    private static final String UPDATE_RECIPE =
            "UPDATE recipes SET category_id = ?, difficulty_id = ?, title = ?, description = ?, prep_time_min = ?, servings = ?, user_id = ? WHERE id_recipe = ?";

    private static final String DELETE_INGREDIENTS =
            "DELETE FROM recipe_ingredients WHERE recipe_id = ?";

    private static final String DELETE_RECIPE =
            "DELETE FROM recipes WHERE id_recipe = ?";

    private static final String SELECT_BY_ID =
            "SELECT id_recipe, user_id, category_id, difficulty_id, title, description, prep_time_min, servings FROM recipes WHERE id_recipe = ?";

    /**
     * Consulta optimizada para la carga profunda de ingredientes.
     * Recupera la abreviatura de la unidad (um.abbreviation) en lugar del nombre completo
     * para satisfacer los requisitos de integridad del Value Object del dominio.
     */
    private static final String SELECT_INGREDIENTS =
            "SELECT ri.ingredient_id, ri.unit_id, ri.quantity, i.name as ing_name, um.abbreviation as unit_abbr " +
                    "FROM recipe_ingredients ri " +
                    "JOIN ingredients i ON ri.ingredient_id = i.id_ingredient " +
                    "JOIN unit_measures um ON ri.unit_id = um.id_unit " +
                    "WHERE ri.recipe_id = ?";

    private static final String SELECT_ALL =
            "SELECT id_recipe, user_id, category_id, difficulty_id, title, description, prep_time_min, servings FROM recipes ORDER BY created_at DESC";

    public JdbcRecipeRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Registra una nueva receta ejecutando una transacción atómica.
     * Gestiona la creación del registro principal y la inserción por lotes de la
     * composición de ingredientes, recuperando la identidad generada por la base de datos.
     */
    @Override
    public void save(Recipe recipe) {
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement ps = conn.prepareStatement(INSERT_RECIPE, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, recipe.getAuthorId().value());
                    ps.setInt(2, recipe.getCategoryId());
                    ps.setInt(3, recipe.getDifficultyId());
                    ps.setString(4, recipe.getTitle());
                    ps.setString(5, recipe.getDescription());
                    ps.setInt(6, recipe.getPreparationTimeMinutes());
                    ps.setInt(7, recipe.getServings());
                    ps.executeUpdate();

                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        if (rs.next()) {
                            recipe.setId(rs.getInt(1));
                        }
                    }
                }

                saveIngredients(conn, recipe);
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error de infraestructura al persistir la receta.", e);
        }
    }

    /**
     * Actualiza el estado de una receta existente mediante el patrón Wipe & Replace.
     * Sincroniza la cabecera de la receta y regenera íntegramente la lista de
     * ingredientes dentro de un bloque transaccional para garantizar la consistencia.
     */
    @Override
    public void update(Recipe recipe) {
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement ps = conn.prepareStatement(UPDATE_RECIPE)) {
                    ps.setInt(1, recipe.getCategoryId());
                    ps.setInt(2, recipe.getDifficultyId());
                    ps.setString(3, recipe.getTitle());
                    ps.setString(4, recipe.getDescription());
                    ps.setInt(5, recipe.getPreparationTimeMinutes());
                    ps.setInt(6, recipe.getServings());
                    ps.setInt(7, recipe.getAuthorId().value());
                    ps.setInt(8, recipe.getId());
                    ps.executeUpdate();
                }

                try (PreparedStatement ps = conn.prepareStatement(DELETE_INGREDIENTS)) {
                    ps.setInt(1, recipe.getId());
                    ps.executeUpdate();
                }

                saveIngredients(conn, recipe);
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error de infraestructura al actualizar la receta.", e);
        }
    }

    /**
     * Reconstruye la entidad Recipe completa a partir de su identificador.
     * Realiza una carga en dos pasos: primero hidrata los atributos base y
     * posteriormente carga la colección de ingredientes mediante una consulta join.
     */
    @Override
    public Optional<Recipe> findById(int id) {
        try (Connection conn = dataSource.getConnection()) {
            Recipe recipe = null;
            try (PreparedStatement ps = conn.prepareStatement(SELECT_BY_ID)) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        recipe = RecipeMapper.mapRow(rs);
                    }
                }
            }

            if (recipe != null) {
                try (PreparedStatement ps = conn.prepareStatement(SELECT_INGREDIENTS)) {
                    ps.setInt(1, id);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            recipe.addIngredient(RecipeMapper.mapIngredientRow(rs));
                        }
                    }
                }
            }
            return Optional.ofNullable(recipe);
        } catch (SQLException e) {
            throw new RuntimeException("Error de infraestructura al consultar la receta.", e);
        }
    }

    /**
     * Obtiene el catálogo de todas las recetas ordenadas por fecha de creación.
     */
    @Override
    public List<Recipe> findAll() {
        List<Recipe> recipes = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_ALL);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                recipes.add(RecipeMapper.mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error de infraestructura al recuperar el catálogo.", e);
        }
        return recipes;
    }

    /**
     * Ejecuta la eliminación física de la receta y sus componentes asociados.
     */
    @Override
    public void delete(int id) {
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement ps = conn.prepareStatement(DELETE_INGREDIENTS)) {
                    ps.setInt(1, id);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = conn.prepareStatement(DELETE_RECIPE)) {
                    ps.setInt(1, id);
                    ps.executeUpdate();
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error de infraestructura al eliminar la receta.", e);
        }
    }

    /**
     * Gestiona la inserción masiva de ingredientes para una receta.
     */
    private void saveIngredients(Connection conn, Recipe recipe) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(INSERT_INGREDIENT)) {
            for (RecipeIngredient ing : recipe.getIngredients()) {
                ps.setInt(1, recipe.getId());
                ps.setInt(2, ing.getIngredientId());
                ps.setInt(3, ing.getUnitId());
                ps.setBigDecimal(4, ing.getQuantity());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }
}