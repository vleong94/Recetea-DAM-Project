package com.recetea.infrastructure.persistence.recipe.jdbc.mappers;

import com.recetea.core.recipe.domain.Unit;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Componente de infraestructura especializado en la transformación de registros
 * de la base de datos hacia la entidad de dominio Unit.
 * Centraliza la lógica de mapeo para asegurar la consistencia de los datos
 * en todas las consultas que involucren unidades de medida.
 */
public class UnitMapper {

    private UnitMapper() {
        // Clase utilitaria con métodos estáticos; no requiere instanciación
    }

    /**
     * Transforma la fila actual de un ResultSet en un objeto Unit hidratado.
     * Este método mapea las columnas del esquema físico a las propiedades
     * inmutables del objeto de dominio, abstrayendo la estructura de la tabla
     * de la lógica de negocio.
     *
     * @param rs ResultSet posicionado en el registro a convertir.
     * @return Instancia de la entidad Unit con sus atributos asignados.
     * @throws SQLException Si ocurre un error al acceder a las columnas
     * o si los nombres de los campos no coinciden con el esquema esperado.
     */
    public static Unit mapRow(ResultSet rs) throws SQLException {
        return new Unit(
                rs.getInt("id_unit"),
                rs.getString("name"),
                rs.getString("abbreviation")
        );
    }
}