package com.recetea.core.recipe.application.ports.in.difficulty;

import com.recetea.core.recipe.domain.Difficulty;
import java.util.List;

/**
 * Lists every difficulty level for the recipe-form combo and the dashboard
 * filter drawer. Backed by the JVM-level catalogue cache — see
 * {@code IDifficultyRepository}.
 *
 * <p><b>ES — </b>Lista todos los niveles de dificultad para el combo del
 * formulario de receta y el cajón de filtros del dashboard. Respaldado
 * por la caché de catálogo a nivel de JVM — véase
 * {@code IDifficultyRepository}.
 */
public interface IGetAllDifficultiesUseCase {

    /**
     * Returns all persisted difficulty levels; never null — returns an empty list when none exist.
     *
     * <p><b>ES — </b>Devuelve todos los niveles de dificultad persistidos;
     * nunca null — devuelve lista vacía si no hay ninguno.
     */
    List<Difficulty> execute();
}
