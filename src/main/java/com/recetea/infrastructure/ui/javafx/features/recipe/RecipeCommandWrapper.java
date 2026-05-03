package com.recetea.infrastructure.ui.javafx.features.recipe;

import com.recetea.core.recipe.application.ports.in.category.IGetAllCategoriesUseCase;
import com.recetea.core.recipe.application.ports.in.difficulty.IGetAllDifficultiesUseCase;
import com.recetea.core.recipe.application.ports.in.ingredient.IGetAllIngredientsUseCase;
import com.recetea.core.recipe.application.ports.in.interop.IExportRecipeUseCase;
import com.recetea.core.recipe.application.ports.in.interop.IImportRecipeUseCase;
import com.recetea.core.recipe.application.ports.in.media.IAttachMediaUseCase;
import com.recetea.core.recipe.application.ports.in.report.IGenerateRecipeTechnicalSheetUseCase;
import com.recetea.core.recipe.application.ports.in.recipe.IAddRatingUseCase;
import com.recetea.core.recipe.application.ports.in.recipe.ICreateRecipeUseCase;
import com.recetea.core.recipe.application.ports.in.recipe.IDeleteRecipeUseCase;
import com.recetea.core.recipe.application.ports.in.recipe.IUpdateRecipeUseCase;
import com.recetea.core.recipe.application.ports.in.unit.IGetAllUnitsUseCase;
import com.recetea.core.shared.application.ports.in.IUserSessionService;
import com.recetea.core.shared.domain.utils.ExecutionContext;
import com.recetea.core.social.application.ports.in.IIsFavoriteUseCase;
import com.recetea.core.social.application.ports.in.IToggleFavoriteUseCase;

/**
 * Each use case getter returns a SAM lambda that opens a CORRELATION_ID scope
 * around the underlying execute() call. When the JavaFX Task runs the use case
 * on a virtual thread, the lambda binds a fresh CID (or reuses an inherited one)
 * for the entire downstream call chain, including the JDBC transaction.
 */
public final class RecipeCommandWrapper implements RecipeCommandProvider {

    private final RecipeCommandContext context;

    public RecipeCommandWrapper(RecipeCommandContext context) {
        this.context = context;
    }

    /**
     * Resolves the active user ID at the moment the wrapper opens its scope, so the
     * subsequent {@link ExecutionContext} binding tags every downstream log line and
     * {@link com.recetea.infrastructure.persistence.recipe.jdbc.InfrastructureException}
     * with both a correlation id and the responsible user. Returns {@code null} for
     * pre-login flows (the session has not been populated yet).
     */
    private String currentUserId() {
        return context.sessionService().getCurrentUserId()
                .map(uid -> Integer.toString(uid.value()))
                .orElse(null);
    }

    @Override
    public IAddRatingUseCase addRating() {
        IAddRatingUseCase d = context.addRating();
        return req -> ExecutionContext.run(currentUserId(), () -> d.execute(req));
    }

    @Override
    public ICreateRecipeUseCase createRecipe() {
        ICreateRecipeUseCase d = context.createRecipe();
        return req -> ExecutionContext.call(currentUserId(), () -> d.execute(req));
    }

    @Override
    public IUpdateRecipeUseCase updateRecipe() {
        IUpdateRecipeUseCase d = context.updateRecipe();
        return (id, req) -> ExecutionContext.run(currentUserId(), () -> d.execute(id, req));
    }

    @Override
    public IDeleteRecipeUseCase deleteRecipe() {
        IDeleteRecipeUseCase d = context.deleteRecipe();
        return id -> ExecutionContext.run(currentUserId(), () -> d.execute(id));
    }

    @Override
    public IAttachMediaUseCase attachMedia() {
        IAttachMediaUseCase d = context.attachMedia();
        return (id, data, name) -> ExecutionContext.run(currentUserId(), () -> d.execute(id, data, name));
    }

    @Override
    public IGetAllCategoriesUseCase getAllCategories() {
        IGetAllCategoriesUseCase d = context.getAllCategories();
        return () -> ExecutionContext.call(currentUserId(), d::execute);
    }

    @Override
    public IGetAllDifficultiesUseCase getAllDifficulties() {
        IGetAllDifficultiesUseCase d = context.getAllDifficulties();
        return () -> ExecutionContext.call(currentUserId(), d::execute);
    }

    @Override
    public IGetAllIngredientsUseCase getAllIngredients() {
        IGetAllIngredientsUseCase d = context.getAllIngredients();
        return () -> ExecutionContext.call(currentUserId(), d::execute);
    }

    @Override
    public IGetAllUnitsUseCase getAllUnits() {
        IGetAllUnitsUseCase d = context.getAllUnits();
        return () -> ExecutionContext.call(currentUserId(), d::execute);
    }

    @Override
    public IUserSessionService sessionService() { return context.sessionService(); }

    @Override
    public IToggleFavoriteUseCase toggleFavorite() {
        IToggleFavoriteUseCase d = context.toggleFavorite();
        return id -> ExecutionContext.run(currentUserId(), () -> d.execute(id));
    }

    @Override
    public IIsFavoriteUseCase isFavorite() {
        IIsFavoriteUseCase d = context.isFavorite();
        return id -> ExecutionContext.call(currentUserId(), () -> d.execute(id));
    }

    @Override
    public IImportRecipeUseCase importRecipe() {
        IImportRecipeUseCase d = context.importRecipe();
        return src -> ExecutionContext.call(currentUserId(), () -> d.execute(src));
    }

    @Override
    public IExportRecipeUseCase exportRecipe() {
        IExportRecipeUseCase d = context.exportRecipe();
        return (id, dst) -> ExecutionContext.run(currentUserId(), () -> d.execute(id, dst));
    }

    @Override
    public IGenerateRecipeTechnicalSheetUseCase generateTechnicalSheet() {
        IGenerateRecipeTechnicalSheetUseCase d = context.generateTechnicalSheet();
        return id -> ExecutionContext.call(currentUserId(), () -> d.execute(id));
    }
}
