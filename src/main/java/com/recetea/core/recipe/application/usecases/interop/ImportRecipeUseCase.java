package com.recetea.core.recipe.application.usecases.interop;

import com.recetea.core.recipe.application.ports.in.interop.IImportRecipeUseCase;
import com.recetea.core.recipe.application.ports.out.interop.IRecipeInteropPort;
import com.recetea.core.recipe.application.ports.out.recipe.IRecipeRepository;
import com.recetea.core.recipe.domain.AuthenticationRequiredException;
import com.recetea.core.recipe.domain.Recipe;
import com.recetea.core.recipe.domain.vo.RecipeId;
import com.recetea.core.shared.application.ports.in.IUserSessionService;
import com.recetea.core.shared.application.ports.out.ITransactionManager;
import com.recetea.core.user.domain.UserId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * Reads an XML file via {@code IRecipeInteropPort} and persists the
 * resulting domain Recipe under the current user as author. Critical
 * security property: the {@code authorId} is sourced from the active
 * session, <em>not</em> from anything in the XML payload — an XML file
 * exported by user A and imported by user B becomes user B's recipe.
 *
 * <p>The unmarshalling step ({@code interopPort.importFromSource}) runs
 * outside the transaction because XML parsing + XSD validation + catalogue
 * resolution can throw without touching the DB; only the final
 * {@code save(recipe)} is wrapped in {@code transactionManager.execute}.
 *
 * <p>Catalogue mismatches (a category / unit / ingredient name in the XML
 * that doesn't resolve in the local catalogue) bubble up as
 * {@code InvalidIngredientException} from the adapter — that's a domain
 * exception with code {@code VALIDATION_ERROR}, so the global handler
 * surfaces it as a WARNING to the user.
 *
 * <p><b>ES — </b>Lee un archivo XML vía {@code IRecipeInteropPort} y
 * persiste la Recipe del dominio resultante con el usuario actual
 * como autor. Propiedad de seguridad crítica: el {@code authorId} se
 * obtiene de la sesión activa, <em>no</em> de nada del payload XML —
 * un archivo XML exportado por el usuario A e importado por el
 * usuario B se convierte en una receta del usuario B.
 *
 * <p>El paso de unmarshalling
 * ({@code interopPort.importFromSource}) se ejecuta fuera de la
 * transacción porque el parseo XML + validación XSD + resolución de
 * catálogo pueden lanzar excepción sin tocar la BD; sólo el
 * {@code save(recipe)} final se envuelve en
 * {@code transactionManager.execute}.
 *
 * <p>Las discrepancias de catálogo (un nombre de categoría / unidad /
 * ingrediente en el XML que no resuelve en el catálogo local) afloran
 * como {@code InvalidIngredientException} desde el adaptador — es una
 * excepción de dominio con código {@code VALIDATION_ERROR}, así que
 * el handler global la muestra al usuario como WARNING.
 */
public class ImportRecipeUseCase implements IImportRecipeUseCase {

    private static final Logger log = LoggerFactory.getLogger(ImportRecipeUseCase.class);

    private final IRecipeRepository recipeRepository;
    private final ITransactionManager transactionManager;
    private final IUserSessionService sessionService;
    private final IRecipeInteropPort interopPort;

    public ImportRecipeUseCase(IRecipeRepository recipeRepository,
                               ITransactionManager transactionManager,
                               IUserSessionService sessionService,
                               IRecipeInteropPort interopPort) {
        this.recipeRepository = recipeRepository;
        this.transactionManager = transactionManager;
        this.sessionService     = sessionService;
        this.interopPort        = interopPort;
    }

    @Override
    public RecipeId execute(File source) {
        UserId currentUser = sessionService.getCurrentUserId()
                .orElseThrow(AuthenticationRequiredException::new);

        log.info("Importing recipe from '{}' for user: {}", source.getName(), currentUser.value());

        Recipe recipe = interopPort.importFromSource(source, currentUser);

        RecipeId newId = transactionManager.execute(() -> recipeRepository.save(recipe));

        log.info("Recipe imported successfully. ID: {}", newId.value());
        return newId;
    }
}
