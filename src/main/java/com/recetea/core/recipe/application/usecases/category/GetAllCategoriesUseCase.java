package com.recetea.core.recipe.application.usecases.category;

import com.recetea.core.recipe.application.ports.in.category.IGetAllCategoriesUseCase;
import com.recetea.core.recipe.application.ports.out.category.ICategoryRepository;
import com.recetea.core.recipe.domain.Category;

import java.util.List;

/**
 * Implementación del caso de uso encargado de recuperar el catálogo completo de categorías.
 * Actúa como orquestador en la capa de aplicación, interconectando el puerto de entrada
 * (solicitado por la interfaz de usuario) con el puerto de salida (persistencia).
 * Garantiza el flujo de lectura unidireccional manteniendo el aislamiento del dominio.
 */
public class GetAllCategoriesUseCase implements IGetAllCategoriesUseCase {

    private final ICategoryRepository repository;

    /**
     * Inicializa el componente mediante inyección de dependencias.
     * La dependencia exclusiva de la interfaz del repositorio (Outbound Port) asegura
     * el cumplimiento del principio de inversión de dependencias (DIP), manteniendo
     * el caso de uso agnóstico a la tecnología de base de datos subyacente.
     *
     * @param repository Contrato de persistencia para la entidad Category.
     */
    public GetAllCategoriesUseCase(ICategoryRepository repository) {
        this.repository = repository;
    }

    /**
     * Ejecuta el flujo de lectura para obtener la taxonomía de clasificaciones.
     * Delega la extracción física de los datos al adaptador de infraestructura y
     * retorna la colección de entidades de dominio íntegras.
     *
     * @return Lista inmutable de entidades Category.
     */
    @Override
    public List<Category> execute() {
        return repository.findAll();
    }
}