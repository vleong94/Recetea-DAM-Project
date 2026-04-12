package com.recetea.core.ports.in.unit;

import com.recetea.core.domain.Unit;
import java.util.List;

public interface IGetAllUnitsUseCase {
    List<Unit> execute();
}