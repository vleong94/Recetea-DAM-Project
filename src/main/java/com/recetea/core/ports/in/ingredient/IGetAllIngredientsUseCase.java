package com.recetea.core.ports.in.ingredient;

import com.recetea.core.domain.Ingredient;
import java.util.List;

public interface IGetAllIngredientsUseCase {
    List<Ingredient> execute();
}