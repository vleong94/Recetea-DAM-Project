package com.recetea.core.shared.domain;

/**
 * Page coordinates for any paged read. Zero-based ({@code page=0} is the
 * first page) to match the SQL-friendly {@link #offset()} computation —
 * {@code OFFSET page * size}.
 *
 * <p>The compact constructor enforces structural sanity ({@code page >= 0},
 * {@code size >= 1}) at the boundary so SQL never sees a negative offset
 * or a zero-row LIMIT.
 *
 * <p><b>ES — </b>Coordenadas de página para cualquier lectura paginada.
 * Empieza en cero ({@code page=0} es la primera página) para encajar con
 * el cálculo amigable para SQL de {@link #offset()} —
 * {@code OFFSET page * size}.
 *
 * <p>El constructor compacto aplica sanidad estructural
 * ({@code page >= 0}, {@code size >= 1}) en la frontera, de modo que SQL
 * nunca ve un offset negativo ni un LIMIT de cero filas.
 */
public record PageRequest(int page, int size) {

    public PageRequest {
        if (page < 0) throw new IllegalArgumentException("page must be >= 0");
        if (size < 1) throw new IllegalArgumentException("size must be >= 1");
    }

    /** SQL-friendly offset for use with {@code OFFSET ? LIMIT ?}. */
    public int offset() { return page * size; }
}
