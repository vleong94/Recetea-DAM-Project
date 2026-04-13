package com.recetea.core.recipe.application.usecases.recipe;

import com.recetea.core.recipe.application.ports.in.dto.RecipeDetailResponse;
import com.recetea.core.recipe.application.ports.in.dto.RecipeIngredientResponse;
import com.recetea.core.recipe.application.ports.in.recipe.IGetRecipeByIdUseCase;
import com.recetea.core.recipe.application.ports.out.recipe.IRecipeRepository;
import com.recetea.core.recipe.domain.Recipe;
import com.recetea.core.recipe.domain.RecipeIngredient;

import java.util.Optional;

/**
 * Orquestador que implementa la lógica de recuperación de la información detallada de una Recipe.
 * Extrae el Aggregate Root desde la capa de Infrastructure y transforma la entidad
 * a un Inbound DTO seguro, garantizando que el Domain Model no se exponga a la UI.
 */
public class GetRecipeByIdUseCase implements IGetRecipeByIdUseCase {

    private final IRecipeRepository repository;

    /**
     * Inicializa el Use Case mediante Dependency Injection.
     * Se acopla exclusivamente al Outbound Port para mantener el agnosticismo del Database.
     *
     * @param repository Contrato de salida para el acceso al Data Store de recetas.
     */
    public GetRecipeByIdUseCase(IRecipeRepository repository) {
        this.repository = repository;
    }

    /**
     * Ejecuta el Query por identificador y coordina el Mapping jerárquico de los datos.
     *
     * @param recipeId Identificador único del registro.
     * @return Un Optional que contiene el RecipeDetailResponse si el Entity existe.
     */
    @Override
    public Optional<RecipeDetailResponse> execute(int recipeId) {
        return repository.findById(recipeId)
                .map(this::mapToResponse);
    }

    /**
     * Realiza el Deep Mapping del Aggregate Root y de su colección interna.
     * Transforma la jerarquía del Domain en un Data Transfer Object inmutable.
     */
    private RecipeDetailResponse mapToResponse(Recipe recipe) {
        return new RecipeDetailResponse(
                recipe.getId(),
                recipe.getUserId(),
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
     * Transforma un Value Object individual a su representación DTO de solo lectura.
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