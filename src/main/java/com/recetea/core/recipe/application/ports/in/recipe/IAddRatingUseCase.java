package com.recetea.core.recipe.application.ports.in.recipe;

import com.recetea.core.recipe.application.ports.in.dto.AddRatingRequest;

public interface IAddRatingUseCase {

    void execute(AddRatingRequest request);
}