package com.recetea.infrastructure.persistence.jbdc;

import com.recetea.core.domain.Unit;
import com.recetea.core.ports.out.IUnitRepository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Infrastructure Layer: Adaptador JDBC para la entidad Unit (Medidas).
 * Implementa el puerto de salida IUnitRepository para PostgreSQL.
 * Recibe la configuración de conexión por constructor para garantizar el desacoplamiento.
 */
public class JdbcUnitRepository implements IUnitRepository {

    private final String url;
    private final String user;
    private final String password;

    /**
     * Constructor para inyección de dependencias.
     * Permite que el Composition Root (Main) distribuya las credenciales del archivo properties.
     */
    public JdbcUnitRepository(String url, String user, String password) {
        this.url = url;
        this.user = user;
        this.password = password;
    }

    /**
     * Centraliza la obtención de la conexión para mantener la consistencia en toda la capa de persistencia.
     */
    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    @Override
    public List<Unit> findAll() {
        List<Unit> units = new ArrayList<>();
        String sql = "SELECT id_unit, name, abbreviation FROM unit_measures ORDER BY name ASC";

        // Uso de try-with-resources para garantizar que los recursos de la DB se liberen correctamente
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                units.add(new Unit(
                        rs.getInt("id_unit"),
                        rs.getString("name"),
                        rs.getString("abbreviation")
                ));
            }
        } catch (SQLException e) {
            // Transformación de excepción técnica a excepción de tiempo de ejecución para simplificar las capas superiores
            throw new RuntimeException("Error de infraestructura: No se pudo recuperar el catálogo de unidades de medida", e);
        }
        return units;
    }
}