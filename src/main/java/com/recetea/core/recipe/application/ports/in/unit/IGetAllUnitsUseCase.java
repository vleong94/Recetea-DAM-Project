package com.recetea.core.recipe.application.ports.in.unit;

import com.recetea.core.recipe.application.ports.in.dto.UnitResponse;
import java.util.List;

/**
 * Lists every measurement unit as a {@link UnitResponse} — used by the
 * recipe-form ingredient row's unit combo. Same DTO-mapping rationale as
 * {@code IGetAllIngredientsUseCase}. Catalogue is cached at the repository
 * layer.
 *
 * <p><b>ES — </b>Lista todas las unidades de medida como
 * {@link UnitResponse} — usado por el combo de unidades en la fila de
 * ingrediente del formulario de receta. Misma justificación de mapeo a
 * DTOs que {@code IGetAllIngredientsUseCase}. El catálogo se cachea en
 * la capa de repositorio.
 */
public interface IGetAllUnitsUseCase {

    /**
     * Returns all persisted measurement units as DTOs; never null — returns an empty list when none exist.
     *
     * <p><b>ES — </b>Devuelve todas las unidades de medida persistidas
     * como DTOs; nunca null — devuelve lista vacía si no hay ninguna.
     */
    List<UnitResponse> execute();
}
