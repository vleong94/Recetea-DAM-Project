package com.recetea.core.ports;

import com.recetea.core.domain.Recipe;

import java.util.List;
import java.util.Optional;

public interface IRecipeRepository {
    void save(Recipe recipe);
    Optional<Recipe> findById(int id); // Cambiado a int
    List<Recipe> findAll();
    void delete(int id);               // Cambiado a int
}