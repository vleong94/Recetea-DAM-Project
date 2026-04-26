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
