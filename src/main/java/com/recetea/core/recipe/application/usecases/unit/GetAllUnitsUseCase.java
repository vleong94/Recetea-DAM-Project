package com.recetea.core.recipe.application.usecases.unit;

import com.recetea.core.recipe.application.ports.in.dto.UnitResponse;
import com.recetea.core.recipe.application.ports.in.unit.IGetAllUnitsUseCase;
import com.recetea.core.recipe.application.ports.out.unit.IUnitRepository;
import java.util.List;

/**
 * Returns the full list of unit measures, projected into the lighter-weight
 * {@link UnitResponse} DTO (carries id + name only — the abbreviation lives
 * on the underlying {@code Unit} record but isn't surfaced through this use
 * case). Mapping happens here rather than in the repository to keep the
 * repository's concern strictly persistence; the projection lives in the
 * application layer.
 *
 * <p>Same lazy-cache model as the other catalogue use cases — the underlying
 * repository call is a no-op after first invocation.
 *
 * <p><b>ES — </b>Devuelve la lista completa de unidades de medida,
 * proyectada al DTO más ligero {@link UnitResponse} (lleva sólo id +
 * nombre — la abreviatura vive en el record subyacente {@code Unit}
 * pero no se expone a través de este caso de uso). El mapeo ocurre
 * aquí en lugar de en el repositorio para que la responsabilidad del
 * repositorio sea estrictamente persistencia; la proyección vive en
 * la capa de aplicación.
 *
 * <p>Mismo modelo de caché perezosa que los otros casos de uso de
 * catálogo — la llamada al repositorio subyacente es un no-op
 * después de la primera invocación.
 */
public class GetAllUnitsUseCase implements IGetAllUnitsUseCase {

    private final IUnitRepository repository;

    public GetAllUnitsUseCase(IUnitRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<UnitResponse> execute() {
        return repository.findAll().stream()
                .map(unit -> new UnitResponse(
                        unit.id(),
                        unit.name()
                ))
                .toList();
    }
}
