package com.recetea.core.recipe.application.ports.in.unit;

import com.recetea.core.recipe.application.ports.in.dto.UnitResponse;
import java.util.List;

/**
 * Define el contrato de entrada (Inbound Port) para el caso de uso encargado
 * de recuperar el catálogo completo de unidades de medida.
 * Al estar situado en la capa Application, esta interfaz establece un límite
 * arquitectónico estricto. Obliga a que la comunicación con clientes externos
 * (como controladores o interfaces de usuario) se realice exclusivamente a
 * través de objetos de transferencia de datos (DTOs), protegiendo así las
 * entidades del Domain de cualquier acoplamiento o manipulación externa.
 */
public interface IGetAllUnitsUseCase {

    /**
     * Ejecuta la operación de consulta para obtener todas las unidades de medida
     * disponibles delegando la extracción a la infraestructura subyacente.
     *
     * @return Una lista inmutable de objetos UnitResponse. Retorna una colección
     * vacía si no existen registros, garantizando la seguridad contra valores nulos
     * en la capa de consumo.
     */
    List<UnitResponse> execute();
}