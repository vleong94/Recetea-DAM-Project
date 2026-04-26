package com.recetea.core.recipe.application.ports.in.category;

import com.recetea.core.recipe.domain.Category;
import java.util.List;

public interface IGetAllCategoriesUseCase {

    /** Returns all persisted categories; never null — returns an empty list when none exist. */
    List<Category> execute();
}
