package com.recetea;

import com.recetea.core.recipe.application.ports.out.category.ICategoryRepository;
import com.recetea.core.recipe.application.ports.out.difficulty.IDifficultyRepository;
import com.recetea.core.recipe.application.ports.out.ingredient.IIngredientRepository;
import com.recetea.core.recipe.application.ports.out.interop.IRecipeInteropPort;
import com.recetea.core.recipe.application.ports.out.media.IMediaStorageService;
import com.recetea.core.recipe.application.ports.out.recipe.IRecipeRepository;
import com.recetea.core.recipe.application.ports.out.report.IRecipeReportPort;
import com.recetea.core.recipe.application.ports.out.unit.IUnitRepository;
import com.recetea.core.recipe.application.usecases.category.GetAllCategoriesUseCase;
import com.recetea.core.recipe.application.usecases.difficulty.GetAllDifficultiesUseCase;
import com.recetea.core.recipe.application.usecases.ingredient.GetAllIngredientsUseCase;
import com.recetea.core.recipe.application.usecases.interop.ExportRecipeUseCase;
import com.recetea.core.recipe.application.usecases.interop.ImportRecipeUseCase;
import com.recetea.core.recipe.application.usecases.media.AttachMediaUseCase;
import com.recetea.core.recipe.application.usecases.recipe.*;
import com.recetea.core.recipe.application.usecases.report.GenerateRecipeTechnicalSheetUseCase;
import com.recetea.core.recipe.application.usecases.unit.GetAllUnitsUseCase;
import com.recetea.core.shared.application.ConcurrencyGuard;
import com.recetea.core.shared.application.ports.out.IMetricsPort;
import com.recetea.core.shared.domain.utils.MaskingUtils;
import com.recetea.core.social.application.usecases.GetUserFavoritesUseCase;
import com.recetea.core.social.application.usecases.IsFavoriteUseCase;
import com.recetea.core.social.application.usecases.ToggleFavoriteUseCase;
import com.recetea.core.user.application.ports.out.IPasswordEncoder;
import com.recetea.core.user.application.usecases.LoginUseCase;
import com.recetea.core.user.application.usecases.RegisterUserUseCase;
import com.recetea.infrastructure.concurrency.ConcurrencyProvider;
import com.recetea.infrastructure.interop.xml.XmlInteropAdapter;
import com.recetea.infrastructure.metrics.LogMetricsAdapter;
import com.recetea.infrastructure.persistence.recipe.jdbc.JdbcTransactionManager;
import com.recetea.infrastructure.persistence.recipe.jdbc.config.AppConfig;
import com.recetea.infrastructure.persistence.recipe.jdbc.config.DatabaseConfig;
import com.recetea.infrastructure.persistence.recipe.jdbc.repositories.*;
import com.recetea.infrastructure.persistence.social.jdbc.repositories.JdbcFavoriteRepository;
import com.recetea.infrastructure.persistence.user.jdbc.repositories.JdbcUserRepository;
import com.recetea.infrastructure.security.SessionManager;
import com.recetea.infrastructure.storage.MediaStorageFactory;
import com.recetea.infrastructure.ui.javafx.features.recipe.RecipeCommandContext;
import com.recetea.infrastructure.ui.javafx.features.recipe.RecipeCommandWrapper;
import com.recetea.infrastructure.ui.javafx.features.recipe.RecipeQueryContext;
import com.recetea.infrastructure.ui.javafx.features.recipe.RecipeQueryWrapper;
import atlantafx.base.theme.PrimerLight;
import com.recetea.infrastructure.ui.javafx.shared.error.GlobalExceptionHandler;
import com.recetea.infrastructure.ui.javafx.shared.navigation.NavigationService;
import javafx.application.Application;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ServiceLoader;

/**
 * Composition root — wires the full dependency graph and hands off to JavaFX.
 *
 * <h2>How to run</h2>
 * <pre>
 * mvn clean javafx:run
 * </pre>
 * <p>The Java 24 preview flag is the only JVM argument the launcher needs;
 * it is already plumbed through {@code pom.xml}'s {@code maven-compiler-plugin}
 * and {@code javafx-maven-plugin}, so the IntelliJ Run Configuration only needs
 * {@code --enable-preview} in its "VM options" field if you launch
 * {@code Main.main()} directly without going through Maven.
 */
public class Main extends Application {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    @Override
    public void start(Stage primaryStage) {
        log.info("Starting Recetea v1.0 (Java {})", System.getProperty("java.version"));
        GlobalExceptionHandler.register();

        // Global theme: AtlantaFX's user-agent stylesheet replaces the default Modena.
        // Scene-level stylesheets (NavigationService loads app.css per scene) have higher
        // CSS specificity, so the project's design tokens still win where they overlap.
        Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());

