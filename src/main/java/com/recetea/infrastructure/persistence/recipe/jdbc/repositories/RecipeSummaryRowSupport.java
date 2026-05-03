package com.recetea.infrastructure.persistence.recipe.jdbc.repositories;

import com.recetea.core.recipe.application.ports.in.dto.RecipeSummaryResponse;
import com.recetea.core.recipe.domain.vo.IngredientId;
import com.recetea.core.recipe.domain.vo.RecipeId;
import com.recetea.core.user.domain.UserId;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Shared SQL templates and row-mapping helpers for the {@link RecipeSummaryResponse}
 * projection. Owned jointly by {@link JdbcRecipeRepository} (for {@code findAllSummaries},
 * {@code findSummariesByIds}, {@code findByAuthorId}) and {@link RecipeSearchGateway}
 * (for {@code searchSummaries}). Single source of truth for the summary column list and
 * row hydration — column-name drift cannot diverge between repo and gateway.
 *
 * <p>Final + private constructor + static methods: it's a value namespace, never instantiated.
 */
final class RecipeSummaryRowSupport {

    /**
     * Summary projection — alias to {@link RecipeSqlRegistry#SELECT_SUMMARIES}. Kept under
     * the original name so existing repository call sites continue to compile, but the SQL
     * itself lives in the central registry.
     */
    static final String SELECT_SUMMARIES_FROM = RecipeSqlRegistry.SELECT_SUMMARIES;

    /** Count base — alias to {@link RecipeSqlRegistry#COUNT_RECIPES}. */
    static final String COUNT_FROM = RecipeSqlRegistry.COUNT_RECIPES;

    private RecipeSummaryRowSupport() {}

    /**
     * Single-row scalar count helper. Caller supplies an already-built parameterised SQL
     * (typically {@code COUNT_FROM + whereClause}) and the matching parameter list.
     */
    static long queryCount(Connection conn, String sql, List<Object> params) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.size(); i++) ps.setObject(i + 1, params.get(i));
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }

    /**
     * Hydrates one row of the {@link #SELECT_SUMMARIES_FROM} projection into a DTO. Reads the
     * denormalised {@code average_score} / {@code total_ratings} columns directly — no JOIN to
     * {@code ratings}.
     */
    static RecipeSummaryResponse mapSummaryRow(ResultSet rs) throws SQLException {
        return new RecipeSummaryResponse(
                new RecipeId(rs.getInt("recipe_id")),
                rs.getString("title"),
                rs.getString("category_name"),
                rs.getString("difficulty_name"),
                rs.getInt("prep_time_min"),
                rs.getInt("servings"),
                Optional.ofNullable(rs.getBigDecimal("average_score")).orElse(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP),
                rs.getInt("total_ratings"),
                rs.getString("main_media_storage_key"),
                new UserId(rs.getInt("user_id")),
                rs.getString("author_username"),
                mapIngredientIds(rs.getArray("ingredient_ids")));
    }

    private static List<IngredientId> mapIngredientIds(java.sql.Array sqlArray) throws SQLException {
        if (sqlArray == null) return List.of();
        try {
            Object raw = sqlArray.getArray();
            if (!(raw instanceof Integer[] ids) || ids.length == 0) return List.of();
            List<IngredientId> result = new ArrayList<>(ids.length);
            for (Integer id : ids) {
                if (id != null) result.add(new IngredientId(id));
            }
            return result;
        } finally {
            sqlArray.free();
        }
    }
}
