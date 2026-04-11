package com.recetea;

import com.recetea.core.ports.IRecipeRepository;
import com.recetea.core.ports.in.ICreateRecipeUseCase;
import com.recetea.core.ports.in.IGetAllRecipesUseCase;
import com.recetea.core.ports.in.IGetRecipeByIdUseCase;
import com.recetea.core.usecases.CreateRecipeUseCase;
import com.recetea.core.usecases.GetAllRecipesUseCase;
import com.recetea.core.usecases.GetRecipeByIdUseCase;
import com.recetea.infrastructure.persistence.JdbcRecipeRepository;
import com.recetea.infrastructure.ui.DashboardController;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Composition Root: Punto de entrada de la aplicación.
 * Ensambla todas las capas de la Arquitectura Hexagonal, inyecta dependencias
 * y levanta la interfaz principal (Dashboard).
 */
public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // 1. INICIALIZACIÓN DEL NÚCLEO (Backend)
        IRecipeRepository repository = new JdbcRecipeRepository();

        // Instanciamos los 3 Casos de Uso (Puertos de Entrada)
        IGetAllRecipesUseCase getAllRecipesUseCase = new GetAllRecipesUseCase(repository);
        ICreateRecipeUseCase createRecipeUseCase = new CreateRecipeUseCase(repository);
        IGetRecipeByIdUseCase getRecipeByIdUseCase = new GetRecipeByIdUseCase(repository); // <--- NUEVA INSTANCIA

        // 2. CARGA DE LA VISTA (Frontend - Dashboard Principal)
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/recetea/infrastructure/ui/dashboard.fxml"));
        Parent root = loader.load();

        // 3. INYECCIÓN DE DEPENDENCIAS (Wiring)
        DashboardController controller = loader.getController();

        // Inyectamos el caso de uso de lectura para poblar la tabla
        controller.setGetAllRecipesUseCase(getAllRecipesUseCase);

        // Inyectamos el caso de uso de creación para el routing
        controller.setCreateRecipeUseCase(createRecipeUseCase);

        // Inyectamos el caso de uso de extracción profunda (Lectura por ID) para el evento de doble clic
        controller.setGetRecipeByIdUseCase(getRecipeByIdUseCase); // <--- NUEVA INYECCIÓN

        // Disparamos la carga de datos inmediatamente después de inyectar las dependencias
        controller.loadData();

        // 4. RENDERIZADO DE LA VENTANA
        primaryStage.setTitle("Recetea - Dashboard Principal");
        primaryStage.setScene(new Scene(root, 700, 500));
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    public static void main(String[] args) {
        // Delega la ejecución al motor de JavaFX
        launch(args);
    }
}