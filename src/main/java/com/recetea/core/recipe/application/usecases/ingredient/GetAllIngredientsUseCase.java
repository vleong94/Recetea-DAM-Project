package com.recetea.core.recipe.application.usecases.ingredient;

import com.recetea.core.recipe.application.ports.in.dto.IngredientResponse;
import com.recetea.core.recipe.application.ports.in.ingredient.IGetAllIngredientsUseCase;
import com.recetea.core.recipe.application.ports.out.ingredient.IIngredientRepository;
import java.util.List;

/**
 * Implementa la lógica de aplicación para la obtención del catálogo de ingredientes.
 * Coordina la recuperación de datos desde la infraestructura y su posterior
 * transformación a un formato de salida seguro y agnóstico.
 */
public class GetAllIngredientsUseCase implements IGetAllIngredientsUseCase {

    private final IIngredientRepository repository;

    /**
     * El constructor facilita la inyección de dependencias del repositorio.
     * Al depender de una interfaz (puerto de salida), el caso de uso se mantiene
     * aislado de la implementación técnica de la base de datos.
     */
    public GetAllIngredientsUseCase(IIngredientRepository repository) {
        this.repository = repository;
    }

    /**
     * Ejecuta el flujo de consulta y mapeo. Transforma la lista de entidades
     * de dominio obtenidas del repositorio en una lista de registros de
     * respuesta (DTOs) inmutables para su consumo externo.
     */
    @Override
    public List<IngredientResponse> execute() {
        return repository.findAll().stream()
                .map(ingredient -> new IngredientResponse(
                        ingredient.getId().value(),
                        ingredient.getName()
                ))
                .toList();
    }
}