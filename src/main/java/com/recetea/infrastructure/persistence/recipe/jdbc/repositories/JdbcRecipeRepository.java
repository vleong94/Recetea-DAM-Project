package com.recetea.infrastructure.persistence.recipe.jdbc.repositories;

import com.recetea.core.recipe.application.ports.in.dto.RecipeSummaryResponse;
import com.recetea.core.recipe.application.ports.in.dto.SearchCriteria;
import com.recetea.core.recipe.application.ports.out.recipe.IRecipeRepository;
import com.recetea.core.shared.application.ports.out.IMetricsPort;
import com.recetea.core.shared.domain.PageRequest;
import com.recetea.core.shared.domain.PageResponse;
import com.recetea.core.recipe.domain.Rating;
import com.recetea.core.recipe.domain.Recipe;
import com.recetea.core.recipe.domain.vo.*;
import com.recetea.core.user.domain.UserId;
import com.recetea.infrastructure.persistence.recipe.jdbc.JdbcTransactionManager;
import com.recetea.infrastructure.persistence.recipe.jdbc.mappers.RecipeMapper;

import java.math.BigDecimal;
import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Pure orchestrator: every SQL fragment lives in {@link RecipeSqlRegistry}; the
 * three per-table sync gateways own their own DML; the search predicate building
 * lives in {@link RecipeSearchGateway}; the summary projection mapping lives in
 * {@link RecipeSummaryRowSupport}. This class reads as a thin façade over those
 * collaborators — the only logic it owns is connection-binding (via
 * {@link BaseJdbcRepository#withConnection}) and the orchestration of {@code save}
 * / {@code update} across the four child collections.
 */
public class JdbcRecipeRepository extends BaseJdbcRepository implements IRecipeRepository {

    private final RecipeIngredientTableGateway ingredientGateway;
    private final RecipeStepTableGateway stepGateway;
    private final RecipeMediaTableGateway mediaGateway;
    private final RecipeSearchGateway searchGateway;

    public JdbcRecipeRepository(JdbcTransactionManager transactionManager, IMetricsPort metricsPort) {
        super(transactionManager, metricsPort);
        this.ingredientGateway = new RecipeIngredientTableGateway(transactionManager, metricsPort);
        this.stepGateway       = new RecipeStepTableGateway(transactionManager, metricsPort);
        this.mediaGateway      = new RecipeMediaTableGateway(transactionManager, metricsPort);
        this.searchGateway     = new RecipeSearchGateway(transactionManager, metricsPort);
    }

    @Override
    public RecipeId save(Recipe recipe) {
        return withConnection(conn -> {
            RecipeId generatedId;
            try (PreparedStatement ps = conn.prepareStatement(RecipeSqlRegistry.INSERT_RECIPE, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, recipe.getAuthorId().value());
                ps.setInt(2, recipe.getCategory().id().value());
                ps.setInt(3, recipe.getDifficulty().id().value());
                ps.setString(4, recipe.getTitle());
                ps.setString(5, recipe.getDescription());
                ps.setInt(6, recipe.getPreparationTimeMinutes().value());
                ps.setInt(7, recipe.getServings().value());
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (!rs.next()) throw new RuntimeException("No generated key returned after recipe insert.");
                    generatedId = new RecipeId(rs.getInt(1));
                }
            }
            ingredientGateway.insertIngredients(recipe, generatedId);
            stepGateway.insertSteps(recipe, generatedId);
            saveRatings(conn, recipe, generatedId);
            mediaGateway.insertMedia(recipe, generatedId);
            return generatedId;
        }, "save recipe");
    }

    @Override
    public void update(Recipe recipe) {
        withConnection(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(RecipeSqlRegistry.UPDATE_RECIPE)) {
                ps.setInt(1, recipe.getCategory().id().value());
                ps.setInt(2, recipe.getDifficulty().id().value());
                ps.setString(3, recipe.getTitle());
                ps.setString(4, recipe.getDescription());
                ps.setInt(5, recipe.getPreparationTimeMinutes().value());
                ps.setInt(6, recipe.getServings().value());
                ps.setInt(7, recipe.getId().value());
                ps.executeUpdate();
            }
            ingredientGateway.syncIngredients(recipe);
            stepGateway.syncSteps(recipe);
            mediaGateway.syncMedia(recipe);
            // Recipe is now an immutable record, so the previous "metricsDirty" flag is gone.
            // saveRatings is unconditional: ratings are upserted (idempotent) and metrics are
            // overwritten. For metadata-only updates this re-touches every existing rating row,
            // which is harmless because the upserts produce the same values that were already
            // in the table — the steady-state cost is one batched round-trip per update.
            saveRatings(conn, recipe, recipe.getId());
            return null;
        }, "update recipe id=" + recipe.getId().value());
    }

    @Override
    public void delete(RecipeId id) {
        withConnection(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(RecipeSqlRegistry.DELETE_RECIPE)) {
                ps.setInt(1, id.value());
                ps.executeUpdate();
            }
            return null;
        }, "delete recipe id=" + id.value());
    }

    @Override
    public void updateSocialMetrics(RecipeId id, BigDecimal averageScore, int totalRatings) {
        withConnection(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(RecipeSqlRegistry.UPDATE_SOCIAL_METRICS)) {
                ps.setBigDecimal(1, averageScore);
                ps.setInt(2, totalRatings);
                ps.setInt(3, id.value());
                ps.executeUpdate();
            }
            return null;
        }, "updateSocialMetrics recipe id=" + id.value());
    }

    private void saveRatings(Connection conn, Recipe recipe, RecipeId id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(RecipeSqlRegistry.UPSERT_RATING)) {
            for (Rating rating : recipe.getRatings()) {
                ps.setInt(1, rating.userId().value());
                ps.setInt(2, id.value());
                ps.setInt(3, rating.score().value());
                ps.setString(4, rating.comment());
                ps.setObject(5, rating.createdAt());
                ps.addBatch();
            }
            ps.executeBatch();
        }
        try (PreparedStatement ps = conn.prepareStatement(RecipeSqlRegistry.UPDATE_SOCIAL_METRICS)) {
            ps.setBigDecimal(1, recipe.getAverageScore());
            ps.setInt(2, recipe.getTotalRatings());
            ps.setInt(3, id.value());
            ps.executeUpdate();
        }
    }

    // -------------------------------------------------------------------------
    // Query methods
    // -------------------------------------------------------------------------

    @Override
    public Optional<Recipe> findById(RecipeId id) {
        return withConnection(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(RecipeSqlRegistry.SELECT_FULL_AGGREGATE)) {
                ps.setInt(1, id.value());
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) return Optional.empty();
                    // Read every JSONB-aggregated child collection up-front so the immutable
                    // Recipe record receives all 14 components in a single constructor call.
                    RecipeId rid = new RecipeId(rs.getInt("recipe_id"));
                    var ingredients = RecipeMapper.mapIngredientsJson(rs.getString("ingredients_json"));
                    var steps       = RecipeMapper.mapStepsJson(rs.getString("steps_json"));
                    var ratings     = RecipeMapper.mapRatingsJson(rs.getString("ratings_json"));
                    var media       = RecipeMapper.mapMediaJson(rs.getString("media_json"), rid);
                    Recipe recipe = RecipeMapper.mapRow(rs, ingredients, steps, ratings, media);
                    return Optional.of(recipe);
                }
            }
        }, "findById id=" + id.value());
    }

    @Override
    public PageResponse<RecipeSummaryResponse> findAllSummaries(PageRequest pageRequest) {
        return withConnection(conn -> {
            long total = RecipeSummaryRowSupport.queryCount(conn, RecipeSqlRegistry.COUNT_RECIPES, List.of());
            String sql = RecipeSqlRegistry.SELECT_SUMMARIES + " LIMIT ? OFFSET ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, pageRequest.size());
                ps.setInt(2, pageRequest.offset());
                try (ResultSet rs = ps.executeQuery()) {
                    List<RecipeSummaryResponse> results = new ArrayList<>();
                    while (rs.next()) results.add(RecipeSummaryRowSupport.mapSummaryRow(rs));
                    return PageResponse.of(results, total, pageRequest.size());
                }
            }
        }, "findAllSummaries");
    }

    @Override
    public PageResponse<RecipeSummaryResponse> searchSummaries(SearchCriteria criteria, PageRequest pageRequest) {
        return searchGateway.searchSummaries(criteria, pageRequest);
    }

    @Override
    public List<String> suggestTitles(String prefix, int limit) {
        if (prefix == null || prefix.isBlank() || limit <= 0) return List.of();
        return withConnection(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(RecipeSqlRegistry.SUGGEST_TITLES)) {
                ps.setString(1, prefix.trim() + "%");
                ps.setInt(2, limit);
                try (ResultSet rs = ps.executeQuery()) {
                    List<String> results = new ArrayList<>();
                    while (rs.next()) results.add(rs.getString(1));
                    return results;
                }
            }
        }, "suggestTitles");
    }

    @Override
    public List<RecipeSummaryResponse> findSummariesByIds(List<RecipeId> ids) {
        if (ids.isEmpty()) return List.of();
        String placeholders = ids.stream().map(id -> "?").collect(Collectors.joining(", "));
        String sql = RecipeSqlRegistry.SELECT_SUMMARIES + " WHERE r.recipe_id IN (" + placeholders + ")";
        return withConnection(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (int i = 0; i < ids.size(); i++) ps.setInt(i + 1, ids.get(i).value());
                try (ResultSet rs = ps.executeQuery()) {
                    List<RecipeSummaryResponse> results = new ArrayList<>();
                    while (rs.next()) results.add(RecipeSummaryRowSupport.mapSummaryRow(rs));
                    return results;
                }
            }
        }, "findSummariesByIds");
    }

    @Override
    public boolean hasUserRatedRecipe(UserId userId, RecipeId recipeId) {
        return withConnection(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(RecipeSqlRegistry.COUNT_USER_RATING)) {
                ps.setInt(1, userId.value());
                ps.setInt(2, recipeId.value());
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    return rs.getLong(1) > 0;
                }
            }
        }, "hasUserRatedRecipe");
    }

    @Override
    public PageResponse<RecipeSummaryResponse> findByAuthorId(UserId authorId, PageRequest page) {
        return withConnection(conn -> {
            long total = RecipeSummaryRowSupport.queryCount(
                    conn, RecipeSqlRegistry.COUNT_RECIPES + " WHERE r.user_id = ?", List.of(authorId.value()));
            String sql = RecipeSqlRegistry.SELECT_SUMMARIES + " WHERE r.user_id = ? LIMIT ? OFFSET ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, authorId.value());
                ps.setInt(2, page.size());
                ps.setInt(3, page.offset());
                try (ResultSet rs = ps.executeQuery()) {
                    List<RecipeSummaryResponse> results = new ArrayList<>();
                    while (rs.next()) results.add(RecipeSummaryRowSupport.mapSummaryRow(rs));
                    return PageResponse.of(results, total, page.size());
                }
            }
        }, "findByAuthorId userId=" + authorId.value());
    }

}
