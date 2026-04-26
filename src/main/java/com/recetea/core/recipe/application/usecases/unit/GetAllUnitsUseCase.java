package com.recetea.core.recipe.application.usecases.unit;

import com.recetea.core.recipe.application.ports.in.dto.UnitResponse;
import com.recetea.core.recipe.application.ports.in.unit.IGetAllUnitsUseCase;
import com.recetea.core.recipe.application.ports.out.unit.IUnitRepository;
import java.util.List;

public class GetAllUnitsUseCase implements IGetAllUnitsUseCase {

    private final IUnitRepository repository;

    public GetAllUnitsUseCase(IUnitRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<UnitResponse> execute() {
        return repository.findAll().stream()
                .map(unit -> new UnitResponse(
                        unit.getId(),
                        unit.getName()
                ))
                .toList();
    }
}
