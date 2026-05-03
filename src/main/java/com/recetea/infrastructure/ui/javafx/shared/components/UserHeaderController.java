package com.recetea.infrastructure.ui.javafx.shared.components;

import com.recetea.core.shared.application.ports.in.INavigationPort;
import com.recetea.core.shared.application.ports.in.IUserSessionService;
import com.recetea.infrastructure.ui.javafx.shared.i18n.I18n;
import javafx.fxml.FXML;
import javafx.scene.control.MenuItem;

/**
 * Shared top-right hamburger menu surfaced via {@code <fx:include>} on every page that
 * carries a logged-in session (currently the dashboard and the user workspace).
 *
 * <p>The host page provides the runtime context — session service, navigation port, and
 * a {@link Mode} that says which page we're rendered on — so the menu can show only the
 * cross-navigation item that points away from the current page. Layout / placement is
 * left entirely to the host FXML.
 *
 * <p>Hidden {@code MenuItem}s don't reserve space in the popup skin, so no
 * {@code managedProperty} is needed (MenuItem isn't a {@code Node} — JavaFX's popup
 * factory simply skips invisible items without leaving a gap or doubling separators).
 */
public class UserHeaderController {

    /** Identifies the page hosting this header so the menu can adapt its item set. */
    public enum Mode {
        /** Recipe dashboard / catalog. Menu shows only the cross-nav to the user profile (i18n key {@code nav.profile}). */
        DASHBOARD,
        /** User workspace / profile. Menu shows only the cross-nav to the catalog (i18n key {@code nav.catalog}). */
        WORKSPACE
    }

    @FXML private MenuItem usernameMenuItem;
    @FXML private MenuItem profileMenuItem;
    @FXML private MenuItem catalogMenuItem;
    @FXML private MenuItem logoutMenuItem;

    private INavigationPort nav;

    public void init(IUserSessionService sessionService, INavigationPort nav, Mode mode) {
        this.nav = nav;

        String username = sessionService.getCurrentUsername().orElse("");
        usernameMenuItem.setText(I18n.format("dashboard.menu.loggedInAs", username));

        // Exclusive visibility: only the destination item is shown. The current-page item
        // disappears entirely (not greyed out) so the menu surfaces only forward actions.
        boolean onWorkspace = mode == Mode.WORKSPACE;
        profileMenuItem.setVisible(!onWorkspace);
        catalogMenuItem.setVisible(onWorkspace);
    }

    @FXML private void onProfileClick() { nav.toUserProfile(); }
    @FXML private void onCatalogClick() { nav.toDashboard(); }
    @FXML private void onLogoutClick()  { nav.logout(); }
}
