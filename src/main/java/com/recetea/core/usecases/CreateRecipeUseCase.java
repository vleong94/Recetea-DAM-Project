package com.recetea.core.usecases;

import com.recetea.core.domain.Recipe;
import com.recetea.core.domain.RecipeIngredient;
import com.recetea.core.ports.in.CreateRecipeCommand;
import com.recetea.core.ports.in.ICreateRecipeUseCase;
import com.recetea.core.ports.IRecipeRepository;

/**
 * Application Layer: Caso de Uso para la creación de recetas.
 * Coordina la transformación de datos (Command -> Domain) y delega la persistencia.
 */
public class CreateRecipeUseCase implements ICreateRecipeUseCase {

    private final IRecipeRepository repository;

    // Dependency Injection: El caso de uso exige un repositorio, pero no le importa su implementación técnica.
    public CreateRecipeUseCase(IRecipeRepository repository) {
        this.repository = repository;
    }

    @Override
    public int execute(CreateRecipeCommand command) {

        // 1. Data Mapping: Command -> Aggregate Root
        Recipe recipe = new Recipe(
                command.userId(),
                command.categoryId(),
                command.difficultyId(),
                command.title(),
                command.description(),
                command.preparationTimeMinutes(),
                command.servings()
        );

        // 2. Data Mapping: Injectando los Value Objects (Ingredientes)
        if (command.ingredients() != null) {
            for (CreateRecipeCommand.IngredientCommand ic : command.ingredients()) {
                recipe.addIngredient(new RecipeIngredient(
                        ic.ingredientId(),
                        ic.unitId(),
                        ic.quantity()
                ));
            }
        }

        // 3. Delegación: Enviamos el objeto de Dominio puro al Outbound Port
        repository.save(recipe);

        // 4. Verificación de Estado
        if (recipe.getId() == null) {
            throw new IllegalStateException("Fallo de integridad: El repositorio no devolvió un ID válido tras el guardado.");
        }

        return recipe.getId();
    }
}