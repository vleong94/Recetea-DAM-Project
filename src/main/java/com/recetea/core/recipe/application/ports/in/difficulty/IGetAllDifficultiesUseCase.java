package com.recetea.core.recipe.application.ports.in.difficulty;

import com.recetea.core.recipe.domain.Difficulty;
import java.util.List;

public interface IGetAllDifficultiesUseCase {

    /** Returns all persisted difficulty levels; never null — returns an empty list when none exist. */
    List<Difficulty> execute();
}
