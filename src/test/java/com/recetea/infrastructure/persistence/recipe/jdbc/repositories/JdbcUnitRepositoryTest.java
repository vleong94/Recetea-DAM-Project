package com.recetea.infrastructure.persistence.recipe.jdbc.repositories;

import com.recetea.core.recipe.domain.Unit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration Test para el Outbound Adapter JdbcUnitRepository.
 * Valida la extracción de datos y el Data Mapping del catálogo de unidades,
 * garantizando el aislamiento transaccional mediante la herencia de BaseRepositoryTest.
 */
class JdbcUnitRepositoryTest extends BaseRepositoryTest {

    private JdbcUnitRepository repository;

    @BeforeEach
    void setUp() {
        // Inicializa el Outbound Adapter inyectando el DataSource del Connection Pool
        repository = new JdbcUnitRepository(dataSource);
        seedDatabase();
    }

    /**
     * Ejecuta sentencias SQL nativas para sembrar la tabla unit_measures.
     * Aisla el estado del test evitando el acoplamiento con otros componentes de persistencia.
     */
    private void seedDatabase() {
        String insertSql = "INSERT INTO unit_measures (name, abbreviation) VALUES (?, ?)";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(insertSql)) {

            // Inserción diseñada para validar la eficacia de la cláusula ORDER BY name ASC
            pstmt.setString(1, "Mililitros");
            pstmt.setString(2, "ml");
            pstmt.addBatch();

            pstmt.setString(1, "Gramos");
            pstmt.setString(2, "g");
            pstmt.addBatch();

            pstmt.executeBatch();

        } catch (SQLException e) {
            throw new RuntimeException("Fallo crítico de I/O al sembrar los datos de prueba en unit_measures.", e);
        }
    }

    @Test
    void findAll_RecuperaYConstruyeCatalogoCorrectamente() {
        // Ejecución del Query
        List<Unit> units = repository.findAll();

        // Verificaciones de integridad de la colección devuelta
        assertNotNull(units, "El ResultSet procesado no debe retornar una colección nula.");
        assertEquals(2, units.size(), "El Data Mapper no procesó la cantidad exacta de registros esperados.");

        // Verificación del Data Mapping y ordenamiento SQL (Gramos debe preceder a Mililitros)
        Unit primeraUnidad = units.get(0);
        Unit segundaUnidad = units.get(1);

        // Validación de atributos y Primary Key para el primer registro
        assertEquals("Gramos", primeraUnidad.getName(), "Fallo en el ordenamiento del Query o mapeo del atributo name.");
        assertEquals("g", primeraUnidad.getAbbreviation(), "Fallo en el mapeo del atributo abbreviation.");
        assertTrue(primeraUnidad.getId() > 0, "Fallo estructural: Omitida la inyección de la Primary Key en la Entity.");

        // Validación de atributos para el segundo registro
        assertEquals("Mililitros", segundaUnidad.getName(), "Fallo en el ordenamiento del Query o mapeo del atributo name.");
        assertEquals("ml", segundaUnidad.getAbbreviation(), "Fallo en el mapeo del atributo abbreviation.");
    }
}