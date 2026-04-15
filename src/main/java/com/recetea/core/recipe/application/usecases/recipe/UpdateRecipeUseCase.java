package com.recetea.core.recipe.application.usecases.recipe;

import com.recetea.core.recipe.application.ports.in.dto.SaveRecipeRequest;
import com.recetea.core.recipe.application.ports.in.recipe.IUpdateRecipeUseCase;
import com.recetea.core.recipe.application.ports.out.recipe.IRecipeRepository;
import com.recetea.core.recipe.domain.Recipe;
import com.recetea.core.recipe.domain.RecipeIngredient;

import java.util.List;

/**
 * Orquestador de aplicación encargado de gestionar la actualización de recetas existentes.
 * Implementa el flujo de trabajo transaccional para modificar el estado de un agregado,
 * asegurando que la transición desde el estado persistente al nuevo estado deseado
 * se realice bajo las validaciones de integridad definidas en el dominio.
 */
public class UpdateRecipeUseCase implements IUpdateRecipeUseCase {

    private final IRecipeRepository repository;

    /**
     * Inicializa el caso de uso mediante inyección de dependencias.
     * Al depender de la interfaz del repositorio, el componente permanece desacoplado
     * de los detalles técnicos de la base de datos, cumpliendo con el principio
     * de inversión de dependencias.
     */
    public UpdateRecipeUseCase(IRecipeRepository repository) {
        this.repository = repository;
    }

    /**
     * Ejecuta la lógica de mutación de la receta identificada.
     * Recupera la entidad desde la infraestructura, transforma los tipos primitivos
     * del contrato de entrada en objetos con significado de negocio y sincroniza
     * la colección de ingredientes antes de delegar la persistencia definitiva.
     *
     * @param recipeId Identificador único de la receta objetivo.
     * @param request Datos de entrada con la información actualizada.
     */
    @Override
    public void execute(int recipeId, SaveRecipeRequest request) {
        // 1. Localización y carga del Aggregate Root desde la infraestructura
        Recipe recipe = repository.findById(recipeId)
                .orElseThrow(() -> new RuntimeException("Fallo de consistencia: No se encontró la entidad solicitada para actualizar."));

        // 2. Aplicación de mutaciones sobre los atributos de cabecera
        // Se realiza la conversión explícita del identificador de autor al Value Object AuthorId
        recipe.setAuthorId(new Recipe.AuthorId(request.userId()));
        recipe.setCategoryId(request.categoryId());
        recipe.setDifficultyId(request.difficultyId());
        recipe.setTitle(request.title());
        recipe.setDescription(request.description());
        recipe.setPreparationTimeMinutes(request.preparationTimeMinutes());
        recipe.setServings(request.servings());

        // 3. Sincronización de la composición de ingredientes
        // Se reconstruye la lista de Value Objects a partir de la petición de entrada
        if (request.ingredients() != null) {
            List<RecipeIngredient> updatedIngredients = request.ingredients().stream()
                    .map(ir -> new RecipeIngredient(
                            ir.ingredientId(),
                            ir.unitId(),
                            ir.quantity(),
                            ir.ingredientName(),
                            ir.unitName()
                    ))
                    .toList();

            // Actualización atómica de la colección interna del agregado
            recipe.setIngredients(updatedIngredients);
        }

        // 4. Persistencia de los cambios en el Data Store
        repository.update(recipe);
    }
}