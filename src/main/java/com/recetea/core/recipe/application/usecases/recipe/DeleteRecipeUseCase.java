package com.recetea.core.recipe.application.usecases.recipe;

import com.recetea.core.recipe.application.ports.in.recipe.IDeleteRecipeUseCase;
import com.recetea.core.recipe.application.ports.out.recipe.IRecipeRepository;
import com.recetea.core.recipe.domain.AuthenticationRequiredException;
import com.recetea.core.recipe.domain.Recipe;
import com.recetea.core.recipe.domain.RecipeNotFoundException;
import com.recetea.core.recipe.domain.UnauthorizedRecipeAccessException;
import com.recetea.core.recipe.domain.vo.RecipeId;
import com.recetea.core.shared.application.ports.in.IUserSessionService;
import com.recetea.core.shared.application.ports.out.ITransactionManager;
import com.recetea.core.social.application.ports.out.IFavoriteRepository;
import com.recetea.core.user.domain.UserId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Removes a recipe and any favorites pointing at it — cross-module
 * atomic deletion across the {@code recipe} and {@code social} bounded
 * contexts.
 *
 * <p><b>Ordering.</b> The favorites cleanup runs <em>before</em> the
 * recipe delete inside the same transaction. This is defense-in-depth
 * against the schema's {@code ON DELETE CASCADE} on
 * {@code favorites.recipe_id}: routing the cleanup through the
 * application layer keeps the use case's intent self-documenting and
 * decouples correctness from a schema setting that could be relaxed in
 * the future. {@code DeleteRecipeUseCaseTest} verifies the strict
 * ordering, the shared {@code ScopedValue} connection binding, and the
 * roll-back path when the favourite cleanup throws.
 *
 * <p>Ownership is checked first — non-owners see
 * {@code UnauthorizedRecipeAccessException} before any write executes.
 *
 * <p><b>ES — </b>Elimina una receta y todos los favoritos que apunten
 * a ella — eliminación atómica entre módulos, a través de los
 * contextos delimitados {@code recipe} y {@code social}.
 *
 * <p><b>Orden.</b> La limpieza de favoritos se ejecuta <em>antes</em>
 * que el delete de la receta dentro de la misma transacción. Esto es
 * defensa en profundidad frente al {@code ON DELETE CASCADE} del
 * esquema sobre {@code favorites.recipe_id}: enrutar la limpieza por
 * la capa de aplicación mantiene la intención del caso de uso
 * autoexplicativa y desacopla la corrección de un ajuste de esquema
 * que podría relajarse en el futuro.
 * {@code DeleteRecipeUseCaseTest} verifica el orden estricto, el
 * binding compartido de la conexión {@code ScopedValue} y la ruta
 * de rollback cuando la limpieza de favoritos lanza excepción.
 *
 * <p>La propiedad se comprueba primero — los no-propietarios ven
 * {@code UnauthorizedRecipeAccessException} antes de que se ejecute
 * cualquier escritura.
 */
public class DeleteRecipeUseCase implements IDeleteRecipeUseCase {

    private static final Logger log = LoggerFactory.getLogger(DeleteRecipeUseCase.class);

    private final IRecipeRepository recipeRepository;
    private final IFavoriteRepository favoriteRepository;
    private final ITransactionManager transactionManager;
    private final IUserSessionService sessionService;

    public DeleteRecipeUseCase(IRecipeRepository recipeRepository,
                               IFavoriteRepository favoriteRepository,
                               ITransactionManager transactionManager,
                               IUserSessionService sessionService) {
        this.recipeRepository   = recipeRepository;
        this.favoriteRepository = favoriteRepository;
        this.transactionManager = transactionManager;
        this.sessionService     = sessionService;
    }

    @Override
    public void execute(RecipeId recipeId) {
        log.info("Deleting recipe: {}", recipeId.value());

        // Both writes run inside one transaction so the deletion is atomic across the
        // recipe + social bounded contexts. Favourites are cleared first so any FK
        // constraint that fires before the recipe row is gone sees a consistent state;
        // both calls share the ScopedValue-bound connection via the BaseJdbcRepository
        // withConnection pattern, so the deletes commit or roll back together.
        transactionManager.execute(() -> {
            Recipe recipe = recipeRepository.findById(recipeId)
                    .orElseThrow(() -> new RecipeNotFoundException(recipeId.value()));

            UserId currentUser = sessionService.getCurrentUserId()
                    .orElseThrow(AuthenticationRequiredException::new);
            if (!recipe.getAuthorId().equals(currentUser)) {
                throw new UnauthorizedRecipeAccessException(
                        "User " + currentUser.value() + " is not authorized to delete recipe " + recipeId.value() + ".");
            }

            favoriteRepository.deleteAllByRecipeId(recipeId);
            recipeRepository.delete(recipeId);
        });

        log.info("Recipe {} deleted successfully.", recipeId.value());
    }
}
