package com.recetea.core.recipe.application.ports.in.recipe;

import com.recetea.core.recipe.application.ports.in.dto.SaveRecipeRequest;

/**
 * Define el contrato de entrada (Inbound Port) para el caso de uso responsable
 * de la creación de nuevas recetas en el sistema.
 * Al pertenecer a la capa Application, esta interfaz establece el límite
 * arquitectónico a través del cual los clientes externos (interfaces de usuario,
 * controladores o APIs) envían instrucciones de escritura hacia el Domain.
 * El uso exclusivo de objetos de transferencia de datos (DTOs) garantiza
 * que las reglas de negocio permanezcan aisladas y agnósticas a la tecnología de la vista.
 */
public interface ICreateRecipeUseCase {

    /**
     * Ejecuta la operación principal para instanciar y persistir una nueva
     * receta junto con su composición de ingredientes (Aggregate Root).
     *
     * @param request Objeto inmutable que encapsula todos los datos crudos
     * necesarios para la creación, validación y persistencia de la receta.
     * @return El identificador numérico único (ID) asignado por la infraestructura
     * de persistencia a la nueva entidad.
     */
    int execute(SaveRecipeRequest request);
}