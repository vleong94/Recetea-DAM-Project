package com.recetea.core.recipe.application.ports.in.ingredient;

import com.recetea.core.recipe.application.ports.in.dto.IngredientResponse;
import java.util.List;

/**
 * Define el contrato de entrada (Inbound Port) para el Use Case encargado
 * de recuperar el catálogo completo de ingredientes.
 * Al residir en la capa Application, esta interfaz establece una frontera de
 * aislamiento estricta: los clientes externos interactúan con el sistema
 * utilizando exclusivamente Data Transfer Objects (DTOs), protegiendo las
 * entidades del Domain de cualquier manipulación o acoplamiento externo.
 */
public interface IGetAllIngredientsUseCase {

    /**
     * Ejecuta la operación de consulta en el origen de datos subyacente.
     *
     * @return Una colección inmutable de objetos IngredientResponse. Retorna
     * una lista vacía si no existen registros, evitando el manejo de valores null.
     */
    List<IngredientResponse> execute();
}