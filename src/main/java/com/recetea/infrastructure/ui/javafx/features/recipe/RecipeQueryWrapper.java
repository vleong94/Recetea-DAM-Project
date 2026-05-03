package com.recetea.infrastructure.ui.javafx.features.recipe;

import com.recetea.core.recipe.application.ports.in.recipe.IGetAllRecipesUseCase;
import com.recetea.core.recipe.application.ports.in.recipe.IGetRecipeByIdUseCase;
import com.recetea.core.recipe.application.ports.in.recipe.IGetRecipeSummariesByIdsUseCase;
import com.recetea.core.recipe.application.ports.in.recipe.IGetRecipesByAuthorUseCase;
import com.recetea.core.recipe.application.ports.in.recipe.ISearchRecipesUseCase;
import com.recetea.core.recipe.application.ports.in.recipe.ISuggestRecipeTitlesUseCase;
import com.recetea.core.shared.application.ports.in.IUserSessionService;
import com.recetea.core.shared.domain.utils.ExecutionContext;
import com.recetea.core.social.application.ports.in.IGetUserFavoritesUseCase;

/**
 * Read-side counterpart of {@link RecipeCommandWrapper}. Same CID-binding
 * pattern: each query returns a SAM lambda that wraps the underlying
 * execute() call so the entire fetch flow runs under a correlation scope.
 *
 * <p>Takes the {@link IUserSessionService} as a separate constructor argument
 * (it doesn't live on {@link RecipeQueryContext} since that DTO is read-only by
 * design) so every read flow can attribute its log lines to the responsible user
 * via the {@code userId} field of the bound {@code ExecutionMetadata}.
 */
public final class RecipeQueryWrapper implements RecipeQueryProvider {

    private final RecipeQueryContext   context;
    private final IUserSessionService  sessionService;

    public RecipeQueryWrapper(RecipeQueryContext context, IUserSessionService sessionService) {
        this.context        = context;
        this.sessionService = sessionService;
    }

    /** {@code null} when no session is active — anonymous read paths still get a CID, just no userId. */
    private String currentUserId() {
        return sessionService.getCurrentUserId()
                .map(uid -> Integer.toString(uid.value()))
                .orElse(null);
    }

    @Override
    public IGetAllRecipesUseCase getAllRecipes() {
        IGetAllRecipesUseCase d = context.getAllRecipes();
        return pr -> ExecutionContext.call(currentUserId(), () -> d.execute(pr));
    }

    @Override
    public IGetRecipeByIdUseCase getRecipeById() {
        IGetRecipeByIdUseCase d = context.getRecipeById();
        return id -> ExecutionContext.call(currentUserId(), () -> d.execute(id));
    }

    @Override
    public ISearchRecipesUseCase searchRecipes() {
        ISearchRecipesUseCase d = context.searchRecipes();
        return (c, p) -> ExecutionContext.call(currentUserId(), () -> d.execute(c, p));
    }

    @Override
    public IGetUserFavoritesUseCase getUserFavorites() {
        IGetUserFavoritesUseCase d = context.getUserFavorites();
        return () -> ExecutionContext.call(currentUserId(), d::execute);
    }

    @Override
    public IGetRecipesByAuthorUseCase getRecipesByAuthor() {
        IGetRecipesByAuthorUseCase d = context.getRecipesByAuthor();
        return (u, p) -> ExecutionContext.call(currentUserId(), () -> d.execute(u, p));
    }

    @Override
    public IGetRecipeSummariesByIdsUseCase getRecipeSummariesByIds() {
        IGetRecipeSummariesByIdsUseCase d = context.getRecipeSummariesByIds();
        return ids -> ExecutionContext.call(currentUserId(), () -> d.execute(ids));
    }

    @Override
    public ISuggestRecipeTitlesUseCase suggestRecipeTitles() {
        ISuggestRecipeTitlesUseCase d = context.suggestRecipeTitles();
        return (prefix, limit) -> ExecutionContext.call(currentUserId(), () -> d.execute(prefix, limit));
    }
}
