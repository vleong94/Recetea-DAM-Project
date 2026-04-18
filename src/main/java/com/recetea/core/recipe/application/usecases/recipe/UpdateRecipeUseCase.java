package com.recetea.core.recipe.application.usecases.recipe;

import com.recetea.core.recipe.application.ports.in.dto.SaveRecipeRequest;
import com.recetea.core.recipe.application.ports.in.recipe.IUpdateRecipeUseCase;
import com.recetea.core.recipe.application.ports.out.category.ICategoryRepository;
import com.recetea.core.recipe.application.ports.out.difficulty.IDifficultyRepository;
import com.recetea.core.recipe.application.ports.out.recipe.IRecipeRepository;
import com.recetea.core.recipe.domain.Category;
import com.recetea.core.recipe.domain.Difficulty;
import com.recetea.core.recipe.domain.Recipe;
import com.recetea.core.recipe.domain.RecipeIngredient;
import com.recetea.core.recipe.domain.RecipeStep;
import com.recetea.core.recipe.domain.UnauthorizedRecipeAccessException;
import com.recetea.core.recipe.domain.vo.PreparationTime;
import com.recetea.core.recipe.domain.vo.RecipeId;
import com.recetea.core.recipe.domain.vo.Servings;
import com.recetea.core.shared.application.ports.in.IUserSessionService;
import com.recetea.core.shared.application.ports.out.ITransactionManager;
import com.recetea.core.user.domain.UserId;

public class UpdateRecipeUseCase implements IUpdateRecipeUseCase {

    private final IRecipeRepository recipeRepository;
    private final ICategoryRepository categoryRepository;
    private final IDifficultyRepository difficultyRepository;
    private final ITransactionManager transactionManager;
    private final IUserSessionService sessionService;

    public UpdateRecipeUseCase(IRecipeRepository recipeRepository,
                               ICategoryRepository categoryRepository,
                               IDifficultyRepository difficultyRepository,
                               ITransactionManager transactionManager,
                               IUserSessionService sessionService) {
        this.recipeRepository = recipeRepository;
        this.categoryRepository = categoryRepository;
        this.difficultyRepository = difficultyRepository;
        this.transactionManager = transactionManager;
        this.sessionService = sessionService;
    }

    @Override
    public void execute(RecipeId recipeId, SaveRecipeRequest request) {
        transactionManager.execute(() -> {
            Recipe recipe = recipeRepository.findById(recipeId)
                    .orElseThrow(() -> new IllegalArgumentException("Receta no encontrada con ID: " + recipeId.value()));

            UserId currentUser = sessionService.getCurrentUserId();
            if (!recipe.getAuthorId().equals(currentUser)) {
                throw new UnauthorizedRecipeAccessException(
                        "El usuario " + currentUser.value() + " no tiene permiso para modificar esta receta.");
            }

            Category category = categoryRepository.findById(request.categoryId())
                    .orElseThrow(() -> new IllegalArgumentException("Categoría inválida."));
            Difficulty difficulty = difficultyRepository.findById(request.difficultyId())
                    .orElseThrow(() -> new IllegalArgumentException("Dificultad inválida."));

            recipe.setTitle(request.title());
            recipe.setDescription(request.description());
            recipe.setPreparationTimeMinutes(new PreparationTime(request.preparationTimeMinutes()));
            recipe.setServings(new Servings(request.servings()));
            recipe.setCategory(category);
            recipe.setDifficulty(difficulty);

            recipe.syncIngredients(request.ingredients().stream()
                    .map(ir -> new RecipeIngredient(
                            ir.ingredientId(),
                            ir.unitId(),
                            ir.quantity(),
                            ir.ingredientName(),
                            ir.unitName()))
                    .toList());

            recipe.syncSteps(request.steps().stream()
                    .map(sr -> new RecipeStep(sr.stepOrder(), sr.instruction()))
                    .toList());

            recipeRepository.update(recipe);
        });
    }
}
