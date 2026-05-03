package com.recetea.infrastructure.ui.javafx.shared.navigation;

import com.recetea.core.recipe.application.ports.in.dto.RecipeDetailResponse;
import com.recetea.core.recipe.domain.vo.RecipeId;
import com.recetea.core.shared.application.ports.in.INavigationPort;
import com.recetea.core.shared.application.ports.in.IUserSessionService;
import com.recetea.core.user.application.ports.in.ILoginUseCase;
import com.recetea.core.user.application.ports.in.IRegisterUserUseCase;
import com.recetea.infrastructure.ui.javafx.features.identity.controllers.LoginController;
import com.recetea.infrastructure.ui.javafx.features.identity.controllers.RegisterController;
import com.recetea.infrastructure.ui.javafx.features.recipe.RecipeCommandProvider;
import com.recetea.infrastructure.ui.javafx.features.recipe.RecipeQueryProvider;
import com.recetea.infrastructure.ui.javafx.features.recipe.controllers.BaseRecipeFormController;
import com.recetea.infrastructure.ui.javafx.features.recipe.controllers.RecipeCreateController;
import com.recetea.infrastructure.ui.javafx.features.recipe.controllers.RecipeDashboardController;
import com.recetea.infrastructure.ui.javafx.features.recipe.controllers.RecipeDetailController;
import com.recetea.infrastructure.ui.javafx.features.recipe.controllers.RecipeUpdateController;
import com.recetea.infrastructure.ui.javafx.features.recipe.controllers.UserProfileController;
import com.recetea.infrastructure.ui.javafx.shared.i18n.I18n;
import javafx.animation.FadeTransition;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.Region;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

/**
 * JavaFX-specific implementation of {@link INavigationPort}. Owns scene
 * construction, controller injection, and the per-view keyboard
 * shortcut dispatcher — controllers depend on the port interface and
 * never see this class directly.
 *
 * <p><b>Scene reuse.</b> The {@link Scene} is constructed once on the
 * first navigation (when {@code stage.getScene() == null}); subsequent
 * navigations call {@code scene.setRoot(root)} to swap content. This
 * preserves the user's window size, position, and maximize state across
 * page transitions — the alternative (one Scene per view) would reset
 * the OS window state on every click.
 *
 * <p><b>Permanent key filter.</b> ESC and Ctrl+F bindings dispatch
 * through one {@code KEY_PRESSED} event filter installed on the Scene at
 * first nav; the filter reads two mutable fields
 * ({@link #currentBackAction} / {@link #currentCtrlFAction}) which
 * {@code loadScene} updates per navigation. Replacing the filter each
 * time would either leak old handlers or require tracking the previous
 * one — both messier than this single-filter dispatch.
 *
 * <p><b>Storage path threading.</b> The constructor takes the resolved
 * storage root as a String and threads it down to every controller that
 * renders media URIs, so UI URI resolution and storage writes can never
 * disagree.
 *
 * <p><b>ES — </b>Implementación específica de JavaFX de
 * {@link INavigationPort}. Es dueña de la construcción de la escena,
 * la inyección de controladores y el dispatcher de atajos de teclado
 * por vista — los controladores dependen de la interfaz del puerto
 * y nunca ven directamente esta clase.
 *
 * <p><b>Reutilización de escena.</b> La {@link Scene} se construye
 * una vez en la primera navegación (cuando
 * {@code stage.getScene() == null}); las navegaciones posteriores
 * llaman a {@code scene.setRoot(root)} para intercambiar el
 * contenido. Esto preserva el tamaño, posición y estado de
 * maximización de la ventana del usuario entre transiciones de
 * página — la alternativa (una Scene por vista) reiniciaría el
 * estado de ventana del SO en cada click.
 *
 * <p><b>Filtro de teclado permanente.</b> Los bindings de ESC y
 * Ctrl+F se despachan a través de un único filtro de evento
 * {@code KEY_PRESSED} instalado en la Scene en la primera
 * navegación; el filtro lee dos campos mutables
 * ({@link #currentBackAction} / {@link #currentCtrlFAction}) que
 * {@code loadScene} actualiza en cada navegación. Reemplazar el
 * filtro cada vez filtraría handlers antiguos o requeriría
 * rastrear el anterior — ambas cosas son más feas que este
 * despacho con un solo filtro.
 *
 * <p><b>Threading de la ruta de storage.</b> El constructor recibe
 * la raíz de storage resuelta como String y la propaga hacia abajo
 * a cada controlador que renderiza URIs de multimedia, de modo que
 * la resolución de URIs en UI y las escrituras de storage no
 * puedan discrepar.
 */
