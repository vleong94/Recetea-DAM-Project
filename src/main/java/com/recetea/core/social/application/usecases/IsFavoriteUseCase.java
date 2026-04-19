package com.recetea.core.social.application.usecases;

import com.recetea.core.recipe.domain.vo.RecipeId;
import com.recetea.core.shared.application.ports.in.IUserSessionService;
import com.recetea.core.social.application.ports.in.IIsFavoriteUseCase;
import com.recetea.core.social.application.ports.out.IFavoriteRepository;

public class IsFavoriteUseCase implements IIsFavoriteUseCase {

    private final IFavoriteRepository favoriteRepository;
    private final IUserSessionService sessionService;

    public IsFavoriteUseCase(IFavoriteRepository favoriteRepository,
                             IUserSessionService sessionService) {
        this.favoriteRepository = favoriteRepository;
        this.sessionService = sessionService;
    }

    @Override
    public boolean execute(RecipeId recipeId) {
        return sessionService.getCurrentUserId()
                .map(userId -> favoriteRepository.isFavorite(userId, recipeId))
                .orElse(false);
    }
}
