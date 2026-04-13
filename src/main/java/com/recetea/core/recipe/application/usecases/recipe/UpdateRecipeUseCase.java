package com.recetea.core.recipe.application.usecases.recipe;

import com.recetea.core.recipe.application.ports.in.dto.SaveRecipeRequest;
import com.recetea.core.recipe.application.ports.in.recipe.IUpdateRecipeUseCase;
import com.recetea.core.recipe.application.ports.out.recipe.IRecipeRepository;
import com.recetea.core.recipe.domain.Recipe;
import com.recetea.core.recipe.domain.RecipeIngredient;

import java.util.List;

/**
 * Implementa la lógica de aplicación para la mutación de estado de recetas existentes.
 * Aplica el patrón Fetch-Mutate-Save para preservar la integridad de los metadatos
 * de infraestructura y garantizar que el Domain valide las transiciones de estado.
 */
public class UpdateRecipeUseCase implements IUpdateRecipeUseCase {

    private final IRecipeRepository repository;

    /**
     * Inicializa el Use Case mediante Dependency Injection.
     * El acoplamiento se realiza exclusivamente contra el Outbound Port,
     * garantizando el estricto cumplimiento del Dependency Inversion Principle.
     *
     * @param repository Contrato de salida para la persistencia del Aggregate Root.
     */
    public UpdateRecipeUseCase(IRecipeRepository repository) {
        this.repository = repository;
    }

    /**
     * Ejecuta la actualización integral del estado de la entidad.
     * Recupera el estado actual desde la infraestructura, aplica las mutaciones
     * validadas por el Aggregate Root y delega la reconciliación transaccional.
     *
     * @param recipeId Identificador único del registro a modificar.
     * @param request Payload inmutable con el nuevo estado validado.
     */
    @Override
    public void execute(int recipeId, SaveRecipeRequest request) {

        // 1. Fetch: Recuperación del estado actual
        // Extrae la entidad existente para no sobrescribir metadatos técnicos de infraestructura (ej. created_at).
        Recipe recipe = repository.findById(recipeId)
                .orElseThrow(() -> new IllegalArgumentException("Target Entity no encontrada en el Data Store."));

        // 2. Mutate: Aplicación de los cambios sobre el Aggregate Root
        // La entidad Recipe ejecuta sus propios invariantes (Invariants) de negocio en cada mutador.
        recipe.setUserId(request.userId());
        recipe.setCategoryId(request.categoryId());
        recipe.setDifficultyId(request.difficultyId());
        recipe.setTitle(request.title());
        recipe.setDescription(request.description());
        recipe.setPreparationTimeMinutes(request.preparationTimeMinutes());
        recipe.setServings(request.servings());

        // Reconciliación de la colección de Value Objects (Ingredientes)
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

            // Sustitución atómica de la colección interna.
            recipe.setIngredients(updatedIngredients);
        }

        // 3. Save: Delegación de persistencia a la infraestructura
        // El Repository asume el Transaction Scope para reconciliar el estado atómico en el Data Store.
        repository.update(recipe);
    }
}