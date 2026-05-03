package com.recetea.infrastructure.persistence.recipe.jdbc.repositories;

import com.recetea.core.recipe.application.ports.in.dto.RecipeSummaryResponse;
import com.recetea.core.recipe.application.ports.in.dto.SearchCriteria;
import com.recetea.core.recipe.domain.vo.IngredientId;
import com.recetea.core.shared.application.ports.out.IMetricsPort;
import com.recetea.core.shared.domain.PageRequest;
import com.recetea.core.shared.domain.PageResponse;
import com.recetea.infrastructure.persistence.recipe.jdbc.JdbcTransactionManager;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Search-specialist gateway: extracts the recipe-search predicate building and pagination
 * out of {@link JdbcRecipeRepository}. Mirrors the existing per-table gateways
 * ({@code RecipeIngredientTableGateway}, {@code RecipeStepTableGateway},
 * {@code RecipeMediaTableGateway}) — package-private, extends {@link BaseJdbcRepository}
 * so {@link BaseJdbcRepository#withConnection} preserves connection reuse inside an
 * active transaction and threads query timings through {@code IMetricsPort}.
 *
 * <p>The summary projection ({@code SELECT_SUMMARIES_FROM} / {@code mapSummaryRow}) is shared
 * with the repository through {@link RecipeSummaryRowSupport} so column-name drift cannot
 * diverge between caller paths.
 */
class RecipeSearchGateway extends BaseJdbcRepository {

    RecipeSearchGateway(JdbcTransactionManager transactionManager, IMetricsPort metricsPort) {
        super(transactionManager, metricsPort);
    }

    /**
     * Filtered, paginated summary query. Every nullable / blank field on {@link SearchCriteria}
     * is treated as "absent" → no contribution to the WHERE clause. The two SQL invocations
     * (count + page) reuse the same composed predicate so totals and rows stay consistent
     * within one transactional read.
     */
    PageResponse<RecipeSummaryResponse> searchSummaries(SearchCriteria criteria, PageRequest pageRequest) {
        WhereClause where = buildWhereClause(criteria);

        String countSql = RecipeSqlRegistry.COUNT_RECIPES + where.sql();
        String dataSql  = RecipeSqlRegistry.SEARCH_BASE_QUERY + where.sql() + " LIMIT ? OFFSET ?";

        List<Object> dataParams = new ArrayList<>(where.params());
        dataParams.add(pageRequest.size());
        dataParams.add(pageRequest.offset());

        return withConnection(conn -> {
            long total = RecipeSummaryRowSupport.queryCount(conn, countSql, where.params());
            try (PreparedStatement ps = conn.prepareStatement(dataSql)) {
                for (int i = 0; i < dataParams.size(); i++) ps.setObject(i + 1, dataParams.get(i));
                try (ResultSet rs = ps.executeQuery()) {
                    List<RecipeSummaryResponse> results = new ArrayList<>();
                    while (rs.next()) results.add(RecipeSummaryRowSupport.mapSummaryRow(rs));
                    return PageResponse.of(results, total, pageRequest.size());
                }
            }
        }, "searchSummaries");
    }

    // -------------------------------------------------------------------------
    // Predicate building — pure function over SearchCriteria, no JDBC state
    // -------------------------------------------------------------------------

    private WhereClause buildWhereClause(SearchCriteria criteria) {
        List<Object> params = new ArrayList<>();
        List<String> conditions = new ArrayList<>();

        if (criteria.title() != null && !criteria.title().isBlank()) {
            conditions.add("r.title ILIKE ?");
            params.add("%" + criteria.title().trim() + "%");
        }
        if (criteria.maxPreparationTime() != null) {
            conditions.add("r.prep_time_min <= ?");
            params.add(criteria.maxPreparationTime());
        }
        if (criteria.minServings() != null) {
            conditions.add("r.servings >= ?");
            params.add(criteria.minServings());
        }
        if (criteria.categoryName() != null && !criteria.categoryName().isBlank()) {
            conditions.add("c.name ILIKE ?");
            params.add("%" + criteria.categoryName().trim() + "%");
        }
        if (criteria.difficultyName() != null && !criteria.difficultyName().isBlank()) {
            conditions.add("d.difficulty_level ILIKE ?");
            params.add("%" + criteria.difficultyName().trim() + "%");
        }
        if (criteria.ingredientIds() != null && !criteria.ingredientIds().isEmpty()) {
            // AND-match: a recipe must contain *all* selected ingredients. Counting the
            // distinct matched ids and comparing against the selection size enforces this
            // in a single correlated subquery — keeps WHERE clause composable with the
            // other predicates above.
            String placeholders = criteria.ingredientIds().stream()
                    .map(id -> "?").collect(Collectors.joining(", "));
            conditions.add("EXISTS (SELECT 1 FROM recipe_ingredients ri " +
                    "WHERE ri.recipe_id = r.recipe_id " +
                    "AND ri.ingredient_id IN (" + placeholders + ") " +
                    "GROUP BY ri.recipe_id " +
                    "HAVING COUNT(DISTINCT ri.ingredient_id) = ?)");
            for (IngredientId id : criteria.ingredientIds()) params.add(id.value());
            params.add(criteria.ingredientIds().size());
        }
        if (criteria.authorUsername() != null && !criteria.authorUsername().isBlank()) {
            conditions.add("EXISTS (SELECT 1 FROM users uf " +
                    "WHERE uf.user_id = r.user_id AND uf.username ILIKE ?)");
            params.add("%" + criteria.authorUsername().trim() + "%");
        }
        if (criteria.minScore() != null) {
            conditions.add("r.average_score >= ?");
            params.add(criteria.minScore());
        }

        String sql = conditions.isEmpty() ? "" : " WHERE " + String.join(" AND ", conditions);
        return new WhereClause(sql, params);
    }

    /** Pair of (SQL fragment, ordered bind values) emitted by {@link #buildWhereClause}. */
    private record WhereClause(String sql, List<Object> params) {}
}
