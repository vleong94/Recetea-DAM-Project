package com.recetea.core.ports.in;

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