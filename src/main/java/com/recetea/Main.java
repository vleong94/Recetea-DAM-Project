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
import com.recetea.infrastructure.ui.javafx.shared.navigation.NavigationService;
import com.recetea.infrastructure.ui.javafx.features.recipe.RecipeContext;

import javafx.application.Application;
import javafx.stage.Stage;
import javax.sql.DataSource;

/**
 * Punto de entrada de la aplicación y Composition Root del sistema.
 * Asume la responsabilidad exclusiva de instanciar y ensamblar el grafo de dependencias,
 * conectando los adaptadores de infraestructura con los casos de uso del Core,
 * garantizando la inyección de dependencias manual y el aislamiento arquitectónico.
 */
public class Main extends Application {

    /**
     * Orquesta el ciclo de inicialización del sistema.
     * Configura las conexiones de base de datos, instancia los adaptadores de salida (Repositorios),
     * los inyecta en la capa de aplicación y delega el control del hilo principal al motor de navegación.
     */
    @Override
    public void start(Stage primaryStage) {

        // Fase 1: Resolución de infraestructura de base de datos
        DataSource dataSource = DatabaseConfig.getDataSource();

        // Fase 2: Instanciación de Outbound Adapters (Persistencia)
        IRecipeRepository recipeRepository = new JdbcRecipeRepository(dataSource);
        IIngredientRepository ingredientRepository = new JdbcIngredientRepository(dataSource);
        IUnitRepository unitRepository = new JdbcUnitRepository(dataSource);

        // Fase 3: Ensamblaje del Application Core y encapsulación en el Contexto
        RecipeContext context = new RecipeContext(
                new CreateRecipeUseCase(recipeRepository),
                new GetAllRecipesUseCase(recipeRepository),
                new GetRecipeByIdUseCase(recipeRepository),
                new UpdateRecipeUseCase(recipeRepository),
                new DeleteRecipeUseCase(recipeRepository),
                new GetAllIngredientsUseCase(ingredientRepository),
                new GetAllUnitsUseCase(unitRepository)
        );

        // Fase 4: Inicialización del motor de enrutamiento (UI Boundary)
        NavigationService nav = new NavigationService(primaryStage, context);
        nav.toDashboard();
    }

    /**
     * Método de arranque nativo.
     * Transfiere el control de ejecución al framework JavaFX.
     */
    public static void main(String[] args) {
        launch(args);
    }
}