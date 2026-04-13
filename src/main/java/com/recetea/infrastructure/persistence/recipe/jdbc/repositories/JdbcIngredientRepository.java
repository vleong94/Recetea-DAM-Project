package com.recetea.infrastructure.persistence.recipe.jdbc.repositories;

import com.recetea.core.recipe.domain.Ingredient;
import com.recetea.core.recipe.application.ports.out.ingredient.IIngredientRepository;
import com.recetea.infrastructure.persistence.recipe.jdbc.mappers.IngredientMapper;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Outbound Adapter (JDBC) para la entidad Ingredient.
 * Implementa el Port definido en el Domain ejecutando consultas SQL directas y
 * delegando la traducción de datos al Data Mapper.
 */
public class JdbcIngredientRepository implements IIngredientRepository {

    private final DataSource dataSource;

    /**
     * Inyecta el DataSource (HikariCP) para aislar el repositorio de la
     * gestión de credenciales y optimizar el uso del Connection Pool.
     */
    public JdbcIngredientRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public List<Ingredient> findAll() {
        List<Ingredient> ingredients = new ArrayList<>();
        String sql = "SELECT id_ingredient, ing_category_id, name FROM ingredients ORDER BY name ASC";

        // El bloque try-with-resources asegura el retorno atómico de la conexión al pool.
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                // Delega la instanciación inmutable para proteger el esquema interno del Domain.
                ingredients.add(IngredientMapper.mapRow(rs));
            }
        } catch (SQLException e) {
            // Patrón Fail-Fast: encapsula el error SQL en una excepción Runtime no recuperable.
            throw new RuntimeException("Fallo de I/O al consultar el catálogo de ingredientes.", e);
        }

        return ingredients;
    }
}