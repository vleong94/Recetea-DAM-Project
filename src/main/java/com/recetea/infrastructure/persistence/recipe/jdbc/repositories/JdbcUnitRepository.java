package com.recetea.infrastructure.persistence.recipe.jdbc.repositories;

import com.recetea.core.recipe.domain.Unit;
import com.recetea.core.recipe.application.ports.out.unit.IUnitRepository;
import com.recetea.infrastructure.persistence.recipe.jdbc.mappers.UnitMapper;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Outbound Adapter (JDBC) para la Entity Unit.
 * Implementa el Port definido en el Domain, aislando la lógica de acceso a datos.
 */
public class JdbcUnitRepository implements IUnitRepository {

    private final DataSource dataSource;

    /**
     * Inyecta el DataSource para delegar la gestión física de las conexiones
     * al Connection Pool de la capa de infraestructura.
     */
    public JdbcUnitRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public List<Unit> findAll() {
        List<Unit> units = new ArrayList<>();
        String sql = "SELECT id_unit, name, abbreviation FROM unit_measures ORDER BY name ASC";

        // El bloque try-with-resources garantiza la liberación automática de los recursos JDBC.
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                // Delega la transformación del ResultSet a un Data Mapper especializado.
                units.add(UnitMapper.mapRow(rs));
            }
        } catch (SQLException e) {
            // Implementa el patrón Fail-Fast encapsulando fallos de I/O en RuntimeExceptions.
            throw new RuntimeException("Fallo de infraestructura al recuperar el catálogo de unidades de medida.", e);
        }
        return units;
    }
}