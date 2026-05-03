package com.recetea.core.recipe.application.usecases.difficulty;

import com.recetea.core.recipe.application.ports.in.difficulty.IGetAllDifficultiesUseCase;
import com.recetea.core.recipe.application.ports.out.difficulty.IDifficultyRepository;
import com.recetea.core.recipe.domain.Difficulty;

import java.util.List;

/**
 * Returns the four-row difficulty catalogue (Fácil / Medio / Difícil / Muy
 * Difícil). Same lazy-cache model as the category use case: first call hits
 * the DB, subsequent calls return the cached unmodifiable list. The catalogue
 * is treated as immutable at runtime — there is no cache invalidation hook.
 *
 * <p><b>ES — </b>Devuelve el catálogo de dificultad de cuatro filas
 * (Fácil / Medio / Difícil / Muy Difícil). Mismo modelo de caché
 * perezosa que el caso de uso de categoría: la primera llamada toca
 * la BD, las posteriores devuelven la lista inmutable cacheada. El
 * catálogo se trata como inmutable en runtime — no hay hook de
 * invalidación de caché.
 */
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
