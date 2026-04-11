package com.recetea.core.ports.in;

import com.recetea.core.domain.Recipe;
import java.util.List;

/**
 * Inbound Port: Contrato para la extracción del catálogo completo de recetas.
 */
public interface IGetAllRecipesUseCase {
    List<Recipe> execute();
}