package com.recetea;

import com.recetea.core.recipe.application.ports.out.category.ICategoryRepository;
import com.recetea.core.recipe.application.ports.out.difficulty.IDifficultyRepository;
import com.recetea.core.recipe.application.ports.out.ingredient.IIngredientRepository;
import com.recetea.core.recipe.application.ports.out.interop.IRecipeInteropPort;
import com.recetea.core.recipe.application.ports.out.recipe.IRecipeRepository;
import com.recetea.core.recipe.application.ports.out.unit.IUnitRepository;
import com.recetea.core.recipe.application.usecases.category.GetAllCategoriesUseCase;
import com.recetea.core.recipe.application.usecases.difficulty.GetAllDifficultiesUseCase;
import com.recetea.core.recipe.application.usecases.ingredient.GetAllIngredientsUseCase;
import com.recetea.core.recipe.application.usecases.interop.ExportRecipeUseCase;
import com.recetea.core.recipe.application.usecases.interop.ImportRecipeUseCase;
import com.recetea.core.recipe.application.usecases.media.AttachMediaUseCase;
import com.recetea.core.recipe.application.usecases.recipe.*;
import com.recetea.core.recipe.application.usecases.report.GenerateGlobalInventoryReportUseCase;
import com.recetea.core.recipe.application.usecases.report.GenerateRecipeTechnicalSheetUseCase;
import com.recetea.core.recipe.application.usecases.unit.GetAllUnitsUseCase;
import com.recetea.core.social.application.usecases.GetUserFavoritesUseCase;
import com.recetea.core.social.application.usecases.IsFavoriteUseCase;
import com.recetea.core.social.application.usecases.ToggleFavoriteUseCase;
import com.recetea.core.user.application.usecases.LoginUseCase;
import com.recetea.core.user.application.usecases.RegisterUserUseCase;
import com.recetea.infrastructure.concurrency.ConcurrencyProvider;
import com.recetea.infrastructure.interop.xml.XmlInteropAdapter;
import com.recetea.infrastructure.persistence.recipe.jdbc.JdbcTransactionManager;
import com.recetea.infrastructure.persistence.recipe.jdbc.config.DatabaseConfig;
import com.recetea.infrastructure.persistence.recipe.jdbc.repositories.*;
import com.recetea.infrastructure.persistence.social.jdbc.repositories.JdbcFavoriteRepository;
import com.recetea.infrastructure.persistence.user.jdbc.repositories.JdbcUserRepository;
import com.recetea.infrastructure.reports.openpdf.OpenPdfRecipeAdapter;
import com.recetea.infrastructure.reports.openpdf.OpenPdfStatsAdapter;
import com.recetea.infrastructure.security.PasswordHasher;
import com.recetea.infrastructure.security.SessionManager;
import com.recetea.infrastructure.storage.LocalFileSystemMediaStorage;
import com.recetea.infrastructure.storage.StorageConfig;
import com.recetea.infrastructure.ui.javafx.features.recipe.RecipeCommandContext;
import com.recetea.infrastructure.ui.javafx.features.recipe.RecipeCommandWrapper;
import com.recetea.infrastructure.ui.javafx.features.recipe.RecipeQueryContext;
import com.recetea.infrastructure.ui.javafx.features.recipe.RecipeQueryWrapper;
import com.recetea.infrastructure.ui.javafx.shared.error.GlobalExceptionHandler;
import com.recetea.infrastructure.ui.javafx.shared.navigation.NavigationService;
import javafx.application.Application;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Composition root — wires the full dependency graph and hands off to JavaFX. */
public class Main extends Application {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    @Override
    public void start(Stage primaryStage) {
        log.info("Starting Recetea v1.0 (Java {})", System.getProperty("java.version"));
        GlobalExceptionHandler.register();

        // ----------------------------------------------------------------
        // BLOCK 1: Infrastructure & Configuration
        // ----------------------------------------------------------------
        JdbcTransactionManager      transactionManager  = new JdbcTransactionManager(DatabaseConfig.getDataSource());
        ConcurrencyProvider         concurrencyProvider = new ConcurrencyProvider();
        LocalFileSystemMediaStorage mediaStorage        = new LocalFileSystemMediaStorage(StorageConfig.getBasePath());
        PasswordHasher              passwordHasher      = new PasswordHasher();
        SessionManager              sessionService      = new SessionManager();

        // ----------------------------------------------------------------
        // BLOCK 2: Outbound Adapters (Repositories & Ports)
        // ----------------------------------------------------------------
        IRecipeRepository      recipeRepository     = new JdbcRecipeRepository(transactionManager);
        IIngredientRepository  ingredientRepository = new JdbcIngredientRepository(transactionManager);
        IUnitRepository        unitRepository       = new JdbcUnitRepository(transactionManager);
        ICategoryRepository    categoryRepository   = new JdbcCategoryRepository(transactionManager);
        IDifficultyRepository  difficultyRepository = new JdbcDifficultyRepository(transactionManager);
        JdbcUserRepository     userRepository       = new JdbcUserRepository(transactionManager);
        JdbcFavoriteRepository favoriteRepository   = new JdbcFavoriteRepository(transactionManager);

        IRecipeInteropPort   interopPort         = new XmlInteropAdapter();
        OpenPdfRecipeAdapter recipeReportAdapter = new OpenPdfRecipeAdapter();
        OpenPdfStatsAdapter  statsReportAdapter  = new OpenPdfStatsAdapter();

        // ----------------------------------------------------------------
        // BLOCK 3: Application Services (Use Cases)
        // ----------------------------------------------------------------

        // Catalogue
        GetAllCategoriesUseCase   getAllCategories   = new GetAllCategoriesUseCase(categoryRepository);
        GetAllDifficultiesUseCase getAllDifficulties = new GetAllDifficultiesUseCase(difficultyRepository);
        GetAllIngredientsUseCase  getAllIngredients  = new GetAllIngredientsUseCase(ingredientRepository);
        GetAllUnitsUseCase        getAllUnits        = new GetAllUnitsUseCase(unitRepository);

        // Recipe
        GetAllRecipesUseCase      getAllRecipes      = new GetAllRecipesUseCase(recipeRepository);
        GetRecipeByIdUseCase      getRecipeById      = new GetRecipeByIdUseCase(recipeRepository, sessionService, concurrencyProvider.executor());
        SearchRecipesUseCase      searchRecipes      = new SearchRecipesUseCase(recipeRepository);
        GetRecipesByAuthorUseCase getRecipesByAuthor = new GetRecipesByAuthorUseCase(recipeRepository);
        AddRatingUseCase          addRating          = new AddRatingUseCase(recipeRepository, transactionManager, sessionService);
        CreateRecipeUseCase       createRecipe       = new CreateRecipeUseCase(recipeRepository, categoryRepository, difficultyRepository, transactionManager, sessionService);
        UpdateRecipeUseCase       updateRecipe       = new UpdateRecipeUseCase(recipeRepository, categoryRepository, difficultyRepository, transactionManager, sessionService);
        DeleteRecipeUseCase       deleteRecipe       = new DeleteRecipeUseCase(recipeRepository, transactionManager, sessionService);
        AttachMediaUseCase        attachMedia        = new AttachMediaUseCase(recipeRepository, mediaStorage, transactionManager, sessionService);

        // Identity
        LoginUseCase        loginUseCase    = new LoginUseCase(userRepository, passwordHasher);
        RegisterUserUseCase registerUseCase = new RegisterUserUseCase(userRepository, passwordHasher, transactionManager);

        // Social
        GetUserFavoritesUseCase getUserFavorites = new GetUserFavoritesUseCase(favoriteRepository, recipeRepository, sessionService);
        ToggleFavoriteUseCase   toggleFavorite   = new ToggleFavoriteUseCase(favoriteRepository, transactionManager, sessionService);
        IsFavoriteUseCase       isFavorite       = new IsFavoriteUseCase(favoriteRepository, sessionService);

        // Interop
        ImportRecipeUseCase importRecipe = new ImportRecipeUseCase(recipeRepository, categoryRepository, difficultyRepository,
                ingredientRepository, unitRepository, transactionManager, sessionService, interopPort);
        ExportRecipeUseCase exportRecipe = new ExportRecipeUseCase(recipeRepository, interopPort);

        // Reports
        GenerateRecipeTechnicalSheetUseCase  generateTechnicalSheet  = new GenerateRecipeTechnicalSheetUseCase(recipeRepository, recipeReportAdapter);
        GenerateGlobalInventoryReportUseCase generateGlobalInventory = new GenerateGlobalInventoryReportUseCase(getUserFavorites, statsReportAdapter);

        // ----------------------------------------------------------------
        // BLOCK 4: UI Services & Navigation
        // ----------------------------------------------------------------
        RecipeQueryContext queryContext = new RecipeQueryContext(
                getAllRecipes, getRecipeById, searchRecipes, getUserFavorites, getRecipesByAuthor
        );
        RecipeCommandContext commandContext = new RecipeCommandContext(
                addRating, createRecipe, updateRecipe, deleteRecipe, attachMedia,
                getAllIngredients, getAllUnits, getAllCategories, getAllDifficulties,
                sessionService,
                toggleFavorite, isFavorite,
                importRecipe, exportRecipe,
                generateTechnicalSheet, generateGlobalInventory
        );

        RecipeQueryWrapper   queryWrapper   = new RecipeQueryWrapper(queryContext);
        RecipeCommandWrapper commandWrapper = new RecipeCommandWrapper(commandContext);

        NavigationService nav = new NavigationService(primaryStage, queryWrapper, commandWrapper,
                loginUseCase, registerUseCase, sessionService, concurrencyProvider.executor());
        nav.toLogin();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
