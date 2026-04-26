package com.recetea.infrastructure.ui.javafx.shared.i18n;

import java.text.MessageFormat;
import java.util.ResourceBundle;

public final class I18n {

    private static final ResourceBundle BUNDLE = ResourceBundle.getBundle("i18n.messages");

    private I18n() {}

    public static ResourceBundle bundle() { return BUNDLE; }

    public static String get(String key) { return BUNDLE.getString(key); }

    public static String format(String key, Object... args) {
        return MessageFormat.format(BUNDLE.getString(key), args);
    }
}
