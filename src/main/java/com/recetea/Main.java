package com.recetea;

import com.recetea.core.domain.Recipe;
import com.recetea.core.ports.IRecipeRepository;
import com.recetea.infrastructure.persistence.JdbcRecipeRepository;

public class Main {
    public static void main(String[] args) {
        IRecipeRepository repository = new JdbcRecipeRepository();

        try {
            System.out.println("Instanciando receta relacional en memoria...");
            // IMPORTANTE: Asegúrate de que los IDs 1 (User, Category, Difficulty) existen en tu DB.
            Recipe miReceta = new Recipe(
                    1, // user_id
                    1, // category_id
                    1, // difficulty_id
                    "Macarrones con Tomate",
                    "Receta básica de supervivencia",
                    20, // prep_time_min
                    2   // servings
            );

            System.out.println("Enviando a PostgreSQL...");
            repository.save(miReceta);

            // Si funciona, miReceta.getId() ya no será NULL. PostgreSQL lo habrá inyectado.
            System.out.println("¡ÉXITO ABSOLUTO! PostgreSQL ha guardado la receta y asignado el ID: " + miReceta.getId());

        } catch (Exception e) {
            System.err.println("FRACASO EN LA INSERCIÓN:");
            e.printStackTrace();
        }
    }
}