public class NavigationService implements INavigationPort {

    private final Stage stage;
    private final RecipeQueryProvider queryProvider;
    private final RecipeCommandProvider commandProvider;
    private final ILoginUseCase loginUseCase;
    private final IRegisterUserUseCase registerUseCase;
    private final IUserSessionService sessionService;
    private final ExecutorService ioExecutor;

    /**
     * Storage root threaded down from the composition root. Either a local filesystem
     * path or an HTTPS public-read URL — passed verbatim to controllers so the
     * MediaUriResolver can dispatch on shape. Controllers receive only this resolved
     * String, never the raw {@code AppConfig} record (per the DI constraint that
     * controllers stay free of infrastructure-config types).
     */
    private final String storageBasePath;

    // Per-view key bindings live on the (single, reused) Scene via one permanent
    // event filter that dispatches through these mutable fields. Replacing the
    // filter on every navigation would either leak old handlers (if not removed)
    // or require us to track the previous one — both messier than this.
    private Runnable currentBackAction;
    private Runnable currentCtrlFAction;

    public NavigationService(Stage stage, RecipeQueryProvider queryProvider, RecipeCommandProvider commandProvider,
                             ILoginUseCase loginUseCase, IRegisterUserUseCase registerUseCase,
                             IUserSessionService sessionService, ExecutorService ioExecutor,
                             String storageBasePath) {
        this.stage = stage;
        this.queryProvider = queryProvider;
        this.commandProvider = commandProvider;
        this.loginUseCase = loginUseCase;
        this.registerUseCase = registerUseCase;
        this.sessionService = sessionService;
        this.ioExecutor = ioExecutor;
        this.storageBasePath = storageBasePath;
    }

    @Override
    public void toLogin() {
        loadScene("/com/recetea/infrastructure/ui/javafx/fxml/features/identity/pages/login.fxml",
                I18n.get("nav.title.login"),
                loader -> loader.<LoginController>getController().init(loginUseCase, sessionService, this));
    }

    @Override
    public void toRegister() {
        loadScene("/com/recetea/infrastructure/ui/javafx/fxml/features/identity/pages/register.fxml",
                I18n.get("nav.title.register"),
                loader -> loader.<RegisterController>getController().init(registerUseCase, this),
                this::toLogin, null);
    }

    @Override
    public void toDashboard() {
        RecipeDashboardController[] ref = {null};
        loadScene("/com/recetea/infrastructure/ui/javafx/fxml/features/recipe/pages/recipe_dashboard.fxml",
                I18n.get("nav.title.dashboard"),
                loader -> { ref[0] = loader.getController(); ref[0].init(queryProvider, commandProvider, this, ioExecutor, storageBasePath); },
                () -> ref[0].onEscape(), () -> ref[0].onCtrlF());
    }

    @Override
    public void toRecipeCreate() {
        toRecipeForm(new RecipeCreateController(), I18n.get("nav.title.recipeCreate"), null);
    }

    @Override
    public void toRecipeUpdate(RecipeDetailResponse recipe) {
        toRecipeForm(new RecipeUpdateController(), I18n.get("nav.title.recipeUpdate"),
                ctrl -> ctrl.loadRecipeData(recipe));
    }

    /**
     * Shared loader for the unified recipe form (Create / Update).
     * Wires the controller (FXML has no fx:controller), runs the standard init,
     * and applies an optional post-init step (e.g. hydrating an existing recipe).
     */
    private <C extends BaseRecipeFormController> void toRecipeForm(C ctrl, String title, Consumer<C> postInit) {
        loadScene(FORM_FXML, title,
                loader -> loader.setController(ctrl),
                loader -> {
                    ctrl.init(commandProvider, this, storageBasePath);
                    if (postInit != null) postInit.accept(ctrl);
                },
                this::toDashboard, null);
    }

