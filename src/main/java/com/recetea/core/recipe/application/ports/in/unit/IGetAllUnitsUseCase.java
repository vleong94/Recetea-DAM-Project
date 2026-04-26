package com.recetea.core.recipe.application.ports.in.unit;

import com.recetea.core.recipe.application.ports.in.dto.UnitResponse;
import java.util.List;

public interface IGetAllUnitsUseCase {

    /** Returns all persisted measurement units as DTOs; never null — returns an empty list when none exist. */
    List<UnitResponse> execute();
}
