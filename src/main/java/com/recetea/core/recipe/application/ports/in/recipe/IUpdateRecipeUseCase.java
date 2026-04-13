package com.recetea.core.recipe.application.ports.in.recipe;

import com.recetea.core.recipe.application.ports.in.dto.SaveRecipeRequest;

/**
 * Define el contrato de entrada (Inbound Port) para el caso de uso responsable
 * de la actualización de recetas existentes en el sistema.
 * Situada en la capa Application, esta interfaz establece el límite arquitectónico
 * para las operaciones de modificación. Garantiza que los clientes externos
 * interactúen con el Domain exclusivamente a través de identificadores primitivos
 * y objetos de transferencia de datos (DTOs), manteniendo las reglas de negocio
 * aisladas de la infraestructura y la capa de presentación.
 */
public interface IUpdateRecipeUseCase {

    /**
     * Ejecuta la operación de actualización integral (reemplazo de estado)
     * para una receta específica y su composición de ingredientes asociada.
     *
     * @param recipeId El identificador numérico único de la receta objetivo.
     * @param request Objeto inmutable que encapsula el nuevo estado estructurado
     * y validado que se aplicará a la entidad.
     */
    void execute(int recipeId, SaveRecipeRequest request);
}