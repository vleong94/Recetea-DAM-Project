package com.recetea.core.recipe.application.usecases.recipe;

import com.recetea.core.recipe.application.ports.in.dto.SaveRecipeRequest;
import com.recetea.core.recipe.application.ports.in.recipe.ICreateRecipeUseCase;
import com.recetea.core.recipe.application.ports.out.recipe.IRecipeRepository;
import com.recetea.core.recipe.domain.Recipe;
import com.recetea.core.recipe.domain.RecipeIngredient;

/**
 * Implementación del Use Case responsable de la orquestación para la creación de recetas.
 * Transforma los datos de entrada inmutables (Inbound DTOs) en entidades de dominio,
 * garantizando que el Aggregate Root se construya respetando todas las reglas de
 * integridad antes de su almacenamiento definitivo en la capa de Infrastructure.
 */
public class CreateRecipeUseCase implements ICreateRecipeUseCase {

    private final IRecipeRepository repository;

    /**
     * Inicializa el componente mediante Dependency Injection.
     * La dependencia exclusiva de la interfaz del repositorio permite mantener
     * el aislamiento del núcleo del sistema frente a cambios en la persistencia.
     */
    public CreateRecipeUseCase(IRecipeRepository repository) {
        this.repository = repository;
    }

    /**
     * Ejecuta el proceso de negocio para registrar una receta.
     * Realiza la conversión de tipos primitivos a Value Objects de dominio,
     * ensambla los componentes internos del agregado y delega la responsabilidad
     * de persistencia al adaptador de salida.
     *
     * @param request Estructura de datos con la información de la receta.
     * @return Primary Key asignado por el motor de base de datos tras la transacción.
     */
    @Override
    public int execute(SaveRecipeRequest request) {
        // 1. Construcción del objeto de dominio con identidad de autor encapsulada
        Recipe recipe = new Recipe(
                new Recipe.AuthorId(request.userId()),
                request.categoryId(),
                request.difficultyId(),
                request.title(),
                request.description(),
                request.preparationTimeMinutes(),
                request.servings()
        );

        // 2. Mapeo y agregación de la colección de ingredientes
        if (request.ingredients() != null) {
            for (SaveRecipeRequest.IngredientRequest ir : request.ingredients()) {
                recipe.addIngredient(new RecipeIngredient(
                        ir.ingredientId(),
                        ir.unitId(),
                        ir.quantity(),
                        ir.ingredientName(),
                        ir.unitName()
                ));
            }
        }

        // 3. Persistencia atómica de la entidad a través del repositorio
        repository.save(recipe);

        // 4. Verificación de integridad post-operación
        if (recipe.getId() == null || recipe.getId() <= 0) {
            throw new IllegalStateException("Fallo de consistencia: El Data Store no asignó una identidad válida.");
        }

        return recipe.getId();
    }
}