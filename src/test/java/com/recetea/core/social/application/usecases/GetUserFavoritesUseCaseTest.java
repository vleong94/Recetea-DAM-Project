package com.recetea.core.social.application.usecases;

import com.recetea.core.recipe.domain.AuthenticationRequiredException;
import com.recetea.core.recipe.domain.vo.RecipeId;
import com.recetea.core.shared.application.ports.in.IUserSessionService;
import com.recetea.core.social.application.ports.out.IFavoriteRepository;
import com.recetea.core.user.domain.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetUserFavoritesUseCase")
class GetUserFavoritesUseCaseTest {

    @Mock private IFavoriteRepository favoriteRepository;
    @Mock private IUserSessionService sessionService;

    private GetUserFavoritesUseCase useCase;

    private static final UserId   USER_ID   = new UserId(1);
    private static final RecipeId RECIPE_10 = new RecipeId(10);
    private static final RecipeId RECIPE_20 = new RecipeId(20);

    @BeforeEach
    void setUp() {
        useCase = new GetUserFavoritesUseCase(favoriteRepository, sessionService);
    }

    @Test
    @DisplayName("execute: throws AuthenticationRequiredException when there is no active session")
    void execute_ShouldThrow_WhenNoSession() {
        when(sessionService.getCurrentUserId()).thenReturn(Optional.empty());

        assertThrows(AuthenticationRequiredException.class, useCase::execute);

        verifyNoInteractions(favoriteRepository);
    }

    @Test
    @DisplayName("execute: returns an empty list when the user has no favorites")
    void execute_ShouldReturnEmpty_WhenNoFavorites() {
        when(sessionService.getCurrentUserId()).thenReturn(Optional.of(USER_ID));
        when(favoriteRepository.findFavoriteRecipeIdsByUserId(USER_ID)).thenReturn(List.of());

        List<RecipeId> result = useCase.execute();

        assertTrue(result.isEmpty());
        verify(favoriteRepository, times(1)).findFavoriteRecipeIdsByUserId(USER_ID);
    }

    @Test
    @DisplayName("execute: returns exactly the RecipeIds from the favorites repository")
    void execute_ShouldReturnFavoriteIds_WhenFavoritesExist() {
        when(sessionService.getCurrentUserId()).thenReturn(Optional.of(USER_ID));
        when(favoriteRepository.findFavoriteRecipeIdsByUserId(USER_ID))
                .thenReturn(List.of(RECIPE_10, RECIPE_20));

        List<RecipeId> result = useCase.execute();

        assertEquals(List.of(RECIPE_10, RECIPE_20), result);
        verify(favoriteRepository, times(1)).findFavoriteRecipeIdsByUserId(USER_ID);
    }
}
