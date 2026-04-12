package com.recetea.core.usecases.ingredient;

import com.recetea.core.domain.Ingredient;
import com.recetea.core.ports.in.ingredient.IGetAllIngredientsUseCase;
import com.recetea.core.ports.out.IIngredientRepository;
import java.util.List;

public class GetAllIngredientsUseCase implements IGetAllIngredientsUseCase {
    private final IIngredientRepository repository;
    public GetAllIngredientsUseCase(IIngredientRepository repository) { this.repository = repository; }
    @Override public List<Ingredient> execute() { return repository.findAll(); }
}