        // Stage sizing: enforce a usable minimum and pin the "restored" size the OS
        // returns to when the user un-maximises. Setting these before the first show()
        // lets JavaFX snapshot 1280x800 as the restored bounds; without it, the
        // restored dimensions collapse to the scene's preferred size (often unusable).
        primaryStage.setMinWidth(1024);
        primaryStage.setMinHeight(768);
        primaryStage.setWidth(1280);
        primaryStage.setHeight(800);
        primaryStage.setResizable(true);

        // ----------------------------------------------------------------
        // BLOCK 0: Configuration — loaded once, passed explicitly downstream.
        // ----------------------------------------------------------------
        AppConfig appConfig = AppConfig.load();
        log.info("Configuration loaded: env={} url={}",
                System.getProperty("env", "local"),
                MaskingUtils.maskJdbcUrl(appConfig.dbUrl()));

        // ----------------------------------------------------------------
        // BLOCK 1: Infrastructure & Configuration
        // ----------------------------------------------------------------
        JdbcTransactionManager transactionManager  = new JdbcTransactionManager(DatabaseConfig.getDataSource(appConfig));
        ConcurrencyProvider    concurrencyProvider = new ConcurrencyProvider();
        SessionManager         sessionService      = new SessionManager();

        // SPI-loaded adapters: each declared via `provides ... with ...` in module-info.java.
        // Missing providers fail fast at startup with a descriptive IllegalStateException.
        IPasswordEncoder     passwordHasher       = loadService(IPasswordEncoder.class);
        IRecipeReportPort    recipeReportAdapter  = loadService(IRecipeReportPort.class);

        // Media storage is wired explicitly (not via SPI) because the cloud adapter
        // needs the public-read URL + auth token at construction time. Factory routes
        // on the shape of STORAGE_BASE_PATH: http* → Supabase, else local filesystem.
        // resolveBasePath() materialises the effective storage root once here at the
        // composition root; the same String is handed to NavigationService below so
        // every UI consumer (dashboard cards, detail gallery, upload component) sees
        // the same value the storage adapter is writing/reading from.
        String               storageBasePath      = MediaStorageFactory.resolveBasePath(appConfig);
        IMediaStorageService mediaStorage         = MediaStorageFactory.create(appConfig);

        // Metrics: log slow queries (>500 ms) and emit DEBUG-level instrumentation.
        // Threaded explicitly into every repository + gateway constructor — no static
        // singleton — so dependency wiring is fully traceable from this composition root.
        IMetricsPort metricsPort = new LogMetricsAdapter();

        // Cap concurrent heavy DB writes at 80% of the HikariCP pool size (10 → 8 permits)
        // to keep at least 2 connections available for read paths and avoid pool timeouts.
        ConcurrencyGuard heavyIoGuard = new ConcurrencyGuard(8);

        // ----------------------------------------------------------------
        // BLOCK 2: Outbound Adapters (Repositories & Ports)
        // ----------------------------------------------------------------
        IRecipeRepository      recipeRepository     = new JdbcRecipeRepository(transactionManager, metricsPort);
        IIngredientRepository  ingredientRepository = new JdbcIngredientRepository(transactionManager, metricsPort);
        IUnitRepository        unitRepository       = new JdbcUnitRepository(transactionManager, metricsPort);
        ICategoryRepository    categoryRepository   = new JdbcCategoryRepository(transactionManager, metricsPort);
        IDifficultyRepository  difficultyRepository = new JdbcDifficultyRepository(transactionManager, metricsPort);
        JdbcUserRepository     userRepository       = new JdbcUserRepository(transactionManager, metricsPort);
        JdbcFavoriteRepository favoriteRepository   = new JdbcFavoriteRepository(transactionManager, metricsPort);

        IRecipeInteropPort interopPort = new XmlInteropAdapter(
                categoryRepository, difficultyRepository, ingredientRepository, unitRepository, metricsPort);

        // ----------------------------------------------------------------
        // BLOCK 3: Application Services (Use Cases)
        // ----------------------------------------------------------------

        // Catalogue
        GetAllCategoriesUseCase   getAllCategories   = new GetAllCategoriesUseCase(categoryRepository);
        GetAllDifficultiesUseCase getAllDifficulties = new GetAllDifficultiesUseCase(difficultyRepository);
        GetAllIngredientsUseCase  getAllIngredients  = new GetAllIngredientsUseCase(ingredientRepository);
        GetAllUnitsUseCase        getAllUnits        = new GetAllUnitsUseCase(unitRepository);

