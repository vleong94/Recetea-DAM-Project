package com.recetea.core.recipe.application.usecases.category;

import com.recetea.core.recipe.application.ports.in.category.IGetAllCategoriesUseCase;
import com.recetea.core.recipe.application.ports.out.category.ICategoryRepository;
import com.recetea.core.recipe.domain.Category;

import java.util.List;

public class GetAllCategoriesUseCase implements IGetAllCategoriesUseCase {

    private final ICategoryRepository repository;

    public GetAllCategoriesUseCase(ICategoryRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<Category> execute() {
        return repository.findAll();
    }
}
