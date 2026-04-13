package com.recetea.core.recipe.application.usecases.recipe;

import com.recetea.core.recipe.application.ports.in.dto.RecipeSummaryResponse;
import com.recetea.core.recipe.application.ports.in.recipe.IGetAllRecipesUseCase;
import com.recetea.core.recipe.application.ports.out.recipe.IRecipeRepository;
import java.util.List;

/**
 * Implementa la lógica de aplicación para la obtención del catálogo general de recetas.
 * Coordina la recuperación de datos desde el repositorio de infraestructura y su
 * transformación a un formato de salida optimizado (DTO), asegurando el aislamiento
 * estricto de las entidades del Domain.
 */
public class GetAllRecipesUseCase implements IGetAllRecipesUseCase {

    private final IRecipeRepository repository;

    /**
     * Inicializa el caso de uso mediante inyección de dependencias.
     * El acoplamiento se realiza exclusivamente contra la abstracción del puerto de salida,
     * manteniendo la lógica de negocio desacoplada de la tecnología de persistencia.
     *
     * @param repository Contrato de salida para el acceso a la persistencia de recetas.
     */
    public GetAllRecipesUseCase(IRecipeRepository repository) {
        this.repository = repository;
    }

    /**
     * Ejecuta la consulta de recetas. Transforma las entidades complejas del
     * Aggregate Root en registros inmutables (Records) que contienen únicamente
     * la información estrictamente necesaria para las vistas de resumen o listados.
     *
     * @return Colección inmutable de registros RecipeSummaryResponse.
     */
    @Override
    public List<RecipeSummaryResponse> execute() {
        return repository.findAll().stream()
                .map(recipe -> new RecipeSummaryResponse(
                        recipe.getId(),
                        recipe.getTitle(),
                        recipe.getPreparationTimeMinutes(),
                        recipe.getServings()
                ))
                .toList();
    }
}