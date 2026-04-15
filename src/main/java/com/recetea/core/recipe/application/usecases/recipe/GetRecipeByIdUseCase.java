package com.recetea.core.recipe.application.usecases.recipe;

import com.recetea.core.recipe.application.ports.in.dto.RecipeDetailResponse;
import com.recetea.core.recipe.application.ports.in.dto.RecipeIngredientResponse;
import com.recetea.core.recipe.application.ports.in.recipe.IGetRecipeByIdUseCase;
import com.recetea.core.recipe.application.ports.out.recipe.IRecipeRepository;
import com.recetea.core.recipe.domain.Recipe;
import com.recetea.core.recipe.domain.RecipeIngredient;

import java.util.Optional;

/**
 * Caso de uso especializado en la obtención y proyección detallada de una receta.
 * Coordina la recuperación del agregado desde la infraestructura de persistencia
 * y su posterior transformación a un Data Transfer Object (DTO) inmutable,
 * garantizando que las entidades del dominio permanezcan aisladas de la vista.
 */
public class GetRecipeByIdUseCase implements IGetRecipeByIdUseCase {

    private final IRecipeRepository repository;

    /**
     * Inicializa el componente mediante la inyección de la interfaz del repositorio.
     * Este enfoque asegura que el caso de uso sea agnóstico a la implementación
     * técnica del almacenamiento, facilitando la escalabilidad y el mantenimiento.
     */
    public GetRecipeByIdUseCase(IRecipeRepository repository) {
        this.repository = repository;
    }

    /**
     * Ejecuta la lógica de consulta para localizar una receta por su identificador.
     * Retorna un contenedor opcional que encapsula la proyección de datos,
     * permitiendo una gestión segura de la nulidad en las capas de presentación.
     */
    @Override
    public Optional<RecipeDetailResponse> execute(int recipeId) {
        return repository.findById(recipeId)
                .map(this::mapToResponse);
    }

    /**
     * Transforma el Aggregate Root y su jerarquía interna en una respuesta plana.
     * Durante el proceso, extrae el valor escalar del Value Object AuthorId y orquesta
     * la conversión de la colección de ingredientes para satisfacer el contrato del DTO.
     */
    private RecipeDetailResponse mapToResponse(Recipe recipe) {
        return new RecipeDetailResponse(
                recipe.getId(),
                recipe.getAuthorId().value(),
                recipe.getCategoryId(),
                recipe.getDifficultyId(),
                recipe.getTitle(),
                recipe.getDescription(),
                recipe.getPreparationTimeMinutes(),
                recipe.getServings(),
                recipe.getIngredients().stream()
                        .map(this::mapToIngredientResponse)
                        .toList()
        );
    }

    /**
     * Mapea un componente de la receta a su representación inmutable de salida.
     */
    private RecipeIngredientResponse mapToIngredientResponse(RecipeIngredient ri) {
        return new RecipeIngredientResponse(
                ri.getIngredientId(),
                ri.getUnitId(),
                ri.getQuantity(),
                ri.getIngredientName(),
                ri.getUnitAbbreviation()
        );
    }
}