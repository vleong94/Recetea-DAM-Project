package com.recetea.core.shared.domain.utils;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Formats a past {@link LocalDateTime} into a localised "N minutes ago"
 * string. Pure function over the input — no I/O, no global state.
 *
 * <p>The two function arguments ({@code simple}, {@code withCount}) inject
 * the i18n lookup so this utility stays inside {@code core.shared.domain}
 * (no UI dependency). Callers in the JavaFX layer pass
 * {@code I18n::get} and {@code I18n::format}.
 *
 * <p>Bucket boundaries match standard relative-time UX (1 min / 60 min /
 * 24 hr / 30 d / 12 mo) — calendar-imprecise but acceptable for a
 * "rated 3 hours ago" label.
 *
 * <p><b>ES — </b>Formatea un {@link LocalDateTime} pasado en una
 * cadena localizada del estilo "hace N minutos". Función pura sobre
 * la entrada — sin E/S, sin estado global.
 *
 * <p>Los dos argumentos de función ({@code simple},
 * {@code withCount}) inyectan la búsqueda i18n para que esta
 * utilidad quede dentro de {@code core.shared.domain} (sin
 * dependencia de UI). Los llamadores en la capa de JavaFX pasan
 * {@code I18n::get} y {@code I18n::format}.
 *
 * <p>Los límites de los buckets coinciden con la UX estándar de
 * tiempo relativo (1 min / 60 min / 24 h / 30 d / 12 meses) —
 * impreciso a nivel de calendario pero aceptable para una etiqueta
 * "valorado hace 3 horas".
 */
public final class RelativeTimeFormatter {

    private RelativeTimeFormatter() {}

    /**
     * Returns a human-readable relative time string (e.g. "5 min ago") for the given date.
     *
     * @param date        the past date to format; {@code null} returns an empty string.
     * @param simple      resolves a bare i18n key to its localised string.
     * @param withCount   resolves a key + numeric argument to its localised string.
     */
    public static String format(LocalDateTime date,
                                Function<String, String> simple,
                                BiFunction<String, Long, String> withCount) {
        if (date == null) return "";
        long minutes = ChronoUnit.MINUTES.between(date, LocalDateTime.now());
        if (minutes < 1)  return simple.apply("time.now");
        if (minutes < 60) return withCount.apply("time.ago.minutes", minutes);
        long hours = minutes / 60;
        if (hours < 24)   return withCount.apply("time.ago.hours",  hours);
        long days = hours / 24;
        if (days < 30)    return withCount.apply("time.ago.days",   days);
        long months = days / 30;
        if (months < 12)  return withCount.apply("time.ago.months", months);
        long years = months / 12;
        return             withCount.apply("time.ago.years",  years);
    }
}
