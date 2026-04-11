package com.recetea.core.ports;

import com.recetea.core.domain.Recipe;
import java.util.List;
import java.util.Optional;

/**
 * Outbound Port: Define el contrato para la persistencia de recetas.
 * Sigue el principio de Dependency Inversion. [cite: 1]
 */
public interface IRecipeRepository {

    /**
     * Persiste una nueva receta en el storage. [cite: 1]
     */
    void save(Recipe recipe);

    /**
     * Actualiza una receta existente y sincroniza sus ingredientes (Transaction).
     */
    void update(Recipe recipe);

    /**
     * Recupera una receta hidratada por su ID único. [cite: 1]
     */
    Optional<Recipe> findById(int id);

    /**
     * Extrae el catálogo completo de recetas. [cite: 1]
     */
    List<Recipe> findAll();

    /**
     * Elimina físicamente una receta (Cascade Delete en DB). [cite: 1]
     */
    void delete(int id);
}