package com.recetea.infrastructure.ui.javafx.shared.notification;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.stage.Popup;
import javafx.stage.Window;
import javafx.util.Duration;

/**
 * Toast-style notification service for non-blocking UI feedback. Three
 * levels — SUCCESS, WARNING, ERROR — drive matching CSS classes
 * ({@code toast-success}, {@code toast-warning}, {@code toast-error}).
 *
 * <p><b>Why a {@link Popup}, not an in-scene overlay.</b> The Popup is
 * owned by the {@link Window} (not the Scene), so it survives a
 * {@code scene.setRoot(...)} swap. That's what makes the
 * "register success → navigate to login" flow work: the toast pops up
 * on the register screen, the navigation swaps the scene root to login,
 * and the toast remains visible overhead.
 *
 * <p><b>CSS quirk.</b> A {@link Popup} carries its own {@link
 * javafx.scene.Scene}; CSS classes added to its content nodes resolve
 * against <em>that</em> scene's stylesheet list, not the owner's. The
 * caller (or this service) must explicitly copy the owner's stylesheets
 * onto the popup's scene before {@code show(...)} or the toast renders
 * unstyled.
 *
 * <p><b>ES — </b>Servicio de notificaciones tipo toast para feedback
 * de UI no bloqueante. Tres niveles — SUCCESS, WARNING, ERROR —
 * disparan las clases CSS correspondientes
 * ({@code toast-success}, {@code toast-warning},
 * {@code toast-error}).
 *
 * <p><b>Por qué un {@link Popup}, no un overlay dentro de la
 * escena.</b> El Popup pertenece al {@link Window} (no a la Scene),
 * así que sobrevive a un intercambio
 * {@code scene.setRoot(...)}. Eso es lo que hace que el flujo
 * "registro exitoso → navegar a login" funcione: el toast aparece
 * en la pantalla de registro, la navegación cambia el root de la
 * escena a login, y el toast permanece visible por encima.
 *
 * <p><b>Curiosidad de CSS.</b> Un {@link Popup} lleva su propia
 * {@link javafx.scene.Scene}; las clases CSS añadidas a sus nodos
 * de contenido se resuelven contra la lista de stylesheets de
 * <em>esa</em> escena, no la del owner. El llamador (o este
 * servicio) debe copiar explícitamente las stylesheets del owner a
 * la escena del popup antes de {@code show(...)}, o el toast
 * renderiza sin estilo.
 */
public final class NotificationService {

    public enum Level { SUCCESS, WARNING, ERROR }

    private static final double WIDTH        = 340;
    private static final double MARGIN_RIGHT = 20;
    private static final double MARGIN_TOP   = 64;

    private NotificationService() {}

    public static void success(Node anchor, String message) { show(anchor, Level.SUCCESS, message); }
    public static void warning(Node anchor, String message) { show(anchor, Level.WARNING, message); }
    public static void error(Node anchor, String message)   { show(anchor, Level.ERROR,   message); }

    public static void show(Node anchor, Level level, String message) {
        if (anchor == null || anchor.getScene() == null) return;
        Window win = anchor.getScene().getWindow();

        String icon = switch (level) {
            case SUCCESS -> "✓  ";
            case WARNING -> "⚠  ";
            case ERROR   -> "✕  ";
        };
        String levelClass = switch (level) {
            case SUCCESS -> "toast-success";
            case WARNING -> "toast-warning";
            case ERROR   -> "toast-error";
        };

        Label lbl = new Label(icon + message);
        lbl.setWrapText(true);
        lbl.getStyleClass().add("toast-label");

        HBox toast = new HBox(lbl);
        toast.setAlignment(Pos.CENTER_LEFT);
        toast.setMinWidth(WIDTH);
        toast.setMaxWidth(WIDTH);
        toast.getStyleClass().addAll("toast", levelClass);

        Popup popup = new Popup();
        popup.setAutoFix(false);
        popup.getContent().add(toast);

        double x = win.getX() + win.getWidth() - WIDTH - MARGIN_RIGHT;
        double y = win.getY() + MARGIN_TOP;
        popup.show(win, x, y);
        // Popup has its own scene — copy the owner's stylesheets so CSS classes resolve.
        popup.getScene().getStylesheets().addAll(win.getScene().getStylesheets());

        FadeTransition fadeIn = new FadeTransition(Duration.millis(250), toast);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);

        FadeTransition fadeOut = new FadeTransition(Duration.millis(350), toast);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        fadeOut.setOnFinished(e -> popup.hide());

        new SequentialTransition(
            fadeIn,
            new PauseTransition(Duration.seconds(3.2)),
            fadeOut
        ).play();
    }
}
