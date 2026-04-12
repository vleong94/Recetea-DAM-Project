package com.recetea.core.usecases.recipe;

import com.recetea.core.domain.Recipe;
import com.recetea.core.domain.RecipeIngredient;
import com.recetea.core.ports.in.dto.CreateRecipeCommand;
import com.recetea.core.ports.in.recipe.ICreateRecipeUseCase;
import com.recetea.core.ports.out.IRecipeRepository;

/**
 * Application Layer: Caso de Uso para la creación de recetas.
 * Coordina la transformación de datos (Command -> Domain) y delega la persistencia.
 * Sincronizado para manejar nombres descriptivos y precisión decimal[cite: 21].
 */
public class CreateRecipeUseCase implements ICreateRecipeUseCase {

    private final IRecipeRepository repository;

    /**
     * Inyección del puerto de salida[cite: 1].
     */
    public CreateRecipeUseCase(IRecipeRepository repository) {
        this.repository = repository;
    }

    @Override
    public int execute(CreateRecipeCommand command) {

        // 1. Mapeo de Cabecera: Transformación de Command a Aggregate Root (Recipe)
        Recipe recipe = new Recipe(
                command.userId(),
                command.categoryId(),
                command.difficultyId(),
                command.title(),
                command.description(),
                command.preparationTimeMinutes(),
                command.servings()
        );

        // 2. Mapeo de Detalles: Transformación con los nuevos 5 parámetros (incluye nombres)
        if (command.ingredients() != null) {
            for (CreateRecipeCommand.IngredientCommand ic : command.ingredients()) {
                // Sincronización con el constructor de RecipeIngredient
                recipe.addIngredient(new RecipeIngredient(
                        ic.ingredientId(),
                        ic.unitId(),
                        ic.quantity(),
                        ic.ingredientName(), // <--- Nuevo parámetro para UX
                        ic.unitName()        // <--- Nuevo parámetro para UX
                ));
            }
        }

        // 3. Persistencia: Delegación al Adaptador de Infraestructura [cite: 1]
        repository.save(recipe);

        // 4. Validación de Identidad post-guardado
        if (recipe.getId() == null || recipe.getId() <= 0) {
            throw new IllegalStateException("Error de persistencia: El repositorio no retornó una identidad válida.");
        }

        return recipe.getId();
    }
}