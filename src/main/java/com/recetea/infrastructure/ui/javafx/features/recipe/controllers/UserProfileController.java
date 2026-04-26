package com.recetea.infrastructure.ui.javafx.features.recipe.controllers;

import com.recetea.core.recipe.application.ports.in.dto.RecipeSummaryResponse;
import com.recetea.core.shared.domain.PageRequest;
import com.recetea.core.user.domain.UserId;
import com.recetea.infrastructure.storage.StorageConfig;
import com.recetea.infrastructure.ui.javafx.features.recipe.RecipeCommandProvider;
import com.recetea.infrastructure.ui.javafx.features.recipe.RecipeQueryProvider;
import com.recetea.infrastructure.ui.javafx.features.recipe.components.RecipeCardComponent;
import com.recetea.infrastructure.ui.javafx.shared.navigation.NavigationService;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.concurrent.ExecutorService;

public class UserProfileController {

    @FXML private VBox     creationsEmpty;
    @FXML private FlowPane creationsContainer;

    @FXML private VBox     favoritesEmpty;
    @FXML private FlowPane favoritesContainer;

    private RecipeQueryProvider   queryProvider;
    private RecipeCommandProvider commandProvider;
    private NavigationService     nav;
    private ExecutorService       executor;

    public void init(RecipeQueryProvider queryProvider, RecipeCommandProvider commandProvider,
                     NavigationService nav, ExecutorService executor) {
        this.queryProvider   = queryProvider;
        this.commandProvider = commandProvider;
        this.nav             = nav;
        this.executor        = executor;
        loadAll();
    }

    // ── Data loading ──────────────────────────────────────────────────────────

    private void loadAll() {
        UserId currentUser = commandProvider.sessionService().getCurrentUserId().orElse(null);
        if (currentUser == null || executor == null) return;

        Task<ProfileData> task = new Task<>() {
            @Override
            protected ProfileData call() {
                List<RecipeSummaryResponse> creations =
                        queryProvider.getRecipesByAuthor().execute(currentUser, new PageRequest(0, 200)).content();
                List<RecipeSummaryResponse> favorites =
                        queryProvider.getUserFavorites().execute();
                return new ProfileData(creations, favorites);
            }
        };
        task.setOnSucceeded(e -> {
            ProfileData data = task.getValue();
            displayCards(creationsContainer, creationsEmpty, data.creations());
            displayCards(favoritesContainer, favoritesEmpty, data.favorites());
        });
        task.setOnFailed(e -> Thread.getDefaultUncaughtExceptionHandler()
                .uncaughtException(Thread.currentThread(), task.getException()));
        executor.execute(task);
    }

    private void displayCards(FlowPane container, VBox emptyPlaceholder,
                              List<RecipeSummaryResponse> items) {
        container.getChildren().clear();
        boolean empty = items.isEmpty();
        emptyPlaceholder.setVisible(empty);
        emptyPlaceholder.setManaged(empty);
        for (RecipeSummaryResponse recipe : items) {
            container.getChildren().add(
                    new RecipeCardComponent(recipe, id -> nav.toRecipeDetail(id), StorageConfig.getBasePath()));
        }
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    @FXML
    public void onBackButtonClick() {
        nav.toDashboard();
    }

    private record ProfileData(List<RecipeSummaryResponse> creations, List<RecipeSummaryResponse> favorites) {}
}
