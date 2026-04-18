package com.recetea.core.recipe.application.ports.in.category;

import com.recetea.core.recipe.domain.Category;
import java.util.List;

/**
 * Puerto de entrada (Inbound Port) que define el contrato para la recuperación del catálogo global de categorías.
 * Esta interfaz permite que las capas externas, como la interfaz de usuario, soliciten la lista completa
 * de clasificaciones disponibles en el dominio para asegurar que las recetas se asocien a una taxonomía válida.
 */
public interface IGetAllCategoriesUseCase {

    /**
     * Ejecuta la lógica de negocio para obtener la totalidad de las categorías registradas.
     * La implementación debe garantizar el retorno de objetos de dominio íntegros que posean
     * tanto su identidad única como su descriptor nominal.
     *
     * @return Una lista de entidades Category. Retorna una lista vacía si no hay datos persistidos.
     */
    List<Category> execute();
}