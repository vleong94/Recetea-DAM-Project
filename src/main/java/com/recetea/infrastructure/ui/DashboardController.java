package com.recetea.infrastructure.ui;

import com.recetea.core.domain.Recipe;
import com.recetea.core.ports.in.ICreateRecipeUseCase;
import com.recetea.core.ports.in.IGetAllRecipesUseCase;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;

/**
 * UI Controller: Gestiona la pantalla principal (Dashboard).
 * Se encarga de mostrar el catálogo de recetas y navegar a la pantalla de creación.
 */
public class DashboardController {

    @FXML private TableView<Recipe> recipeTable;
    @FXML private TableColumn<Recipe, Integer> idColumn;
    @FXML private TableColumn<Recipe, String> titleColumn;
    @FXML private TableColumn<Recipe, Integer> prepColumn;
    @FXML private TableColumn<Recipe, Integer> servingsColumn;

    // Puertos de entrada (Casos de uso)
    private IGetAllRecipesUseCase getAllRecipesUseCase;
    private ICreateRecipeUseCase createRecipeUseCase;

    // --- INYECCIÓN DE DEPENDENCIAS ---

    public void setGetAllRecipesUseCase(IGetAllRecipesUseCase useCase) {
        this.getAllRecipesUseCase = useCase;
    }

    public void setCreateRecipeUseCase(ICreateRecipeUseCase useCase) {
        this.createRecipeUseCase = useCase;
    }

    // --- LÓGICA DE PRESENTACIÓN ---

    /**
     * Extrae los datos del núcleo y puebla la tabla visual.
     */
    public void loadData() {
        if (getAllRecipesUseCase != null) {
            // 1. Configuramos el Data Binding (Mapeo de propiedades a columnas)
            idColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getId()));
            titleColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getTitle()));
            prepColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getPreparationTimeMinutes()));
            servingsColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getServings()));

            // 2. Extraemos los datos a través del puerto
            List<Recipe> recipes = getAllRecipesUseCase.execute();

            // 3. Inyectamos los datos en el componente reactivo de JavaFX
            ObservableList<Recipe> observableRecipes = FXCollections.observableArrayList(recipes);
            recipeTable.setItems(observableRecipes);
        }
    }

    // --- NAVEGACIÓN (ROUTING) ---

    /**
     * Intercepta el clic del botón "Nueva Receta" y cambia la escena.
     */
    @FXML
    public void onNewRecipeClick(ActionEvent event) {
        try {
            // 1. Cargamos el FXML de la pantalla de destino
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/recetea/infrastructure/ui/create_recipe.fxml"));
            Parent root = loader.load();

            // 2. Transmitimos la dependencia (El caso de uso de creación) al nuevo controlador
            CreateRecipeController controller = loader.getController();
            // Pasamos AMBOS casos de uso para que el otro controlador pueda devolvérnoslos al regresar
            controller.setUseCases(this.createRecipeUseCase, this.getAllRecipesUseCase);

            // 3. Reemplazamos la escena actual en la misma ventana (Stage)
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root, 600, 500));
            stage.setTitle("Recetea - Creación de Recetas");

        } catch (IOException e) {
            System.err.println("Error crítico de UI al cargar create_recipe.fxml: " + e.getMessage());
            e.printStackTrace();
        }
    }
}