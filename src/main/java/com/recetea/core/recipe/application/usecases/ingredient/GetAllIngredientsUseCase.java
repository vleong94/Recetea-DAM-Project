package com.recetea.core.recipe.application.usecases.ingredient;

import com.recetea.core.recipe.application.ports.in.dto.IngredientResponse;
import com.recetea.core.recipe.application.ports.in.ingredient.IGetAllIngredientsUseCase;
import com.recetea.core.recipe.application.ports.out.ingredient.IIngredientRepository;
import java.util.List;

public class GetAllIngredientsUseCase implements IGetAllIngredientsUseCase {

    private final IIngredientRepository repository;

    public GetAllIngredientsUseCase(IIngredientRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<IngredientResponse> execute() {
        return repository.findAll().stream()
                .map(ingredient -> new IngredientResponse(
                        ingredient.getId(),
                        ingredient.getName()
                ))
                .toList();
    }
}
