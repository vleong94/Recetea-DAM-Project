package com.recetea.core.recipe.application.ports.in.media;

import com.recetea.core.recipe.domain.vo.RecipeId;

import java.io.InputStream;

/**
 * Persists a media file against a recipe — stores bytes through
 * {@code IMediaStorageService} first, then writes the {@code recipe_media}
 * row inside a transaction.
 *
 * <p><b>Compensating action.</b> If the DB write fails after the file
 * lands, the implementation deletes the just-stored file so storage is
 * never left holding orphan bytes that the DB doesn't know about. The
 * inverse drift (DB row pointing at a missing file) is acceptable — a GC
 * sweep can reconcile, but a leaked file with no row is invisible.
 *
 * <p>Caller must ensure the stream is closed; the implementation reads it
 * to EOF inside the storage adapter and releases its own resources.
 * Throws {@code UnauthorizedRecipeAccessException} when the session user
 * doesn't own the recipe.
 *
 * <p><b>ES — </b>Persiste un archivo multimedia asociado a una receta —
 * almacena los bytes a través de {@code IMediaStorageService} primero,
 * y luego escribe la fila {@code recipe_media} dentro de una
 * transacción.
 *
 * <p><b>Acción compensatoria.</b> Si la escritura en BD falla después
 * de que el archivo aterrice, la implementación elimina el archivo
 * recién almacenado para que el storage nunca quede con bytes
 * huérfanos que la BD desconoce. La deriva inversa (fila en BD
 * apuntando a un archivo inexistente) es aceptable — un barrido GC
 * puede reconciliar, pero un archivo filtrado sin fila es invisible.
 *
 * <p>El llamador debe asegurar que el stream se cierre; la
 * implementación lo lee hasta EOF dentro del adaptador de storage y
 * libera sus propios recursos. Lanza
 * {@code UnauthorizedRecipeAccessException} cuando el usuario de
 * sesión no es propietario de la receta.
 */
public interface IAttachMediaUseCase {

    void execute(RecipeId recipeId, InputStream data, String originalName);
}
