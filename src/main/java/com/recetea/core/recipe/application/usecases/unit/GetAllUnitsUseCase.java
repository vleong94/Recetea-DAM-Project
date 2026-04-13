package com.recetea.core.recipe.application.usecases.unit;

import com.recetea.core.recipe.application.ports.in.dto.UnitResponse;
import com.recetea.core.recipe.application.ports.in.unit.IGetAllUnitsUseCase;
import com.recetea.core.recipe.application.ports.out.unit.IUnitRepository;
import java.util.List;

/**
 * Implementa la lógica de aplicación para la obtención del catálogo de unidades de medida.
 * Orquesta la recuperación de datos desde el repositorio y su transformación a un
 * formato de salida inmutable (DTO), asegurando el aislamiento estricto del Domain.
 */
public class GetAllUnitsUseCase implements IGetAllUnitsUseCase {

    private final IUnitRepository repository;

    /**
     * Inicializa el caso de uso mediante inyección de dependencias.
     * Depende de la abstracción del puerto de salida, manteniendo la infraestructura
     * desacoplada de la lógica de aplicación.
     * * @param repository Contrato de salida para el acceso a datos de unidades.
     */
    public GetAllUnitsUseCase(IUnitRepository repository) {
        this.repository = repository;
    }

    /**
     * Ejecuta la consulta general de unidades de medida.
     * Transforma las entidades puras del dominio en objetos de transferencia de datos
     * para evitar la exposición del modelo interno hacia la interfaz de usuario.
     * * @return Colección inmutable de registros UnitResponse.
     */
    @Override
    public List<UnitResponse> execute() {
        return repository.findAll().stream()
                .map(unit -> new UnitResponse(
                        unit.getId(),
                        unit.getName()
                ))
                .toList();
    }
}