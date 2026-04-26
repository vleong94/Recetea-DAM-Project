package com.recetea.core.recipe.application.usecases.recipe;

import com.recetea.core.recipe.application.ports.in.dto.SaveRecipeRequest;
import com.recetea.core.recipe.application.ports.in.recipe.IUpdateRecipeUseCase;
import com.recetea.core.recipe.application.ports.out.category.ICategoryRepository;
import com.recetea.core.recipe.application.ports.out.difficulty.IDifficultyRepository;
import com.recetea.core.recipe.application.ports.out.recipe.IRecipeRepository;
import com.recetea.core.recipe.domain.AuthenticationRequiredException;
import com.recetea.core.recipe.domain.Category;
import com.recetea.core.recipe.domain.Difficulty;
import com.recetea.core.recipe.domain.InvalidRecipeDataException;
import com.recetea.core.recipe.domain.Recipe;
import com.recetea.core.recipe.domain.RecipeIngredient;
import com.recetea.core.recipe.domain.RecipeNotFoundException;
import com.recetea.core.recipe.domain.RecipeStep;
import com.recetea.core.recipe.domain.UnauthorizedRecipeAccessException;
import com.recetea.core.recipe.domain.vo.PreparationTime;
import com.recetea.core.recipe.domain.vo.RecipeId;
import com.recetea.core.recipe.domain.vo.Servings;
import com.recetea.core.shared.application.ports.in.IUserSessionService;
import com.recetea.core.shared.application.ports.out.ITransactionManager;
import com.recetea.core.user.domain.UserId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UpdateRecipeUseCase implements IUpdateRecipeUseCase {

    private static final Logger log = LoggerFactory.getLogger(UpdateRecipeUseCase.class);

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
        var validation = request.validate();
        if (!validation.isValid()) {
            log.warn("Validation failed for recipe update (ID: {}): {}", recipeId.value(), validation.errors());
        }
        validation.getOrThrow(InvalidRecipeDataException::new);

        log.info("Updating recipe: {}", recipeId.value());

        transactionManager.execute(() -> {
            Recipe recipe = recipeRepository.findById(recipeId)
                    .orElseThrow(() -> new RecipeNotFoundException(recipeId.value()));

            UserId currentUser = sessionService.getCurrentUserId()
                    .orElseThrow(AuthenticationRequiredException::new);
            if (!recipe.getAuthorId().equals(currentUser)) {
                throw new UnauthorizedRecipeAccessException(
                        "User " + currentUser.value() + " is not authorized to modify recipe " + recipeId.value() + ".");
            }

            Category category = categoryRepository.findById(request.categoryId())
                    .orElseThrow(() -> new IllegalArgumentException("Invalid category ID: " + request.categoryId()));
            Difficulty difficulty = difficultyRepository.findById(request.difficultyId())
                    .orElseThrow(() -> new IllegalArgumentException("Invalid difficulty ID: " + request.difficultyId()));

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

        log.info("Recipe {} updated successfully.", recipeId.value());
    }
}