        // Recipe
        GetAllRecipesUseCase         getAllRecipes         = new GetAllRecipesUseCase(recipeRepository);
        GetRecipeByIdUseCase         getRecipeById         = new GetRecipeByIdUseCase(recipeRepository, userRepository, sessionService);
        SearchRecipesUseCase         searchRecipes         = new SearchRecipesUseCase(recipeRepository);
        GetRecipesByAuthorUseCase    getRecipesByAuthor    = new GetRecipesByAuthorUseCase(recipeRepository);
        SuggestRecipeTitlesUseCase   suggestRecipeTitles   = new SuggestRecipeTitlesUseCase(recipeRepository);
        AddRatingUseCase          addRating          = new AddRatingUseCase(recipeRepository, transactionManager, sessionService, heavyIoGuard);
        CreateRecipeUseCase       createRecipe       = new CreateRecipeUseCase(recipeRepository, categoryRepository, difficultyRepository, transactionManager, sessionService, heavyIoGuard);
        UpdateRecipeUseCase       updateRecipe       = new UpdateRecipeUseCase(recipeRepository, categoryRepository, difficultyRepository, transactionManager, sessionService);
        DeleteRecipeUseCase       deleteRecipe       = new DeleteRecipeUseCase(recipeRepository, favoriteRepository, transactionManager, sessionService);
        AttachMediaUseCase        attachMedia        = new AttachMediaUseCase(recipeRepository, mediaStorage, transactionManager, sessionService, heavyIoGuard);

        // Identity
        LoginUseCase        loginUseCase    = new LoginUseCase(userRepository, passwordHasher);
        RegisterUserUseCase registerUseCase = new RegisterUserUseCase(userRepository, passwordHasher, transactionManager);

        // Social
        GetUserFavoritesUseCase getUserFavorites = new GetUserFavoritesUseCase(favoriteRepository, sessionService);
        ToggleFavoriteUseCase   toggleFavorite   = new ToggleFavoriteUseCase(favoriteRepository, transactionManager, sessionService);
        IsFavoriteUseCase       isFavorite       = new IsFavoriteUseCase(favoriteRepository, sessionService);

        // Interop
        ImportRecipeUseCase importRecipe = new ImportRecipeUseCase(
                recipeRepository, transactionManager, sessionService, interopPort);
        ExportRecipeUseCase exportRecipe = new ExportRecipeUseCase(recipeRepository, interopPort);

        // Reports
        GenerateRecipeTechnicalSheetUseCase generateTechnicalSheet = new GenerateRecipeTechnicalSheetUseCase(recipeRepository, userRepository, recipeReportAdapter);
        GetRecipeSummariesByIdsUseCase      getRecipeSummariesByIds = new GetRecipeSummariesByIdsUseCase(recipeRepository);

        // ----------------------------------------------------------------
        // BLOCK 4: UI Services & Navigation
        // ----------------------------------------------------------------
        RecipeQueryContext queryContext = new RecipeQueryContext(
                getAllRecipes, getRecipeById, searchRecipes, getUserFavorites, getRecipesByAuthor,
                getRecipeSummariesByIds, suggestRecipeTitles
        );
        RecipeCommandContext commandContext = new RecipeCommandContext(
                addRating, createRecipe, updateRecipe, deleteRecipe, attachMedia,
                getAllIngredients, getAllUnits, getAllCategories, getAllDifficulties,
                sessionService,
                toggleFavorite, isFavorite,
                importRecipe, exportRecipe,
                generateTechnicalSheet
        );

        RecipeQueryWrapper   queryWrapper   = new RecipeQueryWrapper(queryContext, sessionService);
        RecipeCommandWrapper commandWrapper = new RecipeCommandWrapper(commandContext);

        NavigationService nav = new NavigationService(primaryStage, queryWrapper, commandWrapper,
                loginUseCase, registerUseCase, sessionService, concurrencyProvider.executor(),
                storageBasePath);
        nav.toLogin();
    }

    public static void main(String[] args) {
        launch(args);
    }

    /**
     * Resolves the first SPI provider for {@code serviceClass}. Throws an
     * {@link IllegalStateException} naming the missing service if none is
     * registered — turning a misconfigured module-info.java into a clear
     * startup failure rather than a NullPointerException somewhere downstream.
     */
    private static <T> T loadService(Class<T> serviceClass) {
        return ServiceLoader.load(serviceClass).findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No SPI provider registered for: " + serviceClass.getName()
                                + ". Check module-info.java's `provides ... with ...` directive."));
    }
}
