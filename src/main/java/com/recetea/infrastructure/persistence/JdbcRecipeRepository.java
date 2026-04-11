package com.recetea.infrastructure.persistence;

import com.recetea.core.domain.Recipe;
import com.recetea.core.ports.IRecipeRepository;

import java.io.InputStream;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

/**
 * Persistence Adapter: Implementación JDBC para IRecipeRepository.
 * Gestiona la conexión a PostgreSQL y la ejecución de queries SQL.
 */
public class JdbcRecipeRepository implements IRecipeRepository {

    private static final String DB_URL;
    private static final String DB_USER;
    private static final String DB_PASSWORD;

    /*
     * Bloque de inicialización estática (Static Block).
     * Se ejecuta de forma automática una única vez cuando el ClassLoader carga
     * esta clase en la JVM. Se encarga de instanciar un objeto Properties,
     * abrir un InputStream hacia el archivo de configuración externa y poblar
     * las variables estáticas inmutables, aislando las credenciales del código fuente.
     */
    static {
        Properties props = new Properties();
        try (InputStream input = JdbcRecipeRepository.class.getClassLoader().getResourceAsStream("application-local.properties")) {
            if (input == null) {
                throw new RuntimeException("CRÍTICO: No se encontró el archivo application-local.properties en el Classpath.");
            }
            props.load(input);
            DB_URL = props.getProperty("db.url");
            DB_USER = props.getProperty("db.user");
            DB_PASSWORD = props.getProperty("db.password");
        } catch (Exception e) {
            throw new RuntimeException("Error en la inicialización de configuración de base de datos", e);
        }
    }

    /**
     * Establece una nueva Connection con el motor de base de datos
     * utilizando las credenciales inyectadas.
     */
    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }

    @Override
    public void save(Recipe recipe) {
        String query = "INSERT INTO recipes (user_id, category_id, difficulty_id, title, description, prep_time_min, servings) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";

        // Uso de try-with-resources para garantizar el cierre automático de Connection y PreparedStatement
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {

            // Asignación de parámetros posicionales para evitar SQL Injection
            pstmt.setInt(1, recipe.getUserId());
            pstmt.setInt(2, recipe.getCategoryId());
            pstmt.setInt(3, recipe.getDifficultyId());
            pstmt.setString(4, recipe.getTitle());
            pstmt.setString(5, recipe.getDescription());
            pstmt.setInt(6, recipe.getPreparationTimeMinutes());
            pstmt.setInt(7, recipe.getServings());

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("El guardado falló, la operación no afectó a ninguna fila.");
            }

            // Extracción del identificador asignado por el motor relacional (IDENTITY)
            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    recipe.setId(generatedKeys.getInt(1));
                } else {
                    throw new SQLException("El guardado falló, no se recuperó el ID autogenerado.");
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error de persistencia al ejecutar el INSERT de Recipe", e);
        }
    }

    // Stubs pendientes de implementación en futuras iteraciones arquitectónicas
    @Override
    public Optional<Recipe> findById(int id) { return Optional.empty(); }
    @Override
    public List<Recipe> findAll() { return new ArrayList<>(); }
    @Override
    public void delete(int id) {}
}