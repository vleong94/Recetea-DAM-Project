package com.recetea;

import com.recetea.core.ports.IRecipeRepository;
import com.recetea.core.ports.in.ICreateRecipeUseCase;
import com.recetea.core.usecases.CreateRecipeUseCase;
import com.recetea.infrastructure.persistence.JdbcRecipeRepository;
import com.recetea.infrastructure.ui.CreateRecipeController;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Composition Root: Punto de entrada de la aplicación.
 * Ensambla todas las capas de la Arquitectura Hexagonal y levanta JavaFX.
 */
public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // 1. INICIALIZACIÓN DEL NÚCLEO (Backend)
        // Instanciamos el adaptador de infraestructura y el caso de uso
        IRecipeRepository repository = new JdbcRecipeRepository();
        ICreateRecipeUseCase createRecipeUseCase = new CreateRecipeUseCase(repository);

        // 2. CARGA DE LA VISTA (Frontend)
        // Usamos la ruta absoluta del Classpath hacia la carpeta resources
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/recetea/infrastructure/ui/create_recipe.fxml"));
        Parent root = loader.load();

        // 3. INYECCIÓN DE DEPENDENCIAS (Wiring)
        // Le pasamos el orquestador al controlador visual
        CreateRecipeController controller = loader.getController();
        controller.setCreateRecipeUseCase(createRecipeUseCase);

        // 4. RENDERIZADO DE LA VENTANA
        primaryStage.setTitle("Recetea - Creación de Recetas");
        primaryStage.setScene(new Scene(root, 600, 500));
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    public static void main(String[] args) {
        // Delega la ejecución al motor de JavaFX
        launch(args);
    }
}