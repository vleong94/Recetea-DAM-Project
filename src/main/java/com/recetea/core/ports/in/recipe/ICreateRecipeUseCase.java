package com.recetea.core.ports.in.recipe;

import com.recetea.core.ports.in.dto.CreateRecipeCommand;

/**
 * Inbound Port: Define el contrato para el Caso de Uso de creación de recetas.
 * Cualquier framework visual (JavaFX, Web, CLI) consumirá esta interfaz.
 */
public interface ICreateRecipeUseCase {

    /**
     * Orquesta la creación de una receta y sus ingredientes.
     * @param command Datos crudos de entrada.
     * @return El ID autogenerado de la nueva receta en la base de datos.
     */
    int execute(CreateRecipeCommand command);

}