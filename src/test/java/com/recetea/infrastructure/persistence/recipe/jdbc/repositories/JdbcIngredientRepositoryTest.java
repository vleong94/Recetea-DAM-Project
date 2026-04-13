package com.recetea.infrastructure.persistence.recipe.jdbc.repositories;

import com.recetea.core.recipe.domain.Ingredient;
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
 * Test de integración para el Outbound Adapter JdbcIngredientRepository.
 * Hereda de BaseRepositoryTest para asegurar que la base de datos de test
 * sea truncada y reiniciada completamente antes de cada ejecución.
 */
class JdbcIngredientRepositoryTest extends BaseRepositoryTest {

    private JdbcIngredientRepository repository;

    @BeforeEach
    void setUp() {
        // Inicialización del repositorio mediante la inyección del DataSource de pruebas.
        repository = new JdbcIngredientRepository(dataSource);
        seedDatabase();
    }

    /**
     * Satisface las restricciones de integridad referencial inyectando datos en orden jerárquico.
     * Al utilizar JDBC nativo, se garantiza que la preparación del entorno sea independiente
     * de la lógica de negocio de otros adaptadores.
     */
    private void seedDatabase() {
        String insertCategorySql = "INSERT INTO ingredient_categories (id_ing_category, name) VALUES (?, ?)";
        String insertIngredientSql = "INSERT INTO ingredients (ing_category_id, name) VALUES (?, ?)";

        try (Connection conn = dataSource.getConnection()) {
            // Generación del registro padre (Master Data) para evitar violaciones de Foreign Key.
            try (PreparedStatement pstmt = conn.prepareStatement(insertCategorySql)) {
                pstmt.setInt(1, 1);
                pstmt.setString(2, "Verduras y Hortalizas");
                pstmt.executeUpdate();
            }

            // Inserción de los registros dependientes para validar las operaciones de lectura.
            try (PreparedStatement pstmt = conn.prepareStatement(insertIngredientSql)) {
                // Se definen en desorden alfabético para verificar la cláusula ORDER BY del repositorio.
                pstmt.setInt(1, 1);
                pstmt.setString(2, "Zanahoria");
                pstmt.addBatch();

                pstmt.setInt(1, 1);
                pstmt.setString(2, "Ajo");
                pstmt.addBatch();

                pstmt.executeBatch();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Fallo crítico de I/O al sembrar los datos de prueba en la jerarquía de ingredientes.", e);
        }
    }

    @Test
    void findAll_RecuperaYConstruyeCatalogoCorrectamente() {
        // Ejecución de la consulta sobre el adaptador.
        List<Ingredient> ingredients = repository.findAll();

        // Verificaciones de estructura y completitud.
        assertNotNull(ingredients, "El contrato exige retornar una colección vacía en lugar de nula ante fallos de búsqueda.");
        assertEquals(2, ingredients.size(), "La cantidad de objetos hidratados no coincide con los registros inyectados.");

        // Verificación de la estrategia de ordenamiento y Data Mapping.
        Ingredient primerIngrediente = ingredients.get(0);
        Ingredient segundoIngrediente = ingredients.get(1);

        // Valida la correcta ejecución de la cláusula ORDER BY y la integridad del mapeo textual.
        assertEquals("Ajo", primerIngrediente.getName(), "Fallo en el ordenamiento lexicográfico o en el mapeo del nombre.");
        assertEquals("Zanahoria", segundoIngrediente.getName(), "Fallo en el ordenamiento lexicográfico o en el mapeo del nombre.");

        // Validación de inmutabilidad y persistencia de claves.
        assertTrue(primerIngrediente.getId() > 0, "El Data Mapper ha fallado al inyectar el identificador principal generado.");
        assertEquals(1, primerIngrediente.getCategoryId(), "Discrepancia en la integridad referencial de la categoría mapeada.");
    }
}