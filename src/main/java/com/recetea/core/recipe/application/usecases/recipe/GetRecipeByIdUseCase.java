package com.recetea.core.recipe.application.usecases.recipe;

import com.recetea.core.recipe.application.ports.in.dto.RecipeDetailResponse;
import com.recetea.core.recipe.application.ports.in.dto.RecipeIngredientResponse;
import com.recetea.core.recipe.application.ports.in.recipe.IGetRecipeByIdUseCase;
import com.recetea.core.recipe.application.ports.out.recipe.IRecipeRepository;
import com.recetea.core.recipe.domain.Recipe;
import com.recetea.core.recipe.domain.RecipeIngredient;
import com.recetea.core.recipe.domain.RecipeStep;
import com.recetea.core.recipe.domain.vo.RecipeId;

import java.util.Optional;

public class GetRecipeByIdUseCase implements IGetRecipeByIdUseCase {

    private final IRecipeRepository repository;

    public GetRecipeByIdUseCase(IRecipeRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<RecipeDetailResponse> execute(RecipeId recipeId) {
        return repository.findById(recipeId)
                .map(this::mapToResponse);
    }

    private RecipeDetailResponse mapToResponse(Recipe recipe) {
        return new RecipeDetailResponse(
                recipe.getId(),
                recipe.getAuthorId(),
                recipe.getCategory().getId(),
                recipe.getCategory().getName(),
                recipe.getDifficulty().getId(),
                recipe.getDifficulty().getName(),
                recipe.getTitle(),
                recipe.getDescription(),
                recipe.getPreparationTimeMinutes().value(),
                recipe.getServings().value(),
                recipe.getIngredients().stream().map(this::mapToIngredientResponse).toList(),
                recipe.getSteps().stream().map(this::mapToStepResponse).toList(),
                recipe.getAverageScore(),
                recipe.getTotalRatings()
        );
    }

    private RecipeIngredientResponse mapToIngredientResponse(RecipeIngredient ri) {
        return new RecipeIngredientResponse(
                ri.getIngredientId(),
                ri.getUnitId(),
                ri.getQuantity(),
                ri.getIngredientName(),
                ri.getUnitAbbreviation()
        );
    }

    private RecipeDetailResponse.RecipeStepResponse mapToStepResponse(RecipeStep rs) {
        return new RecipeDetailResponse.RecipeStepResponse(rs.stepOrder(), rs.instruction());
    }
}
