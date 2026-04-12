package com.recetea.core.usecases.unit;

import com.recetea.core.domain.Unit;
import com.recetea.core.ports.in.unit.IGetAllUnitsUseCase;
import com.recetea.core.ports.out.IUnitRepository;
import java.util.List;

public class GetAllUnitsUseCase implements IGetAllUnitsUseCase {
    private final IUnitRepository repository;
    public GetAllUnitsUseCase(IUnitRepository repository) { this.repository = repository; }
    @Override public List<Unit> execute() { return repository.findAll(); }
}