package com.recetea.core.recipe.application.ports.in.recipe;

import com.recetea.core.recipe.domain.vo.RecipeId;

/**
 * Removes a recipe owned by the current session user. The implementation
 * runs an atomic cross-module cleanup inside one transaction:
 * {@code favoriteRepository.deleteAllByRecipeId(...)} fires <em>before</em>
 * {@code recipeRepository.delete(...)} so any FK firing mid-transaction
 * sees a consistent state. Both writes commit or roll back together via the
 * {@code ScopedValue}-bound JDBC connection.
 *
 * <p>Authorisation: only the author can delete; non-owners get
 * {@code UnauthorizedRecipeAccessException}. Missing recipes get
 * {@code RecipeNotFoundException}. Anonymous flows get
 * {@code AuthenticationRequiredException}.
 *
 * <p><b>ES — </b>Elimina una receta cuyo dueño es el usuario actual de
 * sesión. La implementación ejecuta una limpieza atómica entre módulos
 * dentro de una sola transacción:
 * {@code favoriteRepository.deleteAllByRecipeId(...)} se dispara
 * <em>antes</em> que {@code recipeRepository.delete(...)} para que
 * cualquier FK que se active durante la transacción vea un estado
 * consistente. Ambas escrituras hacen commit o rollback juntas mediante
 * la conexión JDBC ligada al {@code ScopedValue}.
 *
 * <p>Autorización: sólo el autor puede borrar; los no-propietarios
 * reciben {@code UnauthorizedRecipeAccessException}. Las recetas
 * inexistentes reciben {@code RecipeNotFoundException}. Los flujos
 * anónimos reciben {@code AuthenticationRequiredException}.
 */
public interface IDeleteRecipeUseCase {

    void execute(RecipeId recipeId);
}
