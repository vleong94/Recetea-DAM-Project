package com.recetea;

import com.recetea.core.ports.IRecipeRepository;
import com.recetea.core.ports.in.ICreateRecipeUseCase;
import com.recetea.core.ports.in.IDeleteRecipeUseCase;
import com.recetea.core.ports.in.IGetAllRecipesUseCase;
import com.recetea.core.ports.in.IGetRecipeByIdUseCase;
import com.recetea.core.ports.in.IUpdateRecipeUseCase; // <--- NUEVO IMPORT
import com.recetea.core.usecases.CreateRecipeUseCase;
import com.recetea.core.usecases.DeleteRecipeUseCase;
import com.recetea.core.usecases.GetAllRecipesUseCase;
import com.recetea.core.usecases.GetRecipeByIdUseCase;
import com.recetea.core.usecases.UpdateRecipeUseCase; // <--- NUEVO IMPORT
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
        // 1. INICIALIZACIÓN DEL NÚCLEO (Backend - Adaptador de Infraestructura)
        IRecipeRepository repository = new JdbcRecipeRepository();

        // Instanciamos los 5 Casos de Uso (Puertos de Entrada)
        // El ciclo CRUD completo ya está aquí representado.
        IGetAllRecipesUseCase getAllRecipesUseCase = new GetAllRecipesUseCase(repository);
        ICreateRecipeUseCase createRecipeUseCase = new CreateRecipeUseCase(repository);
        IGetRecipeByIdUseCase getRecipeByIdUseCase = new GetRecipeByIdUseCase(repository);
        IDeleteRecipeUseCase deleteRecipeUseCase = new DeleteRecipeUseCase(repository);
        IUpdateRecipeUseCase updateRecipeUseCase = new UpdateRecipeUseCase(repository); // <--- NUEVA INSTANCIA

        // 2. CARGA DE LA VISTA (Frontend - Dashboard Principal)
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/recetea/infrastructure/ui/dashboard.fxml"));
        Parent root = loader.load();

        // 3. INYECCIÓN DE DEPENDENCIAS (Wiring / Cableado)
        DashboardController controller = loader.getController();

        // Inyectamos el ecosistema completo de casos de uso en el controlador principal
        controller.setGetAllRecipesUseCase(getAllRecipesUseCase);
        controller.setCreateRecipeUseCase(createRecipeUseCase);
        controller.setGetRecipeByIdUseCase(getRecipeByIdUseCase);
        controller.setDeleteRecipeUseCase(deleteRecipeUseCase);
        controller.setUpdateRecipeUseCase(updateRecipeUseCase); // <--- NUEVA INYECCIÓN

        // Disparamos la carga inicial de datos para que la tabla no nazca vacía
        controller.loadData();

        // 4. RENDERIZADO DE LA VENTANA PRINCIPAL
        primaryStage.setTitle("Recetea - Dashboard Principal");
        primaryStage.setScene(new Scene(root, 700, 500));
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    public static void main(String[] args) {
        // Cede el control al framework JavaFX
        launch(args);
    }
}