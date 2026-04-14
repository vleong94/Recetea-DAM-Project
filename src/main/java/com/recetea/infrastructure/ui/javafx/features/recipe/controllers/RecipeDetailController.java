package com.recetea.infrastructure.ui.javafx.features.recipe.controllers;

import com.recetea.core.recipe.application.ports.in.dto.RecipeIngredientResponse;
import com.recetea.infrastructure.ui.javafx.features.recipe.RecipeContext;
import com.recetea.infrastructure.ui.javafx.shared.navigation.NavigationService;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.math.BigDecimal;

/**
 * Controller de la vista de lectura detallada de recetas.
 * Actúa como un Inbound Adapter de solo lectura que consume Data Transfer Objects (DTOs)
 * para hidratar la Interfaz de Usuario. Garantiza el aislamiento de las Entities
 * del Core al proyectar únicamente proyecciones inmutables de los datos.
 */
public class RecipeDetailController {

    @FXML private Label titleLabel;
    @FXML private Label prepTimeLabel;
    @FXML private Label servingsLabel;
    @FXML private Label descriptionLabel;

    @FXML private TableView<RecipeIngredientResponse> ingredientsTable;
    @FXML private TableColumn<RecipeIngredientResponse, String> colIngredientName;
    @FXML private TableColumn<RecipeIngredientResponse, String> colUnit;
    @FXML private TableColumn<RecipeIngredientResponse, BigDecimal> colQuantity;

    private RecipeContext context;
    private NavigationService nav;

    /**
     * Inicializa el Controller inyectando las dependencias estructurales.
     * Configura el mapeo de las columnas de la tabla de forma previa
     * a la recepción de los datos del catálogo.
     */
    public void init(RecipeContext context, NavigationService nav) {
        this.context = context;
        this.nav = nav;
        setupTable();
    }

    /**
     * Define el Data Binding de la tabla mediante Cell Factories.
     * Vincula cada columna a las propiedades específicas del DTO de respuesta,
     * utilizando envoltorios de solo lectura para mantener la inmutabilidad.
     */
    private void setupTable() {
        colIngredientName.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().ingredientName()));
        colUnit.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().unitName()));
        colQuantity.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().quantity()));
    }

    /**
     * Dispara el flujo de recuperación profunda (Deep Load) de la receta.
     * Utiliza el identificador para ejecutar el Use Case correspondiente,
     * procesando la respuesta mediante programación funcional para hidratar
     * los nodos visuales o manejar la ausencia del registro de forma segura.
     */
    public void loadRecipeDetails(int recipeId) {
        context.getRecipeById().execute(recipeId).ifPresentOrElse(recipe -> {
            titleLabel.setText(recipe.title());
            prepTimeLabel.setText(recipe.prepTimeMinutes() + " min");
            servingsLabel.setText(String.valueOf(recipe.servings()));
            descriptionLabel.setText(recipe.description());

            ingredientsTable.setItems(FXCollections.observableArrayList(recipe.ingredients()));
        }, () -> showError("Receta no encontrada", "El registro solicitado no existe en la base de datos."));
    }

    /**
     * Delega el Routing de retorno hacia el panel principal mediante el Navigation Service.
     */
    @FXML
    public void onBackButtonClick() {
        nav.toDashboard();
    }

    /**
     * Centraliza la presentación de excepciones o inconsistencias de datos
     * mediante la instanciación de diálogos modales nativos.
     */
    private void showError(String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}