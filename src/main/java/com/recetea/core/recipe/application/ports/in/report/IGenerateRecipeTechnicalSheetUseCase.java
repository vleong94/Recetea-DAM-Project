package com.recetea.core.recipe.application.ports.in.report;

import com.recetea.core.recipe.domain.vo.RecipeId;

/**
 * Renders a recipe to a printable PDF technical sheet — author + meta + the
 * full ingredient and step listings, strict grayscale by class invariant.
 *
 * <p>Returns {@code byte[]} rather than streaming because the sheets are
 * small (~10–30 KB) and the controller writes through a {@code FileChooser}
 * dialog after the bytes land — buffering simplifies the UX path. The
 * adapter ({@code OpenPdfRecipeAdapter}) pre-sizes a 8 KiB
 * {@code ByteArrayOutputStream} to amortise growth.
 *
 * <p><b>ES — </b>Renderiza una receta a una ficha técnica imprimible en
 * PDF — autor + meta + listados completos de ingredientes y pasos,
 * estrictamente en escala de grises por invariante de clase.
 *
 * <p>Devuelve {@code byte[]} en lugar de streaming porque las fichas son
 * pequeñas (~10–30 KB) y el controlador escribe a través de un diálogo
 * {@code FileChooser} cuando los bytes están listos — el buffering
 * simplifica la ruta UX. El adaptador
 * ({@code OpenPdfRecipeAdapter}) predimensiona un
 * {@code ByteArrayOutputStream} de 8 KiB para amortizar el crecimiento.
 */
public interface IGenerateRecipeTechnicalSheetUseCase {

    byte[] execute(RecipeId id);
}
