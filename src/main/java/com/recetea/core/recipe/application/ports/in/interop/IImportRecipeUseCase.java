package com.recetea.core.recipe.application.ports.in.interop;

import com.recetea.core.recipe.domain.vo.RecipeId;

import java.io.File;

/**
 * Imports an XML recipe document into the database, attributing authorship
 * to the current session user (sourced from {@code IUserSessionService} —
 * the source document's author hint is intentionally ignored to prevent
 * impersonation).
 *
 * <p>Throws {@code AuthenticationRequiredException} when no session exists
 * and {@code XmlInteropException} (technical code {@code INT-400}) for any
 * structural / catalogue-resolution failure. Returns the id of the
 * persisted aggregate so the caller can navigate to the detail view.
 *
 * <p><b>ES — </b>Importa un documento XML de receta a la base de datos,
 * atribuyendo la autoría al usuario actual de sesión (obtenido de
 * {@code IUserSessionService} — la pista de autor del documento fuente
 * se ignora intencionadamente para evitar la suplantación).
 *
 * <p>Lanza {@code AuthenticationRequiredException} cuando no hay sesión
 * y {@code XmlInteropException} (código técnico {@code INT-400}) ante
 * cualquier fallo estructural / de resolución de catálogo. Devuelve el
 * id del agregado persistido para que el llamador pueda navegar a la
 * vista de detalle.
 */
public interface IImportRecipeUseCase {

    RecipeId execute(File source);
}
