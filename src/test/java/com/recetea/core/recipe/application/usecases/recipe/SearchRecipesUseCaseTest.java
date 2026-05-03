package com.recetea.core.recipe.application.usecases.recipe;

import com.recetea.core.recipe.application.ports.in.dto.RecipeSummaryResponse;
import com.recetea.core.recipe.application.ports.in.dto.SearchCriteria;
import com.recetea.core.recipe.application.ports.out.recipe.IRecipeRepository;
import com.recetea.core.recipe.domain.vo.IngredientId;
import com.recetea.core.shared.domain.PageRequest;
import com.recetea.core.shared.domain.PageResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SearchRecipesUseCase — Pagination + criteria pass-through to the repository")
class SearchRecipesUseCaseTest {

    @Mock private IRecipeRepository recipeRepository;

    private SearchRecipesUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new SearchRecipesUseCase(recipeRepository);
    }

    @Test
    @DisplayName("execute: forwards criteria + pageRequest to IRecipeRepository.searchSummaries unchanged")
    void execute_DelegatesToRepository() {
        SearchCriteria criteria = new SearchCriteria(
                "cake", 60, 2,
                "Desserts", "Easy",
                List.of(new IngredientId(1)),
                "victor", 4);
        PageRequest page = new PageRequest(0, 10);
        PageResponse<RecipeSummaryResponse> expected =
                PageResponse.of(List.<RecipeSummaryResponse>of(), 0L, 10);
        when(recipeRepository.searchSummaries(criteria, page)).thenReturn(expected);

        PageResponse<RecipeSummaryResponse> actual = useCase.execute(criteria, page);

        assertSame(expected, actual,
                "Use case must return whatever the repository returns, with no defensive copy");
        verify(recipeRepository, times(1)).searchSummaries(criteria, page);
        verifyNoMoreInteractions(recipeRepository);
    }

    @Test
    @DisplayName("execute: forwards an empty/default criteria too — no client-side filtering applied here")
    void execute_DelegatesEvenWhenCriteriaIsEmpty() {
        SearchCriteria empty = new SearchCriteria(null, null, null, null, null, List.of(), null, null);
        PageRequest page = new PageRequest(0, 25);
        PageResponse<RecipeSummaryResponse> expected =
                PageResponse.of(List.<RecipeSummaryResponse>of(), 0L, 25);
        when(recipeRepository.searchSummaries(empty, page)).thenReturn(expected);

        useCase.execute(empty, page);

        verify(recipeRepository).searchSummaries(empty, page);
    }
}
