package com.recetea;

import com.recetea.core.recipe.application.ports.out.category.ICategoryRepository;
import com.recetea.core.recipe.application.ports.out.difficulty.IDifficultyRepository;
import com.recetea.core.recipe.application.ports.out.ingredient.IIngredientRepository;
import com.recetea.core.recipe.application.ports.out.recipe.IRecipeRepository;
import com.recetea.core.recipe.application.ports.out.unit.IUnitRepository;
import com.recetea.core.recipe.application.usecases.category.GetAllCategoriesUseCase;
import com.recetea.core.recipe.application.usecases.difficulty.GetAllDifficultiesUseCase;
import com.recetea.core.recipe.application.usecases.ingredient.GetAllIngredientsUseCase;
import com.recetea.core.recipe.application.usecases.recipe.*;
import com.recetea.core.recipe.application.usecases.unit.GetAllUnitsUseCase;
import com.recetea.core.shared.application.ports.in.IUserSessionService;
import com.recetea.infrastructure.persistence.recipe.jdbc.JdbcTransactionManager;
import com.recetea.infrastructure.persistence.recipe.jdbc.config.DatabaseConfig;
import com.recetea.infrastructure.persistence.recipe.jdbc.repositories.*;
import com.recetea.infrastructure.security.MockUserSessionService;
import com.recetea.infrastructure.ui.javafx.features.recipe.RecipeContext;
import com.recetea.infrastructure.ui.javafx.shared.navigation.NavigationService;
import javafx.application.Application;
import javafx.stage.Stage;

/**
 * Punto de entrada principal de la aplicación que actúa como Composition Root.
 * Se encarga de la orquestación del grafo de dependencias, inicializando los
 * componentes de infraestructura, servicios transversales y la lógica de aplicación
 * siguiendo los principios de Dependency Injection manual.
 */
public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        // Inicialización del motor de persistencia y gestión de recursos JDBC
        // El Transaction Manager actúa como el Unit of Work central para la persistencia
        JdbcTransactionManager transactionManager = new JdbcTransactionManager(DatabaseConfig.getDataSource());

        // Los Outbound Adapters (Repositories) se vinculan al Transaction Manager para compartir conexiones
        IRecipeRepository recipeRepository = new JdbcRecipeRepository(transactionManager);
        IIngredientRepository ingredientRepository = new JdbcIngredientRepository(transactionManager);
        IUnitRepository unitRepository = new JdbcUnitRepository(transactionManager);
        ICategoryRepository categoryRepository = new JdbcCategoryRepository(transactionManager);
        IDifficultyRepository difficultyRepository = new JdbcDifficultyRepository(transactionManager);

        // Inicialización del motor de seguridad y gestión de identidad (Mock placeholder)
        IUserSessionService sessionService = new MockUserSessionService();

        // Ensamblaje del RecipeContext inyectando los casos de uso y servicios compartidos
        RecipeContext context = new RecipeContext(
                new CreateRecipeUseCase(recipeRepository, categoryRepository, difficultyRepository, transactionManager),
                new GetAllRecipesUseCase(recipeRepository),
                new GetRecipeByIdUseCase(recipeRepository),
                new SearchRecipesUseCase(recipeRepository),
                new UpdateRecipeUseCase(recipeRepository, categoryRepository, difficultyRepository, transactionManager),
                new DeleteRecipeUseCase(recipeRepository, transactionManager),
                new GetAllIngredientsUseCase(ingredientRepository),
                new GetAllUnitsUseCase(unitRepository),
                new GetAllCategoriesUseCase(categoryRepository),
                new GetAllDifficultiesUseCase(difficultyRepository),
                sessionService
        );

        // Inicialización del motor de enrutamiento y despliegue del entorno visual
        NavigationService nav = new NavigationService(primaryStage, context, context);
        nav.toDashboard();
    }

    /**
     * Lanzamiento del runtime de JavaFX para iniciar el ciclo de vida del proceso.
     */
    public static void main(String[] args) {
        launch(args);
    }
}