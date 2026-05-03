package com.recetea.core.recipe.application.usecases.ingredient;

import com.recetea.core.recipe.application.ports.in.dto.IngredientResponse;
import com.recetea.core.recipe.application.ports.in.ingredient.IGetAllIngredientsUseCase;
import com.recetea.core.recipe.application.ports.out.ingredient.IIngredientRepository;
import java.util.List;

/**
 * Returns the full ingredient catalogue, projected to {@link IngredientResponse}
 * (id + name). With ~297 rows in the seeded catalogue, the dashboard and
 * recipe-form combo boxes both consume this; the {@code AutocompleteHelper}
 * narrows the visible dropdown client-side via NFD-normalised contains-match.
 *
 * <p>Same lazy-cache model as the other catalogue use cases.
 *
 * <p><b>ES — </b>Devuelve el catálogo completo de ingredientes,
 * proyectado a {@link IngredientResponse} (id + nombre). Con ~297
 * filas en el catálogo semilla, lo consumen tanto el dashboard como
 * los ComboBox del formulario de receta; el
 * {@code AutocompleteHelper} estrecha el desplegable visible en
 * cliente vía coincidencia contains normalizada con NFD.
 *
 * <p>Mismo modelo de caché perezosa que los otros casos de uso de
 * catálogo.
 */
public class GetAllIngredientsUseCase implements IGetAllIngredientsUseCase {

    private final IIngredientRepository repository;

    public GetAllIngredientsUseCase(IIngredientRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<IngredientResponse> execute() {
        return repository.findAll().stream()
                .map(ingredient -> new IngredientResponse(
                        ingredient.id(),
                        ingredient.name()
                ))
                .toList();
    }
}
