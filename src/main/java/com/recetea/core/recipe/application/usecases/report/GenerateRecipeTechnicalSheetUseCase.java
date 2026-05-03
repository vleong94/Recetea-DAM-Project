package com.recetea.core.recipe.application.usecases.report;

import com.recetea.core.recipe.application.ports.in.report.IGenerateRecipeTechnicalSheetUseCase;
import com.recetea.core.recipe.application.ports.out.recipe.IRecipeRepository;
import com.recetea.core.recipe.application.ports.out.report.IRecipeReportPort;
import com.recetea.core.recipe.domain.Recipe;
import com.recetea.core.recipe.domain.vo.RecipeId;
import com.recetea.core.user.application.ports.out.IUserRepository;

/**
 * Generates a PDF technical sheet for one recipe via {@code IRecipeReportPort}.
 * Loads the recipe + resolves the author's username (the {@code Recipe} record
 * holds an {@code authorId} but no display name — the design keeps Recipe free
 * of UI strings), then hands both to the report adapter.
 *
 * <p>Null author username falls back to a localised "Desconocido" placeholder
 * inside the adapter — covers the case where the author account was deleted
 * after the recipe was published.
 *
 * <p>Returns the PDF as {@code byte[]} rather than streaming to a destination —
 * the controller writes the bytes to a user-chosen file via {@code FileChooser}
 * inside a {@code Task<Void>}, keeping the I/O off the FX thread.
 *
 * <p><b>ES — </b>Genera una ficha técnica PDF para una receta vía
 * {@code IRecipeReportPort}. Carga la receta + resuelve el username
 * del autor (el record {@code Recipe} lleva un {@code authorId} pero
 * no un nombre de presentación — el diseño mantiene Recipe libre de
 * cadenas de UI), y luego entrega ambos al adaptador de informes.
 *
 * <p>Si el username del autor es null, recurre a un placeholder
 * localizado "Desconocido" dentro del adaptador — cubre el caso de
 * que la cuenta del autor haya sido eliminada después de publicar
 * la receta.
 *
 * <p>Devuelve el PDF como {@code byte[]} en lugar de hacer streaming
 * a un destino — el controlador escribe los bytes a un archivo
 * elegido por el usuario vía {@code FileChooser} dentro de un
 * {@code Task<Void>}, manteniendo la E/S fuera del hilo FX.
 */
public class GenerateRecipeTechnicalSheetUseCase implements IGenerateRecipeTechnicalSheetUseCase {

    private final IRecipeRepository repository;
    private final IUserRepository   userRepository;
    private final IRecipeReportPort reportPort;

    public GenerateRecipeTechnicalSheetUseCase(IRecipeRepository repository,
                                               IUserRepository userRepository,
                                               IRecipeReportPort reportPort) {
        this.repository     = repository;
        this.userRepository = userRepository;
        this.reportPort     = reportPort;
    }

    @Override
    public byte[] execute(RecipeId id) {
        Recipe recipe = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Recipe not found: " + id));
        String authorUsername = userRepository.findById(recipe.getAuthorId())
                .map(u -> u.username().value())
                .orElse(null);
        return reportPort.generateTechnicalSheet(recipe, authorUsername);
    }
}
