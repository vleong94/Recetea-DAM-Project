package com.recetea.core.recipe.application.ports.in.recipe;

import com.recetea.core.recipe.application.ports.in.dto.SaveRecipeRequest;
import com.recetea.core.recipe.domain.vo.RecipeId;

/**
 * Replaces a recipe's metadata + child collections with the values from
 * {@link SaveRecipeRequest}. Authorisation is enforced inside the implementation:
 * only the recipe's author may update it (throws
 * {@code UnauthorizedRecipeAccessException} otherwise).
 *
 * <p>The implementation chains {@code Recipe} witherers
 * ({@code withTitle / withDescription / withCategory / …}) and routes ingredient
 * + step changes through the smart-diff gateways inside the repository — so
 * existing rows are kept, edited rows are updated in place, and removed rows
 * are deleted. Callers receive no return value; failure throws.
 *
 * <p><b>ES — </b>Reemplaza los metadatos + colecciones hijas de una receta
 * con los valores de {@link SaveRecipeRequest}. La autorización se aplica
 * dentro de la implementación: sólo el autor de la receta puede
 * actualizarla (en caso contrario lanza
 * {@code UnauthorizedRecipeAccessException}).
 *
 * <p>La implementación encadena withers de {@code Recipe}
 * ({@code withTitle / withDescription / withCategory / …}) y enruta los
 * cambios de ingredientes + pasos a través de los gateways de smart-diff
 * dentro del repositorio — de modo que las filas existentes se mantienen,
 * las editadas se actualizan en sitio y las eliminadas se borran. Los
 * llamadores no reciben valor de retorno; los fallos se lanzan.
 */
public interface IUpdateRecipeUseCase {

    void execute(RecipeId recipeId, SaveRecipeRequest request);
}
