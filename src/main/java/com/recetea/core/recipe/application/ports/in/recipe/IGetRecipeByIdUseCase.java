package com.recetea.core.recipe.application.ports.in.recipe;

import com.recetea.core.recipe.application.ports.in.dto.RecipeDetailResponse;
import java.util.Optional;

/**
 * Define el contrato de entrada (Inbound Port) para el caso de uso encargado
 * de recuperar la información detallada de una receta específica.
 * Ubicada en la capa Application, esta interfaz establece una frontera arquitectónica
 * que garantiza el aislamiento del Domain. Los clientes externos consumen los datos
 * a través de un objeto de transferencia (DTO) que consolida toda la información
 * jerárquica de la receta (incluyendo sus ingredientes), evitando cualquier
 * exposición de la lógica de negocio subyacente.
 */
public interface IGetRecipeByIdUseCase {

    /**
     * Ejecuta la consulta para localizar y reconstruir la vista detallada de
     * una receta utilizando su identificador único.
     *
     * @param recipeId El identificador numérico de la receta a consultar.
     * @return Un Optional que contiene el RecipeDetailResponse si la receta existe.
     * El uso de Optional obliga al consumidor a manejar explícitamente el caso
     * en el que el recurso solicitado no se encuentre, previniendo NullPointerExceptions.
     */
    Optional<RecipeDetailResponse> execute(int recipeId);
}