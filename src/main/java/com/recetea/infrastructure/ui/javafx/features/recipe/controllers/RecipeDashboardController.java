package com.recetea.infrastructure.ui.javafx.features.recipe.controllers;

import com.recetea.core.recipe.application.ports.in.dto.RecipeSummaryResponse;
import com.recetea.core.recipe.domain.vo.RecipeId;
import com.recetea.core.shared.domain.PageRequest;
import com.recetea.infrastructure.storage.StorageConfig;
import com.recetea.infrastructure.ui.javafx.features.recipe.RecipeCommandProvider;
import com.recetea.infrastructure.ui.javafx.features.recipe.RecipeQueryProvider;
import com.recetea.infrastructure.ui.javafx.shared.components.ThumbnailTableCell;
import com.recetea.infrastructure.ui.javafx.shared.navigation.NavigationService;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class RecipeDashboardController {

    @FXML private TableView<RecipeSummaryResponse> recipeTable;
    @FXML private TableColumn<RecipeSummaryResponse, String> photoColumn;
    @FXML private TableColumn<RecipeSummaryResponse, String> idColumn;
    @FXML private TableColumn<RecipeSummaryResponse, String> titleColumn;
    @FXML private TableColumn<RecipeSummaryResponse, String> categoryColumn;
    @FXML private TableColumn<RecipeSummaryResponse, String> difficultyColumn;
    @FXML private TableColumn<RecipeSummaryResponse, Integer> prepColumn;
    @FXML private TableColumn<RecipeSummaryResponse, Integer> servingsColumn;
    @FXML private TableColumn<RecipeSummaryResponse, String> scoreColumn;
    @FXML private TableColumn<RecipeSummaryResponse, Integer> ratingsColumn;
    @FXML private TableColumn<RecipeSummaryResponse, Void> favoriteColumn;
    @FXML private TableColumn<RecipeSummaryResponse, Void> actionsColumn;

    private RecipeQueryProvider queryProvider;
    private RecipeCommandProvider commandProvider;
    private NavigationService nav;

    // Snapshot of the user's favorite recipe IDs; rebuilt on every loadData() call.
    private Set<RecipeId> favoriteIds = new HashSet<>();

    @FXML
    public void initialize() {
        photoColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().mainMediaStorageKey()));
        photoColumn.setCellFactory(col -> new ThumbnailTableCell(StorageConfig.getBasePath()));

        idColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(String.valueOf(cell.getValue().id().value())));
        titleColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().title()));
        categoryColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().categoryName()));
        difficultyColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().difficultyName()));
        prepColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().prepTimeMinutes()));
        servingsColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().servings()));
        scoreColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(
                cell.getValue().averageScore().setScale(1, java.math.RoundingMode.HALF_UP) + " / 5"));
        ratingsColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().totalRatings()));
    }

    public void init(RecipeQueryProvider queryProvider, RecipeCommandProvider commandProvider, NavigationService nav) {
        this.queryProvider = queryProvider;
        this.commandProvider = commandProvider;
        this.nav = nav;
        setupFavoriteColumn();
        setupActionsColumn();
        loadData();
    }

    private void setupFavoriteColumn() {
        favoriteColumn.setCellFactory(col -> new TableCell<>() {
            private final ToggleButton btn = new ToggleButton();

            {
                btn.setOnAction(e -> {
                    RecipeSummaryResponse recipe = getTableView().getItems().get(getIndex());
                    commandProvider.toggleFavorite().execute(recipe.id());
                    loadData();
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() < 0 || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null);
                    return;
                }
                RecipeSummaryResponse recipe = getTableView().getItems().get(getIndex());
                boolean isFav = favoriteIds.contains(recipe.id());
                btn.setSelected(isFav);
                btn.setText(isFav ? "★" : "☆");
                setGraphic(btn);
            }
        });
    }

    private void setupActionsColumn() {
        actionsColumn.setCellFactory(param -> new TableCell<>() {
            private final Button editBtn = new Button("Editar");
            private final Button deleteBtn = new Button("Borrar");
            private final HBox container = new HBox(10, editBtn, deleteBtn);

            {
                editBtn.setOnAction(event -> {
                    RecipeSummaryResponse recipe = getTableView().getItems().get(getIndex());
                    handleEditAction(recipe);
                });
                deleteBtn.setOnAction(event -> {
                    RecipeSummaryResponse recipe = getTableView().getItems().get(getIndex());
                    handleDeleteAction(recipe);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() < 0 || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null);
                    return;
                }
                RecipeSummaryResponse recipe = getTableView().getItems().get(getIndex());
                boolean isOwner = commandProvider.sessionService().getCurrentUserId()
                        .map(id -> id.equals(recipe.authorId())).orElse(false);
                editBtn.setVisible(isOwner);
                editBtn.setManaged(isOwner);
                deleteBtn.setVisible(isOwner);
                deleteBtn.setManaged(isOwner);
                setGraphic(container);
            }
        });

        recipeTable.setOnMouseClicked(this::handleTableDoubleClick);
    }

    public void loadData() {
        List<RecipeSummaryResponse> recipes = queryProvider.getAllRecipes().execute(new PageRequest(0, 20)).content();
        favoriteIds = queryProvider.getUserFavorites().execute().stream()
                .map(RecipeSummaryResponse::id)
                .collect(Collectors.toCollection(HashSet::new));
        recipeTable.setItems(FXCollections.observableArrayList(recipes));
        recipeTable.refresh();
    }

    @FXML
    public void onLogoutButtonClick() {
        nav.logout();
    }

    @FXML
    public void onCreateButtonClick() {
        nav.toRecipeCreate();
    }

    private void handleEditAction(RecipeSummaryResponse recipe) {
        queryProvider.getRecipeById().execute(recipe.id()).ifPresent(nav::toRecipeUpdate);
    }

    private void handleDeleteAction(RecipeSummaryResponse recipe) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "¿Estás seguro de que deseas eliminar la receta: " + recipe.title() + "?");
        alert.setHeaderText("Confirm Delete");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                nav.deleteRecipe(recipe.id());
                loadData();
            }
        });
    }

    private void handleTableDoubleClick(MouseEvent event) {
        if (event.getClickCount() == 2) {
            RecipeSummaryResponse selected = recipeTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                nav.toRecipeDetail(selected.id());
            }
        }
    }
}
