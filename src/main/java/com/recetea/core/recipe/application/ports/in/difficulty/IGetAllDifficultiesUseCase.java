package com.recetea.core.recipe.application.ports.in.difficulty;

import com.recetea.core.recipe.domain.Difficulty;
import java.util.List;

/**
 * Puerto de entrada (Inbound Port) que define el contrato formal para la obtención
 * del catálogo de niveles de dificultad técnica.
 * Expone la funcionalidad necesaria para que la capa de presentación pueda listar
 * las opciones de complejidad, asegurando que el Aggregate Root Recipe se construya
 * siempre sobre una base taxonómica válida y existente en el sistema.
 */
public interface IGetAllDifficultiesUseCase {

    /**
     * Ejecuta la lógica de aplicación para recuperar todas las dificultades configuradas.
     * El retorno consiste en entidades de dominio Difficulty, garantizando que los datos
     * mantengan su integridad y significado de negocio desde la persistencia hasta la vista.
     *
     * @return Colección de entidades Difficulty. Retorna una lista vacía si no existen registros.
     */
    List<Difficulty> execute();
}