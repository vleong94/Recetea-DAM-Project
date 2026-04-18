package com.recetea.infrastructure.persistence.recipe.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Interfaz funcional que define el contrato para la transformación de una fila de un ResultSet
 * hacia un objeto de tipo T. Permite desacoplar la lógica de extracción de datos de la
 * estructura de control de las consultas JDBC.
 *
 * @param <T> Tipo del objeto de destino.
 */
@FunctionalInterface
public interface RowMapper<T> {
    /**
     * Mapea la fila actual del ResultSet a una instancia de T.
     * @param rs ResultSet posicionado en la fila a procesar.
     * @return Instancia del objeto mapeado.
     * @throws SQLException Si ocurre un error de acceso a las columnas de la base de datos.
     */
    T map(ResultSet rs) throws SQLException;
}