package com.recetea.core.recipe.application.ports.in.media;

import com.recetea.core.recipe.domain.vo.RecipeId;
import com.recetea.core.recipe.domain.vo.RecipeMediaId;

/**
 * Removes a media item from a recipe — deletes the {@code recipe_media}
 * row first (inside a transaction), then deletes the underlying file.
 *
 * <p>Order is intentional and the inverse of {@code IAttachMediaUseCase}:
 * the DB is the authority on whether the file is referenced, so removing
 * the row first means a mid-flight failure leaves an orphan file (cheap
 * to clean up) rather than a dangling row pointing at missing bytes.
 *
 * <p>Throws {@code UnauthorizedRecipeAccessException} when the session
 * user doesn't own the recipe. Removing the only media row clears the
 * cover image — the next attached file becomes the new {@code is_main}
 * automatically.
 *
 * <p><b>ES — </b>Elimina un elemento multimedia de una receta — borra la
 * fila {@code recipe_media} primero (dentro de una transacción), y
 * después borra el archivo subyacente.
 *
 * <p>El orden es intencional y el inverso de
 * {@code IAttachMediaUseCase}: la BD es la autoridad sobre si el
 * archivo está referenciado, así que eliminar la fila primero implica
 * que un fallo a mitad de operación deja un archivo huérfano (barato
 * de limpiar) en lugar de una fila colgada apuntando a bytes
 * inexistentes.
 *
 * <p>Lanza {@code UnauthorizedRecipeAccessException} cuando el usuario
 * de sesión no es propietario de la receta. Eliminar la única fila
 * multimedia limpia la imagen de portada — el siguiente archivo
 * adjuntado se convierte automáticamente en el nuevo {@code is_main}.
 */
public interface IRemoveMediaUseCase {

    void execute(RecipeId recipeId, RecipeMediaId mediaId);
}
