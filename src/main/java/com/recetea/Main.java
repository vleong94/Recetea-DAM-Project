package com.recetea;

import com.recetea.core.domain.Recipe;
import com.recetea.core.ports.IRecipeRepository;
import com.recetea.core.ports.in.IGetAllRecipesUseCase;
import com.recetea.core.usecases.GetAllRecipesUseCase;
import com.recetea.infrastructure.persistence.JdbcRecipeRepository;

import java.util.List;

/**
 * Composition Root (Diagnostic Mode):
 * Ensambla la capa de lectura y escupe el catálogo de PostgreSQL a la consola.
 */
public class Main {
    public static void main(String[] args) {
        System.out.println("--- INICIANDO DIAGNÓSTICO DE LECTURA (READ OPERATION) ---");

        // 1. WIRING (Inyección manual de dependencias)
        IRecipeRepository repository = new JdbcRecipeRepository();
        IGetAllRecipesUseCase getAllRecipesUseCase = new GetAllRecipesUseCase(repository);

        try {
            System.out.println("Ejecutando Query contra PostgreSQL...\n");

            // 2. FETCH (Delegación al Use Case)
            List<Recipe> catalog = getAllRecipesUseCase.execute();

            // 3. RENDERIZADO EN CONSOLA
            if (catalog.isEmpty()) {
                System.out.println("ADVERTENCIA: La base de datos está vacía. No hay recetas que mostrar.");
            } else {
                System.out.println("CATÁLOGO EXTRAÍDO CON ÉXITO. Total de registros: " + catalog.size());
                System.out.println("---------------------------------------------------");
                for (Recipe recipe : catalog) {
                    System.out.printf("ID: %-3d | Título: %-30s | Prep: %-3d min | Raciones: %d%n",
                            recipe.getId(),
                            recipe.getTitle(),
                            recipe.getPreparationTimeMinutes(),
                            recipe.getServings()
                    );
                }
                System.out.println("---------------------------------------------------");
            }

        } catch (Exception e) {
            System.err.println("CRASH CRÍTICO DURANTE LA EXTRACCIÓN DE DATOS:");
            e.printStackTrace();
        }
    }
}