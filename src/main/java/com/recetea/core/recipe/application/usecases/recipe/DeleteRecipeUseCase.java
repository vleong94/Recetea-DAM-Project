package com.recetea.core.recipe.application.usecases.recipe;

import com.recetea.core.recipe.application.ports.in.recipe.IDeleteRecipeUseCase;
import com.recetea.core.recipe.application.ports.out.recipe.IRecipeRepository;

/**
 * Implementa la lógica de aplicación para la eliminación de una receta del sistema.
 * Funciona como un comando de escritura puro que orquesta la operación destructiva,
 * garantizando que la petición originada en la interfaz se ejecute en la capa de
 * persistencia de forma controlada y aislada de las reglas de visualización.
 */
public class DeleteRecipeUseCase implements IDeleteRecipeUseCase {

    private final IRecipeRepository repository;

    /**
     * Inicializa el caso de uso mediante inyección de dependencias.
     * Al depender exclusivamente del contrato del puerto de salida, se mantiene
     * el agnosticismo tecnológico. La lógica de negocio no conoce los detalles
     * de cómo se eliminan los registros en la base de datos.
     *
     * @param repository Contrato de salida para el acceso a la persistencia de recetas.
     */
    public DeleteRecipeUseCase(IRecipeRepository repository) {
        this.repository = repository;
    }

    /**
     * Ejecuta la instrucción de borrado delegando la operación al repositorio subyacente.
     * La responsabilidad de mantener la integridad referencial, como el borrado en
     * cascada de los ingredientes (RecipeIngredient) asociados a la receta, recae
     * íntegramente sobre la implementación de la infraestructura.
     *
     * @param recipeId Identificador único numérico de la receta a eliminar.
     */
    @Override
    public void execute(int recipeId) {
        repository.delete(recipeId);
    }
}