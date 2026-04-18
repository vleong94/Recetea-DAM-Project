package com.recetea.core.recipe.application.usecases.recipe;

import com.recetea.core.recipe.application.ports.in.dto.SaveRecipeRequest;
import com.recetea.core.recipe.application.ports.in.recipe.ICreateRecipeUseCase;
import com.recetea.core.recipe.application.ports.out.category.ICategoryRepository;
import com.recetea.core.recipe.application.ports.out.difficulty.IDifficultyRepository;
import com.recetea.core.recipe.application.ports.out.recipe.IRecipeRepository;
import com.recetea.core.recipe.domain.Category;
import com.recetea.core.recipe.domain.Difficulty;
import com.recetea.core.recipe.domain.Recipe;
import com.recetea.core.recipe.domain.RecipeIngredient;
import com.recetea.core.recipe.domain.RecipeStep;
import com.recetea.core.recipe.domain.vo.IngredientId;
import com.recetea.core.recipe.domain.vo.PreparationTime;
import com.recetea.core.recipe.domain.vo.Servings;
import com.recetea.core.recipe.domain.vo.UnitId;
import com.recetea.core.recipe.domain.vo.UserId;
import com.recetea.core.shared.application.ports.out.ITransactionManager;

public class CreateRecipeUseCase implements ICreateRecipeUseCase {

    private final IRecipeRepository recipeRepository;
    private final ICategoryRepository categoryRepository;
    private final IDifficultyRepository difficultyRepository;
    private final ITransactionManager transactionManager;

    public CreateRecipeUseCase(IRecipeRepository recipeRepository,
                               ICategoryRepository categoryRepository,
                               IDifficultyRepository difficultyRepository,
                               ITransactionManager transactionManager) {
        this.recipeRepository = recipeRepository;
        this.categoryRepository = categoryRepository;
        this.difficultyRepository = difficultyRepository;
        this.transactionManager = transactionManager;
    }

    @Override
    public int execute(SaveRecipeRequest request) {
        return transactionManager.execute(() -> {
            Category category = categoryRepository.findById(request.categoryId())
                    .orElseThrow(() -> new IllegalArgumentException("Categoría inválida."));
            Difficulty difficulty = difficultyRepository.findById(request.difficultyId())
                    .orElseThrow(() -> new IllegalArgumentException("Dificultad inválida."));

            Recipe recipe = new Recipe(
                    new UserId(request.userId()),
                    category,
                    difficulty,
                    request.title(),
                    request.description(),
                    new PreparationTime(request.preparationTimeMinutes()),
                    new Servings(request.servings())
            );

            recipe.syncIngredients(request.ingredients().stream()
                    .map(ir -> new RecipeIngredient(
                            new IngredientId(ir.ingredientId()),
                            new UnitId(ir.unitId()),
                            ir.quantity(),
                            ir.ingredientName(),
                            ir.unitName()))
                    .toList());

            recipe.syncSteps(request.steps().stream()
                    .map(sr -> new RecipeStep(sr.stepOrder(), sr.instruction()))
                    .toList());

            recipeRepository.save(recipe);
            return recipe.getId().value();
        });
    }
}
