package com.recetea.core.recipe.application.usecases.interop;

import com.recetea.core.recipe.application.ports.in.interop.IExportRecipeUseCase;
import com.recetea.core.recipe.application.ports.out.interop.IRecipeInteropPort;
import com.recetea.core.recipe.application.ports.out.recipe.IRecipeRepository;
import com.recetea.core.recipe.domain.Recipe;
import com.recetea.core.recipe.domain.vo.RecipeId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * Serialises a recipe to an XML file via {@code IRecipeInteropPort}. The
 * recipe is loaded fresh from the repository so the exported payload reflects
 * the current persistent state, not an in-memory copy that may be stale.
 *
 * <p>No transaction wrapping — read-only. {@code XmlInteropAdapter} handles
 * the JAXB marshalling + the XSD schema reference; this layer only sequences
 * "load → hand to adapter".
 *
 * <p>Throws {@link IllegalArgumentException} when the recipe id has no
 * matching row. The export is typically launched from the recipe detail
 * page, so the id is known to exist at click time — the throw defends
 * against a deletion racing with the export action.
 *
 * <p><b>ES — </b>Serializa una receta a un archivo XML vía
 * {@code IRecipeInteropPort}. La receta se carga fresca desde el
 * repositorio para que el payload exportado refleje el estado
 * persistente actual, no una copia en memoria que pueda estar
 * obsoleta.
 *
 * <p>Sin envoltorio transaccional — sólo lectura.
 * {@code XmlInteropAdapter} gestiona el marshalling JAXB y la
 * referencia al schema XSD; esta capa sólo secuencia "cargar →
 * entregar al adaptador".
 *
 * <p>Lanza {@link IllegalArgumentException} cuando el id de la receta
 * no tiene fila correspondiente. La exportación se lanza típicamente
 * desde la página de detalle de la receta, así que el id se sabe que
 * existe al hacer click — el throw defiende contra una eliminación
 * que entre en carrera con la acción de exportar.
 */
public class ExportRecipeUseCase implements IExportRecipeUseCase {

    private static final Logger log = LoggerFactory.getLogger(ExportRecipeUseCase.class);

    private final IRecipeRepository recipeRepository;
    private final IRecipeInteropPort interopPort;

    public ExportRecipeUseCase(IRecipeRepository recipeRepository, IRecipeInteropPort interopPort) {
        this.recipeRepository = recipeRepository;
        this.interopPort = interopPort;
    }

    @Override
    public void execute(RecipeId recipeId, File destination) {
        log.info("Exporting recipe: {}", recipeId.value());

        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Recipe not found with ID: " + recipeId.value()));
        interopPort.exportRecipe(recipe, destination);

        log.info("Recipe {} exported to: '{}'", recipeId.value(), destination.getName());
    }
}
