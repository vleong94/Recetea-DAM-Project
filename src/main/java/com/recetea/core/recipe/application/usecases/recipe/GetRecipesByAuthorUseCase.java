package com.recetea.core.recipe.application.usecases.recipe;

import com.recetea.core.recipe.application.ports.in.dto.RecipeSummaryResponse;
import com.recetea.core.recipe.application.ports.in.recipe.IGetRecipesByAuthorUseCase;
import com.recetea.core.recipe.application.ports.out.recipe.IRecipeRepository;
import com.recetea.core.shared.domain.PageRequest;
import com.recetea.core.shared.domain.PageResponse;
import com.recetea.core.user.domain.UserId;

import java.util.Objects;

/**
 * Paginated list of recipes authored by one user — drives the "My creations"
 * tab in {@code UserProfileController}. The {@code authorId} parameter lets
 * the caller view either their own creations or, in principle, another user's
 * (the UI only exposes the self-view, but the use case doesn't enforce that).
 *
 * <p>Required-arg null-checks at the entrypoint surface a clean error before
 * the SQL prepared-statement boundary; the underlying gateway query relies on
 * a non-null author for its predicate.
 *
 * <p><b>ES — </b>Lista paginada de recetas creadas por un usuario —
 * impulsa la pestaña "Mis creaciones" en
 * {@code UserProfileController}. El parámetro {@code authorId}
 * permite al llamador ver sus propias creaciones o, en principio,
 * las de otro usuario (la UI sólo expone la vista propia, pero el
 * caso de uso no lo aplica).
 *
 * <p>Las comprobaciones de null en los argumentos requeridos en el
 * punto de entrada exponen un error limpio antes de la frontera del
 * prepared-statement SQL; la consulta del gateway subyacente depende
 * de un autor no nulo para su predicado.
 */
public class GetRecipesByAuthorUseCase implements IGetRecipesByAuthorUseCase {

    private final IRecipeRepository repository;

    public GetRecipesByAuthorUseCase(IRecipeRepository repository) {
        this.repository = Objects.requireNonNull(repository);
    }

    @Override
    public PageResponse<RecipeSummaryResponse> execute(UserId authorId, PageRequest page) {
        Objects.requireNonNull(authorId, "authorId is required.");
        Objects.requireNonNull(page,     "page is required.");
        return repository.findByAuthorId(authorId, page);
    }
}