    @Override
    public void toRecipeDetail(RecipeId recipeId) {
        loadScene("/com/recetea/infrastructure/ui/javafx/fxml/features/recipe/pages/recipe_detail.fxml",
                I18n.get("nav.title.recipeDetail"),
                loader -> { RecipeDetailController c = loader.getController(); c.init(queryProvider, commandProvider, this, ioExecutor, storageBasePath); c.loadRecipeDetails(recipeId); },
                this::toDashboard, null);
    }

    @Override
    public void toUserProfile() {
        loadScene("/com/recetea/infrastructure/ui/javafx/fxml/features/recipe/pages/user_profile.fxml",
                I18n.get("nav.title.profile"),
                loader -> loader.<UserProfileController>getController().init(queryProvider, commandProvider, this, ioExecutor, storageBasePath),
                this::toDashboard, null);
    }

    @Override
    public void logout() {
        sessionService.logout();
        toLogin();
    }

    public void deleteRecipe(RecipeId id) {
        commandProvider.deleteRecipe().execute(id);
    }

    private static final String STYLESHEET =
            NavigationService.class.getResource(
                    "/com/recetea/infrastructure/ui/javafx/css/app.css").toExternalForm();

    private static final String FORM_FXML =
            "/com/recetea/infrastructure/ui/javafx/fxml/features/recipe/pages/recipe_form.fxml";

    private void loadScene(String fxmlPath, String title, Consumer<FXMLLoader> config) {
        loadScene(fxmlPath, title, null, config, null, null);
    }

    private void loadScene(String fxmlPath, String title, Consumer<FXMLLoader> config,
                           Runnable backAction, Runnable ctrlFAction) {
        loadScene(fxmlPath, title, null, config, backAction, ctrlFAction);
    }

    private void loadScene(String fxmlPath, String title,
                           Consumer<FXMLLoader> preLoad, Consumer<FXMLLoader> postLoad,
                           Runnable backAction, Runnable ctrlFAction) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            loader.setResources(I18n.bundle());
            if (preLoad != null) preLoad.accept(loader);
            Parent root = loader.load();
            postLoad.accept(loader);

            // Per-nav key actions are stored in fields; the permanent scene filter
            // dispatches through them. Update before installing/swapping anything.
            currentBackAction  = backAction;
            currentCtrlFAction = ctrlFAction;

            Scene scene = stage.getScene();
            if (scene == null) {
                // Initial launch: build the Scene, attach the stylesheet, install
                // the single permanent KEY_PRESSED filter, then maximize + show.
                // setMaximized + centering only run here — subsequent navigations
                // preserve whatever size/position the user has chosen.
                scene = new Scene(root);
                scene.getStylesheets().add(STYLESHEET);
                scene.addEventFilter(KeyEvent.KEY_PRESSED, this::handleSceneKey);
                stage.setScene(scene);
                stage.setMaximized(true);
                stage.show();
                if (!stage.isMaximized()) {
                    Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
                    stage.setX((bounds.getWidth()  - stage.getWidth())  / 2);
                    stage.setY((bounds.getHeight() - stage.getHeight()) / 2);
                }
            } else {
                // Subsequent navigation: swap the root on the existing Scene.
                // Window size, position, and maximize state stay exactly as the
                // user left them. NO setMaximized, NO sizeToScene, NO centering.
                scene.setRoot(root);
            }

            if (root instanceof Region r) {
                r.prefWidthProperty().bind(scene.widthProperty());
                r.prefHeightProperty().bind(scene.heightProperty());
            }

            stage.setTitle("Recetea - " + title);

            FadeTransition fadeIn = new FadeTransition(Duration.millis(200), root);
            fadeIn.setFromValue(0.0);
            fadeIn.setToValue(1.0);
            fadeIn.play();
        } catch (IOException e) {
            throw new RuntimeException("Critical I/O failure loading view: " + fxmlPath, e);
        }
    }

    private void handleSceneKey(KeyEvent e) {
        if (currentBackAction != null && e.getCode() == KeyCode.ESCAPE) {
            e.consume();
            currentBackAction.run();
        } else if (currentCtrlFAction != null && e.isControlDown() && e.getCode() == KeyCode.F) {
            e.consume();
            currentCtrlFAction.run();
        }
    }
}
