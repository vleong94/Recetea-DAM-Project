package com.recetea.core.shared.domain;

import java.util.List;

/**
 * Generic envelope for a paged read. Carries the rows for the current
 * page plus the total counts so the UI can render "Page X of Y" without
 * a second query.
 *
 * <p>{@link #of(List, long, int)} computes {@code totalPages} via
 * {@code Math.ceil} so a partial last page rounds up correctly. The
 * factory takes the page size (not the request) because the totals are
 * orthogonal to how the read was requested.
 *
 * <p><b>ES — </b>Envoltorio genérico para una lectura paginada. Lleva las
 * filas de la página actual más los totales para que la UI pueda
 * renderizar "Página X de Y" sin una segunda consulta.
 *
 * <p>{@link #of(List, long, int)} calcula {@code totalPages} mediante
 * {@code Math.ceil}, de modo que una última página parcial se redondea
 * correctamente hacia arriba. La factoría recibe el tamaño de página (no
 * la request) porque los totales son ortogonales a cómo se solicitó la
 * lectura.
 */
public record PageResponse<T>(List<T> content, long totalElements, int totalPages) {

    /** Convenience factory: derive {@code totalPages} from {@code totalElements / pageSize}. */
    public static <T> PageResponse<T> of(List<T> content, long totalElements, int pageSize) {
        int totalPages = (int) Math.ceil((double) totalElements / pageSize);
        return new PageResponse<>(content, totalElements, totalPages);
    }
}
