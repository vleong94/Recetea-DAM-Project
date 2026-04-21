package com.recetea;

import com.recetea.core.recipe.application.ports.out.category.ICategoryRepository;
import com.recetea.core.recipe.application.ports.out.difficulty.IDifficultyRepository;
import com.recetea.core.recipe.application.ports.out.ingredient.IIngredientRepository;
import com.recetea.core.recipe.application.ports.out.recipe.IRecipeRepository;
import com.recetea.core.recipe.application.ports.out.unit.IUnitRepository;
import com.recetea.core.recipe.application.usecases.category.GetAllCategoriesUseCase;
import com.recetea.core.recipe.application.usecases.difficulty.GetAllDifficultiesUseCase;
import com.recetea.core.recipe.application.usecases.ingredient.GetAllIngredientsUseCase;
import com.recetea.core.recipe.application.usecases.interop.ExportRecipeUseCase;
import com.recetea.core.recipe.application.usecases.interop.ImportRecipeUseCase;
import com.recetea.core.recipe.application.usecases.media.AttachMediaUseCase;
import com.recetea.core.recipe.application.usecases.recipe.*;
import com.recetea.core.recipe.application.usecases.unit.GetAllUnitsUseCase;
import com.recetea.core.social.application.usecases.GetUserFavoritesUseCase;
import com.recetea.core.social.application.usecases.IsFavoriteUseCase;
import com.recetea.core.social.application.usecases.ToggleFavoriteUseCase;
import com.recetea.core.user.application.usecases.LoginUseCase;
import com.recetea.core.user.application.usecases.RegisterUserUseCase;
import com.recetea.infrastructure.persistence.recipe.jdbc.JdbcTransactionManager;
import com.recetea.infrastructure.persistence.recipe.jdbc.config.DatabaseConfig;
import com.recetea.infrastructure.persistence.recipe.jdbc.repositories.*;
import com.recetea.infrastructure.persistence.social.jdbc.repositories.JdbcFavoriteRepository;
import com.recetea.infrastructure.persistence.user.jdbc.repositories.JdbcUserRepository;
import com.recetea.infrastructure.interop.xml.XmlInteropAdapter;
import com.recetea.infrastructure.storage.LocalFileSystemMediaStorage;
import com.recetea.infrastructure.storage.StorageConfig;
import com.recetea.infrastructure.security.PasswordHasher;
import com.recetea.infrastructure.security.SessionManager;
import com.recetea.infrastructure.ui.javafx.features.recipe.RecipeCommandContext;
import com.recetea.infrastructure.ui.javafx.features.recipe.RecipeCommandWrapper;
import com.recetea.infrastructure.ui.javafx.features.recipe.RecipeQueryContext;
import com.recetea.infrastructure.ui.javafx.features.recipe.RecipeQueryWrapper;
import com.recetea.infrastructure.ui.javafx.shared.error.GlobalExceptionHandler;
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
        GlobalExceptionHandler.register();

        // Inicialización del motor de persistencia y gestión de recursos JDBC
        // El Transaction Manager actúa como el Unit of Work central para la persistencia
        JdbcTransactionManager transactionManager = new JdbcTransactionManager(DatabaseConfig.getDataSource());

        // Los Outbound Adapters (Repositories) se vinculan al Transaction Manager para compartir conexiones
        IRecipeRepository recipeRepository = new JdbcRecipeRepository(transactionManager);
        IIngredientRepository ingredientRepository = new JdbcIngredientRepository(transactionManager);
        IUnitRepository unitRepository = new JdbcUnitRepository(transactionManager);
        ICategoryRepository categoryRepository = new JdbcCategoryRepository(transactionManager);
        IDifficultyRepository difficultyRepository = new JdbcDifficultyRepository(transactionManager);

        JdbcUserRepository userRepository = new JdbcUserRepository(transactionManager);
        JdbcFavoriteRepository favoriteRepository = new JdbcFavoriteRepository(transactionManager);
        PasswordHasher passwordHasher = new PasswordHasher();
        LoginUseCase loginUseCase = new LoginUseCase(userRepository, passwordHasher);
        RegisterUserUseCase registerUseCase = new RegisterUserUseCase(userRepository, passwordHasher, transactionManager);

        SessionManager sessionService = new SessionManager();
        LocalFileSystemMediaStorage mediaStorage = new LocalFileSystemMediaStorage(StorageConfig.getBasePath());
        XmlInteropAdapter xmlAdapter = new XmlInteropAdapter();

        RecipeQueryContext queryContext = new RecipeQueryContext(
                new GetAllRecipesUseCase(recipeRepository),
                new GetRecipeByIdUseCase(recipeRepository),
                new SearchRecipesUseCase(recipeRepository),
                new GetUserFavoritesUseCase(favoriteRepository, recipeRepository, sessionService)
        );

        RecipeCommandContext commandContext = new RecipeCommandContext(
                new AddRatingUseCase(recipeRepository, transactionManager, sessionService),
                new CreateRecipeUseCase(recipeRepository, categoryRepository, difficultyRepository, transactionManager, sessionService),
                new UpdateRecipeUseCase(recipeRepository, categoryRepository, difficultyRepository, transactionManager, sessionService),
                new DeleteRecipeUseCase(recipeRepository, transactionManager, sessionService),
                new AttachMediaUseCase(recipeRepository, mediaStorage, transactionManager, sessionService),
                new GetAllIngredientsUseCase(ingredientRepository),
                new GetAllUnitsUseCase(unitRepository),
                new GetAllCategoriesUseCase(categoryRepository),
                new GetAllDifficultiesUseCase(difficultyRepository),
                sessionService,
                new ToggleFavoriteUseCase(favoriteRepository, transactionManager, sessionService),
                new IsFavoriteUseCase(favoriteRepository, sessionService),
                new ImportRecipeUseCase(recipeRepository, categoryRepository, difficultyRepository,
                        ingredientRepository, unitRepository, transactionManager, sessionService, xmlAdapter),
                new ExportRecipeUseCase(recipeRepository, xmlAdapter)
        );

        RecipeQueryWrapper queryWrapper = new RecipeQueryWrapper(queryContext);
        RecipeCommandWrapper commandWrapper = new RecipeCommandWrapper(commandContext);

        // Inicialización del motor de enrutamiento y despliegue del entorno visual
        NavigationService nav = new NavigationService(primaryStage, queryWrapper, commandWrapper, loginUseCase, registerUseCase, sessionService);
        nav.toLogin();
    }

    /**
     * Lanzamiento del runtime de JavaFX para iniciar el ciclo de vida del proceso.
     */
    public static void main(String[] args) {
        launch(args);
    }
}