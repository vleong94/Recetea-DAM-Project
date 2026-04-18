package com.recetea.core.recipe.application.usecases.difficulty;

import com.recetea.core.recipe.application.ports.in.difficulty.IGetAllDifficultiesUseCase;
import com.recetea.core.recipe.application.ports.out.difficulty.IDifficultyRepository;
import com.recetea.core.recipe.domain.Difficulty;

import java.util.List;

/**
 * Implementación del Use Case responsable de recuperar el catálogo completo de niveles de dificultad.
 * Actúa como orquestador en la capa de Application, interconectando el Inbound Port
 * (solicitado por la UI) con el Outbound Port (persistencia).
 * Garantiza un flujo de lectura unidireccional, manteniendo el aislamiento estricto del Domain.
 */
public class GetAllDifficultiesUseCase implements IGetAllDifficultiesUseCase {

    private final IDifficultyRepository repository;

    /**
     * Inicializa el componente mediante Dependency Injection.
     * La dependencia exclusiva de la interface del repositorio asegura el cumplimiento
     * del Dependency Inversion Principle (DIP), aislando la lógica de negocio
     * de los detalles de implementación de la Infrastructure.
     *
     * @param repository Contrato de persistencia para la Entity Difficulty.
     */
    public GetAllDifficultiesUseCase(IDifficultyRepository repository) {
        this.repository = repository;
    }

    /**
     * Ejecuta el flujo de lectura para obtener la taxonomía de complejidades.
     * Delega la extracción física de los datos al adaptador de persistencia y
     * retorna la collection de Entities de dominio íntegras.
     *
     * @return List inmutable de Entities Difficulty.
     */
    @Override
    public List<Difficulty> execute() {
        return repository.findAll();
    }
}