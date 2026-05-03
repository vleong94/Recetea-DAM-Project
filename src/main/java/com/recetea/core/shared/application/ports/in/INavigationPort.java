package com.recetea.core.shared.application.ports.in;

import com.recetea.core.recipe.application.ports.in.dto.RecipeDetailResponse;
import com.recetea.core.recipe.domain.vo.RecipeId;

/**
 * Inbound port expressing navigation intent. Controllers issue intents in
 * domain language (e.g. "to recipe detail with this id") without knowing
 * the underlying UI framework, scene graph, or how transitions render.
 *
 * The JavaFX-specific implementation lives at
 * {@code com.recetea.infrastructure.ui.javafx.shared.navigation.NavigationService}.
 *
 * <p><b>ES — </b>Puerto de entrada que expresa la intención de
 * navegación. Los controladores emiten intenciones en lenguaje de
 * dominio (p. ej. "al detalle de receta con este id") sin conocer el
 * framework de UI subyacente, el scene graph o cómo se renderizan las
 * transiciones.
 *
 * <p>La implementación específica de JavaFX vive en
 * {@code com.recetea.infrastructure.ui.javafx.shared.navigation.NavigationService}.
 */
public interface INavigationPort {

    /**
     * Navigate to the login view; clears the active session.
     *
     * <p><b>ES — </b>Navega a la vista de login; limpia la sesión activa.
     */
    void toLogin();

    /**
     * Navigate to the registration view.
     *
     * <p><b>ES — </b>Navega a la vista de registro.
     */
    void toRegister();

    /**
     * Navigate to the main dashboard (paged recipe gallery).
     *
     * <p><b>ES — </b>Navega al dashboard principal (galería de recetas
     * paginada).
     */
    void toDashboard();

    /**
     * Open the recipe form in create mode.
     *
     * <p><b>ES — </b>Abre el formulario de receta en modo creación.
     */
    void toRecipeCreate();

    /**
     * Open the recipe form in update mode pre-populated from {@code recipe}.
     *
     * <p><b>ES — </b>Abre el formulario de receta en modo actualización
     * pre-rellenado desde {@code recipe}.
     */
    void toRecipeUpdate(RecipeDetailResponse recipe);

    /**
     * Open the read-only detail view for {@code id}.
     *
     * <p><b>ES — </b>Abre la vista de detalle de sólo lectura para
     * {@code id}.
     */
    void toRecipeDetail(RecipeId id);

    /**
     * Navigate to the current user's profile (recipes + favorites tabs).
     *
     * <p><b>ES — </b>Navega al perfil del usuario actual (pestañas de
     * recetas + favoritos).
     */
    void toUserProfile();

    /**
     * Tear down the active session and route to login.
     *
     * <p><b>ES — </b>Cierra la sesión activa y enruta al login.
     */
    void logout();
}
