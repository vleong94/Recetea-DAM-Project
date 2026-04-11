package com.recetea;

import com.recetea.core.domain.Recipe;
import com.recetea.core.domain.RecipeIngredient;
import com.recetea.core.ports.IRecipeRepository;
import com.recetea.infrastructure.persistence.JdbcRecipeRepository;

public class Main {
    public static void main(String[] args) {
        IRecipeRepository repository = new JdbcRecipeRepository();

        try {
            System.out.println("Instanciando receta transaccional...");

            Recipe miReceta = new Recipe(
                    1, 1, 1,
                    "Tarta de Queso Absoluta",
                    "Prueba de estrés transaccional con ingredientes.",
                    50, 4
            );

            // Inyectamos ingredientes (Asegúrate de que estos IDs existen en tu DB)
            // 1=Queso Crema, 1=Gramos
            miReceta.addIngredient(new RecipeIngredient(1, 1, 500.0));
            // 2=Azúcar Blanco, 1=Gramos
            miReceta.addIngredient(new RecipeIngredient(2, 1, 150.0));

            System.out.println("Disparando transacción a PostgreSQL...");
            repository.save(miReceta);

            System.out.println("¡SISTEMA ACID VERIFICADO! Receta guardada con ID: " + miReceta.getId() + " y sus ingredientes.");

        } catch (Exception e) {
            System.err.println("FRACASO:");
            e.printStackTrace();
        }
    }
}