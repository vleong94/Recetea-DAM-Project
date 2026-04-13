package com.recetea;

import com.recetea.core.recipe.application.ports.out.recipe.IRecipeRepository;
import com.recetea.core.recipe.application.ports.out.ingredient.IIngredientRepository;
import com.recetea.core.recipe.application.ports.out.unit.IUnitRepository;

import com.recetea.core.recipe.application.usecases.recipe.*;
import com.recetea.core.recipe.application.usecases.ingredient.GetAllIngredientsUseCase;
import com.recetea.core.recipe.application.usecases.unit.GetAllUnitsUseCase;

import com.recetea.infrastructure.persistence.recipe.jdbc.config.DatabaseConfig;
import com.recetea.infrastructure.persistence.recipe.jdbc.repositories.JdbcRecipeRepository;
import com.recetea.infrastructure.persistence.recipe.jdbc.repositories.JdbcIngredientRepository;
import com.recetea.infrastructure.persistence.recipe.jdbc.repositories.JdbcUnitRepository;

import com.recetea.infrastructure.ui.javafx.shared.NavigationService;
import com.recetea.infrastructure.ui.javafx.recipe.RecipeContext;

import javafx.application.Application;
import javafx.stage.Stage;

import javax.sql.DataSource;

/**
 * Composition Root del sistema.
 * Orquesta el ensamblaje de todas las capas arquitectónicas centralizando la inicialización.
 * Ejecuta la Dependency Injection manual, conectando los adaptadores de infraestructura
 * con los Use Cases del Core, garantizando el desacoplamiento de la aplicación.
 */
public class Main extends Application {

    /**
     * Entry point del ciclo de vida de JavaFX.
     * Coordina la creación del DataSource, los Repositories, el Context Object y el Routing.
     */
    @Override
    public void start(Stage primaryStage) {
        // Inicialización del Singleton de conexión a la base de datos.
        DataSource dataSource = DatabaseConfig.getDataSource();

        // Instanciación de los adaptadores de la Persistence Layer.
        IRecipeRepository recipeRepository = new JdbcRecipeRepository(dataSource);
        IIngredientRepository ingredientRepository = new JdbcIngredientRepository(dataSource);
        IUnitRepository unitRepository = new JdbcUnitRepository(dataSource);

        // Ensamblaje de la Application Layer inyectando los puertos de salida.
        RecipeContext context = new RecipeContext(
                new CreateRecipeUseCase(recipeRepository),
                new GetAllRecipesUseCase(recipeRepository),
                new GetRecipeByIdUseCase(recipeRepository),
                new UpdateRecipeUseCase(recipeRepository),
                new DeleteRecipeUseCase(recipeRepository),
                new GetAllIngredientsUseCase(ingredientRepository),
                new GetAllUnitsUseCase(unitRepository)
        );

        // Inicialización del motor de Routing inyectando el State y el Stage principal.
        NavigationService nav = new NavigationService(primaryStage, context);

        // Lanzamiento de la vista inicial.
        nav.toDashboard();
    }

    public static void main(String[] args) {
        launch(args);
    }
}