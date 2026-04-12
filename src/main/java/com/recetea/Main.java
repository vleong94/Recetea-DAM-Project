package com.recetea;

import com.recetea.core.ports.out.IRecipeRepository;
import com.recetea.core.ports.out.IIngredientRepository;
import com.recetea.core.ports.out.IUnitRepository;

import com.recetea.core.usecases.recipe.*;
import com.recetea.core.usecases.ingredient.GetAllIngredientsUseCase;
import com.recetea.core.usecases.unit.GetAllUnitsUseCase;

import com.recetea.infrastructure.persistence.jbdc.JdbcRecipeRepository;
import com.recetea.infrastructure.persistence.jbdc.JdbcIngredientRepository;
import com.recetea.infrastructure.persistence.jbdc.JdbcUnitRepository;

import com.recetea.infrastructure.ui.services.NavigationService;
import com.recetea.infrastructure.ui.services.RecipeServiceContext;

import javafx.application.Application;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Composition Root: Punto de entrada de la aplicación.
 * Ensambla las capas, inicializa el contexto de servicios y delega la
 * gestión de la interfaz al NavigationService.
 */
public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        Properties props = new Properties();

        // 1. CARGA DE CONFIGURACIÓN EXTERNA
        // Cargamos las credenciales desde el archivo para evitar subirlas a GitHub.
        try (InputStream input = getClass().getResourceAsStream("/application-local.properties")) {
            if (input == null) {
                throw new RuntimeException("CRÍTICO: No se pudo encontrar application-local.properties en la carpeta de recursos.");
            }
            props.load(input);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        String dbUrl = props.getProperty("db.url");
        String dbUser = props.getProperty("db.user");
        String dbPass = props.getProperty("db.password");

        // 2. CONFIGURACIÓN DE INFRAESTRUCTURA (Repositories)
        // Inyectamos las credenciales cargadas del archivo properties
        IRecipeRepository recipeRepository = new JdbcRecipeRepository(dbUrl, dbUser, dbPass);
        IIngredientRepository ingredientRepository = new JdbcIngredientRepository(dbUrl, dbUser, dbPass);
        IUnitRepository unitRepository = new JdbcUnitRepository(dbUrl, dbUser, dbPass);

        // 3. ENSAMBLAJE DE CASOS DE USO (Application Layer)
        // Agrupamos todas las capacidades en el Context Object para la UI
        RecipeServiceContext context = new RecipeServiceContext(
                new CreateRecipeUseCase(recipeRepository),
                new GetAllRecipesUseCase(recipeRepository),
                new GetRecipeByIdUseCase(recipeRepository),
                new UpdateRecipeUseCase(recipeRepository),
                new DeleteRecipeUseCase(recipeRepository),
                new GetAllIngredientsUseCase(ingredientRepository),
                new GetAllUnitsUseCase(unitRepository)
        );

        // 4. INICIALIZACIÓN DEL MOTOR DE NAVEGACIÓN
        // El Main ya no carga FXMLs; ahora delega esta responsabilidad al servicio especializado
        NavigationService nav = new NavigationService(primaryStage, context);

        // 5. ARRANQUE DE LA APLICACIÓN
        nav.toDashboard();
    }

    public static void main(String[] args) {
        launch(args);
    }
}