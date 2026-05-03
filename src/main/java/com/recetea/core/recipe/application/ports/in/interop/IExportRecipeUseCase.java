package com.recetea.core.recipe.application.ports.in.interop;

import com.recetea.core.recipe.domain.vo.RecipeId;

import java.io.File;

/**
 * Serialises a recipe to an XML document on disk.
 *
 * <p>The recipe is loaded fresh from the repository so the exported payload
 * reflects the persistent state, never an in-memory copy. The destination
 * file is overwritten if it exists — the FileChooser dialog at the call
 * site handles the "are you sure" prompt with the OS-native UX.
 *
 * <p>Throws {@code IllegalArgumentException} when the id has no matching
 * row (defends against a deletion racing with the export click).
 *
 * <p><b>ES — </b>Serializa una receta a un documento XML en disco.
 *
 * <p>La receta se carga fresca desde el repositorio para que el payload
 * exportado refleje el estado persistente, nunca una copia en memoria.
 * El archivo destino se sobreescribe si existe — el diálogo
 * FileChooser en el sitio de llamada gestiona el aviso de "¿estás
 * seguro?" con la UX nativa del SO.
 *
 * <p>Lanza {@code IllegalArgumentException} cuando el id no tiene fila
 * correspondiente (defensa contra una eliminación que entre en carrera
 * con el click de exportar).
 */
public interface IExportRecipeUseCase {

    void execute(RecipeId recipeId, File destination);
}
