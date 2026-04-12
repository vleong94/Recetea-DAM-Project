package com.recetea.infrastructure.persistence.jbdc;

import com.recetea.core.domain.Ingredient;
import com.recetea.core.ports.out.IIngredientRepository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Infrastructure Layer: Adaptador JDBC para la entidad Ingredient.
 * Implementa el puerto de salida IIngredientRepository para PostgreSQL.
 * Sigue el patrón de inyección de dependencias por constructor para desacoplar la configuración.
 */
public class JdbcIngredientRepository implements IIngredientRepository {

    private final String url;
    private final String user;
    private final String password;

    /**
     * Constructor para inyección de dependencias.
     * Recibe las credenciales directamente del Composition Root (Main).
     */
    public JdbcIngredientRepository(String url, String user, String password) {
        this.url = url;
        this.user = user;
        this.password = password;
    }

    /**
     * Centraliza la creación de la conexión para facilitar futuros cambios (como un Pool de conexiones).
     */
    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    @Override
    public List<Ingredient> findAll() {
        List<Ingredient> ingredients = new ArrayList<>();
        String sql = "SELECT id_ingredient, ing_category_id, name FROM ingredients ORDER BY name ASC";

        // Uso de try-with-resources para garantizar el cierre de recursos críticos
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                ingredients.add(new Ingredient(
                        rs.getInt("id_ingredient"),
                        rs.getInt("ing_category_id"),
                        rs.getString("name")
                ));
            }
        } catch (SQLException e) {
            // Propagamos como RuntimeException para mantener limpia la interfaz del puerto
            throw new RuntimeException("Error de infraestructura: No se pudo recuperar el catálogo de ingredientes", e);
        }
        return ingredients;
    }
}