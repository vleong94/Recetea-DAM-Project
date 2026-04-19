package com.recetea.core.social.application.usecases;

import com.recetea.core.recipe.application.ports.in.dto.RecipeSummaryResponse;
import com.recetea.core.recipe.application.ports.out.recipe.IRecipeRepository;
import com.recetea.core.recipe.domain.AuthenticationRequiredException;
import com.recetea.core.recipe.domain.vo.RecipeId;
import com.recetea.core.shared.application.ports.in.IUserSessionService;
import com.recetea.core.social.application.ports.in.IGetUserFavoritesUseCase;
import com.recetea.core.social.application.ports.out.IFavoriteRepository;
import com.recetea.core.user.domain.UserId;

import java.util.List;

public class GetUserFavoritesUseCase implements IGetUserFavoritesUseCase {

    private final IFavoriteRepository favoriteRepository;
    private final IRecipeRepository recipeRepository;
    private final IUserSessionService sessionService;

    public GetUserFavoritesUseCase(IFavoriteRepository favoriteRepository,
                                   IRecipeRepository recipeRepository,
                                   IUserSessionService sessionService) {
        this.favoriteRepository = favoriteRepository;
        this.recipeRepository = recipeRepository;
        this.sessionService = sessionService;
    }

    @Override
    public List<RecipeSummaryResponse> execute() {
        UserId userId = sessionService.getCurrentUserId()
                .orElseThrow(AuthenticationRequiredException::new);
        List<RecipeId> ids = favoriteRepository.findFavoriteRecipeIdsByUserId(userId);
        if (ids.isEmpty()) return List.of();
        return recipeRepository.findSummariesByIds(ids);
    }
}
