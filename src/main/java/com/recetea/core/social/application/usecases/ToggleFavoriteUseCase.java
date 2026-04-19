package com.recetea.core.social.application.usecases;

import com.recetea.core.recipe.domain.AuthenticationRequiredException;
import com.recetea.core.recipe.domain.vo.RecipeId;
import com.recetea.core.shared.application.ports.in.IUserSessionService;
import com.recetea.core.shared.application.ports.out.ITransactionManager;
import com.recetea.core.social.application.ports.in.IToggleFavoriteUseCase;
import com.recetea.core.social.application.ports.out.IFavoriteRepository;
import com.recetea.core.user.domain.UserId;

public class ToggleFavoriteUseCase implements IToggleFavoriteUseCase {

    private final IFavoriteRepository favoriteRepository;
    private final ITransactionManager transactionManager;
    private final IUserSessionService sessionService;

    public ToggleFavoriteUseCase(IFavoriteRepository favoriteRepository,
                                 ITransactionManager transactionManager,
                                 IUserSessionService sessionService) {
        this.favoriteRepository = favoriteRepository;
        this.transactionManager = transactionManager;
        this.sessionService = sessionService;
    }

    @Override
    public void execute(RecipeId recipeId) {
        UserId userId = sessionService.getCurrentUserId()
                .orElseThrow(AuthenticationRequiredException::new);
        transactionManager.execute(() -> {
            if (favoriteRepository.isFavorite(userId, recipeId)) {
                favoriteRepository.delete(userId, recipeId);
            } else {
                favoriteRepository.save(userId, recipeId);
            }
        });
    }
}
