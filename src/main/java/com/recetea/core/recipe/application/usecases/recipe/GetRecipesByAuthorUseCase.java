package com.recetea.core.recipe.application.usecases.recipe;

import com.recetea.core.recipe.application.ports.in.dto.RecipeSummaryResponse;
import com.recetea.core.recipe.application.ports.in.recipe.IGetRecipesByAuthorUseCase;
import com.recetea.core.recipe.application.ports.out.recipe.IRecipeRepository;
import com.recetea.core.shared.domain.PageRequest;
import com.recetea.core.shared.domain.PageResponse;
import com.recetea.core.user.domain.UserId;

import java.util.Objects;

public class GetRecipesByAuthorUseCase implements IGetRecipesByAuthorUseCase {

    private final IRecipeRepository repository;

    public GetRecipesByAuthorUseCase(IRecipeRepository repository) {
        this.repository = Objects.requireNonNull(repository);
    }

    @Override
    public PageResponse<RecipeSummaryResponse> execute(UserId authorId, PageRequest page) {
        Objects.requireNonNull(authorId, "authorId es obligatorio.");
        Objects.requireNonNull(page,     "page es obligatorio.");
        return repository.findByAuthorId(authorId, page);
    }
}
