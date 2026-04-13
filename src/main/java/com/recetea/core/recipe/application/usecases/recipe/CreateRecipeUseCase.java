package com.recetea.core.recipe.application.usecases.recipe;

import com.recetea.core.recipe.application.ports.in.dto.SaveRecipeRequest;
import com.recetea.core.recipe.application.ports.in.recipe.ICreateRecipeUseCase;
import com.recetea.core.recipe.application.ports.out.recipe.IRecipeRepository;
import com.recetea.core.recipe.domain.Recipe;
import com.recetea.core.recipe.domain.RecipeIngredient;

/**
 * Orquestador que implementa la lógica de creación de nuevas recetas.
 * Transforma el Inbound DTO en entidades del Domain, asegurando que el Aggregate Root
 * (Recipe + sus RecipeIngredients) se construya con integridad estructural antes
 * de delegar el proceso de persistencia a la capa de Infrastructure.
 */
public class CreateRecipeUseCase implements ICreateRecipeUseCase {

    private final IRecipeRepository repository;

    /**
     * Inicializa el Use Case mediante Dependency Injection.
     * Al acoplarse exclusivamente al Outbound Port, garantiza el agnosticismo
     * tecnológico y facilita el Unit Testing aislado.
     *
     * @param repository Contrato de salida para la persistencia del Aggregate Root.
     */
    public CreateRecipeUseCase(IRecipeRepository repository) {
        this.repository = repository;
    }

    /**
     * Desempaqueta el Request, construye la entidad Recipe, hidrata sus dependencias
     * y delega la Transaction al Repository.
     *
     * @param request Payload inmutable con los datos de entrada validados.
     * @return El Primary Key numérico generado por el Data Store.
     */
    @Override
    public int execute(SaveRecipeRequest request) {

        // 1. Instanciación del Aggregate Root
        // Se construye la entidad principal utilizando exclusivamente atributos de negocio.
        Recipe recipe = new Recipe(
                request.userId(),
                request.categoryId(),
                request.difficultyId(),
                request.title(),
                request.description(),
                request.preparationTimeMinutes(),
                request.servings()
        );

        // 2. Composición de los Value Objects
        // Se hidrata la colección interna transfiriendo todos los parámetros requeridos
        // por el constructor de RecipeIngredient, incluyendo los descriptores nominales.
        if (request.ingredients() != null) {
            for (SaveRecipeRequest.IngredientRequest ir : request.ingredients()) {
                recipe.addIngredient(new RecipeIngredient(
                        ir.ingredientId(),
                        ir.unitId(),
                        ir.quantity(),
                        ir.ingredientName(),
                        ir.unitName()
                ));
            }
        }

        // 3. Delegación a la Infrastructure
        // El Repository asume el Transaction Scope para insertar la Recipe y sus dependencias.
        repository.save(recipe);

        // 4. Verificación de Primary Key
        // Fail-safe para garantizar que el Data Store asignó un identificador válido post-commit.
        if (recipe.getId() == null || recipe.getId() <= 0) {
            throw new IllegalStateException("Integrity Error: El Data Store no asignó un Primary Key válido a la entidad tras el commit.");
        }

        return recipe.getId();
    }
}