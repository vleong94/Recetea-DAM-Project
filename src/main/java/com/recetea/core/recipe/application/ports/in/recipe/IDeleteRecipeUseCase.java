package com.recetea.core.recipe.application.ports.in.recipe;

/**
 * Define el contrato de entrada (Inbound Port) para el caso de uso encargado
 * de la eliminación de recetas en el sistema.
 * Como componente de la capa Application, esta interfaz actúa como una frontera
 * de delegación. Permite a los clientes externos (controladores de UI o APIs)
 * emitir comandos destructivos mediante identificadores primitivos, manteniendo
 * a la vista completamente aislada de las reglas de integridad referencial y de
 * los detalles del motor de base de datos.
 */
public interface IDeleteRecipeUseCase {

    /**
     * Ejecuta la instrucción de borrado para una receta específica.
     * La implementación subyacente es responsable de gestionar la transacción
     * y la eliminación en cascada de los recursos asociados (ej. ingredientes dosificados).
     *
     * @param recipeId El identificador numérico único de la receta objetivo.
     */
    void execute(int recipeId);
}