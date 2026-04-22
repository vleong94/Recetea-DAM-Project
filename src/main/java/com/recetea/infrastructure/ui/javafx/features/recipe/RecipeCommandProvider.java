package com.recetea.infrastructure.ui.javafx.features.recipe;

import com.recetea.core.recipe.application.ports.in.category.IGetAllCategoriesUseCase;
import com.recetea.core.recipe.application.ports.in.difficulty.IGetAllDifficultiesUseCase;
import com.recetea.core.recipe.application.ports.in.ingredient.IGetAllIngredientsUseCase;
import com.recetea.core.recipe.application.ports.in.interop.IExportRecipeUseCase;
import com.recetea.core.recipe.application.ports.in.interop.IImportRecipeUseCase;
import com.recetea.core.recipe.application.ports.in.media.IAttachMediaUseCase;
import com.recetea.core.recipe.application.ports.in.report.IGenerateGlobalInventoryReportUseCase;
import com.recetea.core.recipe.application.ports.in.report.IGenerateRecipeTechnicalSheetUseCase;
import com.recetea.core.recipe.application.ports.in.recipe.IAddRatingUseCase;
import com.recetea.core.recipe.application.ports.in.recipe.ICreateRecipeUseCase;
import com.recetea.core.recipe.application.ports.in.recipe.IDeleteRecipeUseCase;
import com.recetea.core.recipe.application.ports.in.recipe.IUpdateRecipeUseCase;
import com.recetea.core.recipe.application.ports.in.unit.IGetAllUnitsUseCase;
import com.recetea.core.shared.application.ports.in.IUserSessionService;
import com.recetea.core.social.application.ports.in.IIsFavoriteUseCase;
import com.recetea.core.social.application.ports.in.IToggleFavoriteUseCase;

public interface RecipeCommandProvider {
    IAddRatingUseCase addRating();
    ICreateRecipeUseCase createRecipe();
    IUpdateRecipeUseCase updateRecipe();
    IDeleteRecipeUseCase deleteRecipe();
    IAttachMediaUseCase attachMedia();
    IGetAllCategoriesUseCase getAllCategories();
    IGetAllDifficultiesUseCase getAllDifficulties();
    IGetAllIngredientsUseCase getAllIngredients();
    IGetAllUnitsUseCase getAllUnits();
    IUserSessionService sessionService();
    IToggleFavoriteUseCase toggleFavorite();
    IIsFavoriteUseCase isFavorite();
    IImportRecipeUseCase importRecipe();
    IExportRecipeUseCase exportRecipe();
    IGenerateRecipeTechnicalSheetUseCase generateTechnicalSheet();
    IGenerateGlobalInventoryReportUseCase generateGlobalInventory();
}
