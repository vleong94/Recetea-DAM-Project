package com.recetea.core.recipe.application.usecases.difficulty;

import com.recetea.core.recipe.application.ports.in.difficulty.IGetAllDifficultiesUseCase;
import com.recetea.core.recipe.application.ports.out.difficulty.IDifficultyRepository;
import com.recetea.core.recipe.domain.Difficulty;

import java.util.List;

public class GetAllDifficultiesUseCase implements IGetAllDifficultiesUseCase {

    private final IDifficultyRepository repository;

    public GetAllDifficultiesUseCase(IDifficultyRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<Difficulty> execute() {
        return repository.findAll();
    }
}
