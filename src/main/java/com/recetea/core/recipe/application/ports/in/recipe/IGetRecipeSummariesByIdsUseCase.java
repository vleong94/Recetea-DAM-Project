package com.recetea.core.recipe.application.ports.in.recipe;

import com.recetea.core.recipe.application.ports.in.dto.RecipeSummaryResponse;
import com.recetea.core.recipe.domain.vo.RecipeId;

import java.util.List;

/**
 * Bulk hydration of summary rows by id — backs the favorites tab, where
 * the input list comes from {@code IGetUserFavoritesUseCase} and dictates
 * the display order.
 *
 * <p><b>The repository's row order is unspecified</b>: SQL returns rows in
 * whatever order the planner picks. The implementation reorders the
 * output to match the input id sequence so "most recently favorited" is
 * preserved end-to-end. Missing ids (deleted recipes) are silently
 * dropped — no exception, the favorites tab simply renders fewer cards.
 *
 * <p><b>ES — </b>Hidratación masiva de filas resumen por id — respalda la
 * pestaña de favoritos, donde la lista de entrada proviene de
 * {@code IGetUserFavoritesUseCase} y dicta el orden de presentación.
 *
 * <p><b>El orden de filas del repositorio no está especificado</b>: SQL
 * devuelve las filas en el orden que decida el planner. La
 * implementación reordena la salida para que cuadre con la secuencia de
 * ids de entrada, de modo que "más recientemente favoritado" se
 * preserva de extremo a extremo. Los ids ausentes (recetas eliminadas)
 * se descartan silenciosamente — sin excepción, la pestaña de favoritos
 * simplemente renderiza menos tarjetas.
 */
public interface IGetRecipeSummariesByIdsUseCase {
    List<RecipeSummaryResponse> execute(List<RecipeId> ids);
}
