package com.recetea.core.recipe.application.ports.in.recipe;

import com.recetea.core.recipe.application.ports.in.dto.RecipeSummaryResponse;
import java.util.List;

/**
 * Define el contrato de entrada (Inbound Port) para el caso de uso responsable
 * de recuperar el catálogo completo de recetas.
 * Ubicada en la capa Application, esta interfaz establece una frontera de
 * aislamiento estricta. Obliga a que la comunicación con los clientes externos
 * (como la interfaz de usuario o controladores) se realice exclusivamente mediante
 * objetos de transferencia de datos (DTOs) optimizados para listados, protegiendo
 * la integridad de las entidades del Domain.
 */
public interface IGetAllRecipesUseCase {

    /**
     * Ejecuta la consulta general para extraer todas las recetas disponibles
     * delegando la operación a la infraestructura de persistencia.
     *
     * @return Una colección inmutable de objetos RecipeSummaryResponse. Retorna
     * una lista vacía si no existen registros, evitando la propagación de referencias
     * nulas y garantizando un consumo seguro en la capa de presentación.
     */
    List<RecipeSummaryResponse> execute();
}