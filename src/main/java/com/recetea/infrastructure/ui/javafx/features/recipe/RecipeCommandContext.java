package com.recetea.infrastructure.ui.javafx.features.recipe;

import com.recetea.core.recipe.application.ports.in.category.IGetAllCategoriesUseCase;
import com.recetea.core.recipe.application.ports.in.difficulty.IGetAllDifficultiesUseCase;
import com.recetea.core.recipe.application.ports.in.ingredient.IGetAllIngredientsUseCase;
import com.recetea.core.recipe.application.ports.in.media.IAttachMediaUseCase;
import com.recetea.core.recipe.application.ports.in.recipe.IAddRatingUseCase;
import com.recetea.core.recipe.application.ports.in.recipe.ICreateRecipeUseCase;
import com.recetea.core.recipe.application.ports.in.recipe.IDeleteRecipeUseCase;
import com.recetea.core.recipe.application.ports.in.recipe.IUpdateRecipeUseCase;
import com.recetea.core.recipe.application.ports.in.unit.IGetAllUnitsUseCase;
import com.recetea.core.shared.application.ports.in.IUserSessionService;
import com.recetea.core.social.application.ports.in.IIsFavoriteUseCase;
import com.recetea.core.social.application.ports.in.IToggleFavoriteUseCase;

public record RecipeCommandContext(
        IAddRatingUseCase addRating,
        ICreateRecipeUseCase createRecipe,
        IUpdateRecipeUseCase updateRecipe,
        IDeleteRecipeUseCase deleteRecipe,
        IAttachMediaUseCase attachMedia,
        IGetAllIngredientsUseCase getAllIngredients,
        IGetAllUnitsUseCase getAllUnits,
        IGetAllCategoriesUseCase getAllCategories,
        IGetAllDifficultiesUseCase getAllDifficulties,
        IUserSessionService sessionService,
        IToggleFavoriteUseCase toggleFavorite,
        IIsFavoriteUseCase isFavorite
) implements RecipeCommandProvider {}
