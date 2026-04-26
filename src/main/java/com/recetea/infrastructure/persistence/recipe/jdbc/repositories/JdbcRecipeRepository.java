package com.recetea.infrastructure.persistence.recipe.jdbc.repositories;

import com.recetea.core.recipe.application.ports.in.dto.RecipeSummaryResponse;
import com.recetea.core.recipe.application.ports.in.dto.SearchCriteria;
import com.recetea.core.recipe.application.ports.out.recipe.IRecipeRepository;
import com.recetea.core.shared.domain.PageRequest;
import com.recetea.core.shared.domain.PageResponse;
import com.recetea.core.recipe.domain.Rating;
import com.recetea.core.recipe.domain.Recipe;
import com.recetea.core.recipe.domain.RecipeIngredient;
import com.recetea.core.recipe.domain.RecipeMedia;
import com.recetea.core.recipe.domain.RecipeStep;
import com.recetea.core.recipe.domain.vo.*;
import com.recetea.core.user.domain.UserId;
import com.recetea.infrastructure.persistence.recipe.jdbc.InfrastructureException;
import com.recetea.infrastructure.persistence.recipe.jdbc.JdbcTransactionManager;
import com.recetea.infrastructure.persistence.recipe.jdbc.mappers.RecipeMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

public class JdbcRecipeRepository extends BaseJdbcRepository implements IRecipeRepository {

    private static final String INSERT_RECIPE =
            "INSERT INTO recipes (user_id, category_id, difficulty_id, title, description, prep_time_min, servings) VALUES (?, ?, ?, ?, ?, ?, ?)";
    private static final String UPDATE_RECIPE =
            "UPDATE recipes SET category_id = ?, difficulty_id = ?, title = ?, description = ?, prep_time_min = ?, servings = ? WHERE id_recipe = ?";
    private static final String DELETE_RECIPE =
            "DELETE FROM recipes WHERE id_recipe = ?";

    private static final String SELECT_INGREDIENTS =
            "SELECT ingredient_id, unit_id, quantity FROM recipe_ingredients WHERE recipe_id = ?";
    private static final String INSERT_INGREDIENT =
            "INSERT INTO recipe_ingredients (recipe_id, ingredient_id, unit_id, quantity) VALUES (?, ?, ?, ?)";
    private static final String UPDATE_INGREDIENT =
            "UPDATE recipe_ingredients SET unit_id = ?, quantity = ? WHERE recipe_id = ? AND ingredient_id = ?";
    private static final String DELETE_INGREDIENT =
            "DELETE FROM recipe_ingredients WHERE recipe_id = ? AND ingredient_id = ?";

    private static final String SELECT_STEPS =
            "SELECT step_order, instruction FROM steps WHERE recipe_id = ?";
    private static final String INSERT_STEP =
            "INSERT INTO steps (recipe_id, step_order, instruction) VALUES (?, ?, ?)";
    private static final String UPDATE_STEP =
            "UPDATE steps SET instruction = ? WHERE recipe_id = ? AND step_order = ?";
    private static final String DELETE_STEP =
            "DELETE FROM steps WHERE recipe_id = ? AND step_order = ?";

    private static final String COUNT_USER_RATING =
            "SELECT COUNT(*) FROM ratings WHERE user_id = ? AND recipe_id = ?";
    private static final String UPSERT_RATING =
            "INSERT INTO ratings (user_id, recipe_id, score, comment, created_at) VALUES (?, ?, ?, ?, ?) " +
            "ON CONFLICT (user_id, recipe_id) DO UPDATE SET score = EXCLUDED.score, comment = EXCLUDED.comment";

    private static final String UPDATE_SOCIAL_METRICS =
            "UPDATE recipes SET average_score = ?, total_ratings = ? WHERE id_recipe = ?";

    private static final String SELECT_FULL_AGGREGATE = """
            SELECT
                r.*,
                c.name       AS category_name,
                d.level_name AS difficulty_level,
                ing_agg.ingredients_json,
                step_agg.steps_json,
                rat_agg.ratings_json,
                media_agg.media_json
            FROM recipes r
            LEFT JOIN categories c   ON r.category_id  = c.id_category
            LEFT JOIN difficulties d ON r.difficulty_id = d.id_difficulty
            LEFT JOIN LATERAL (
                SELECT COALESCE(JSONB_AGG(JSONB_BUILD_OBJECT(
                    'ingredient_id', ri.ingredient_id,
                    'unit_id',       ri.unit_id,
                    'quantity',      ri.quantity,
                    'ing_name',      i.name,
                    'unit_abbr',     u.abbreviation
                ) ORDER BY ri.ingredient_id), '[]'::jsonb) AS ingredients_json
                FROM recipe_ingredients ri
                INNER JOIN ingredients i   ON ri.ingredient_id = i.id_ingredient
                INNER JOIN unit_measures u ON ri.unit_id       = u.id_unit
                WHERE ri.recipe_id = r.id_recipe
            ) ing_agg ON true
            LEFT JOIN LATERAL (
                SELECT COALESCE(JSONB_AGG(JSONB_BUILD_OBJECT(
                    'step_order',  s.step_order,
                    'instruction', s.instruction
                ) ORDER BY s.step_order), '[]'::jsonb) AS steps_json
                FROM steps s
                WHERE s.recipe_id = r.id_recipe
            ) step_agg ON true
            LEFT JOIN LATERAL (
                SELECT COALESCE(JSONB_AGG(JSONB_BUILD_OBJECT(
                    'user_id',    rat.user_id,
                    'username',   u.username,
                    'score',      rat.score,
                    'comment',    rat.comment,
                    'created_at', rat.created_at
                ) ORDER BY rat.created_at ASC), '[]'::jsonb) AS ratings_json
                FROM ratings rat
                LEFT JOIN users u ON u.id_user = rat.user_id
                WHERE rat.recipe_id = r.id_recipe
            ) rat_agg ON true
            LEFT JOIN LATERAL (
                SELECT COALESCE(JSONB_AGG(JSONB_BUILD_OBJECT(
                    'id_media',         rm.id_media,
                    'storage_key',      rm.storage_key,
                    'storage_provider', rm.storage_provider,
                    'mime_type',        rm.mime_type,
                    'size_bytes',       rm.size_bytes,
                    'is_main',          rm.is_main,
                    'sort_order',       rm.sort_order
                ) ORDER BY rm.sort_order), '[]'::jsonb) AS media_json
                FROM recipe_media rm
                WHERE rm.recipe_id = r.id_recipe
            ) media_agg ON true
            WHERE r.id_recipe = ?
            """;

    private static final String SELECT_MEDIA_FOR_DIFF =
            "SELECT id_media, is_main, sort_order FROM recipe_media WHERE recipe_id = ?";
    private static final String INSERT_MEDIA =
            "INSERT INTO recipe_media (recipe_id, storage_key, storage_provider, mime_type, size_bytes, is_main, sort_order) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?)";
    private static final String UPDATE_MEDIA =
            "UPDATE recipe_media SET is_main = ?, sort_order = ? WHERE id_media = ?";
    private static final String DELETE_MEDIA_ITEM =
            "DELETE FROM recipe_media WHERE id_media = ?";

    private static final String SELECT_SUMMARIES_FROM =
            "SELECT r.id_recipe, r.user_id, r.title, c.name AS category_name, d.level_name AS difficulty_name, " +
            "r.prep_time_min, r.servings, r.average_score, r.total_ratings, " +
            "rm.storage_key AS main_media_storage_key, u.username AS author_username " +
            "FROM recipes r " +
            "INNER JOIN categories c ON r.category_id = c.id_category " +
            "INNER JOIN difficulties d ON r.difficulty_id = d.id_difficulty " +
            "LEFT JOIN recipe_media rm ON rm.recipe_id = r.id_recipe AND rm.is_main = true " +
            "LEFT JOIN users u ON u.id_user = r.user_id";
    private static final String COUNT_FROM =
            "SELECT count(*) FROM recipes r " +
            "INNER JOIN categories c ON r.category_id = c.id_category " +
            "INNER JOIN difficulties d ON r.difficulty_id = d.id_difficulty";

    public JdbcRecipeRepository(JdbcTransactionManager transactionManager) {
        super(transactionManager);
    }

    @Override
    public void save(Recipe recipe) {
        Connection conn = null;
        try {
            conn = transactionManager.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(INSERT_RECIPE, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, recipe.getAuthorId().value());
                ps.setInt(2, recipe.getCategory().getId().value());
                ps.setInt(3, recipe.getDifficulty().getId().value());
                ps.setString(4, recipe.getTitle());
                ps.setString(5, recipe.getDescription());
                ps.setInt(6, recipe.getPreparationTimeMinutes().value());
                ps.setInt(7, recipe.getServings().value());
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) recipe.setId(new RecipeId(rs.getInt(1)));
                }
            }
            insertIngredients(conn, recipe);
            insertSteps(conn, recipe);
            saveRatings(conn, recipe);
            insertMedia(conn, recipe);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to persist new recipe.", e);
        } finally {
            closeIfNonTransactional(conn, "save recipe");
        }
    }

    @Override
    public void update(Recipe recipe) {
        Connection conn = null;
        try {
            conn = transactionManager.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(UPDATE_RECIPE)) {
                ps.setInt(1, recipe.getCategory().getId().value());
                ps.setInt(2, recipe.getDifficulty().getId().value());
                ps.setString(3, recipe.getTitle());
                ps.setString(4, recipe.getDescription());
                ps.setInt(5, recipe.getPreparationTimeMinutes().value());
                ps.setInt(6, recipe.getServings().value());
                ps.setInt(7, recipe.getId().value());
                ps.executeUpdate();
            }
            syncIngredients(conn, recipe);
            syncSteps(conn, recipe);
            syncMedia(conn, recipe);
            if (recipe.isMetricsDirty()) {
                saveRatings(conn, recipe);
                recipe.clearMetricsDirty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update recipe ID: " + recipe.getId().value(), e);
        } finally {
            closeIfNonTransactional(conn, "update recipe id=" + recipe.getId().value());
        }
    }

    @Override
    public void delete(RecipeId id) {
        Connection conn = null;
        try {
            conn = transactionManager.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(DELETE_RECIPE)) {
                ps.setInt(1, id.value());
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete recipe.", e);
        } finally {
            closeIfNonTransactional(conn, "delete recipe id=" + id.value());
        }
    }

    @Override
    public void updateSocialMetrics(RecipeId id, BigDecimal averageScore, int totalRatings) {
        Connection conn = null;
        try {
            conn = transactionManager.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(UPDATE_SOCIAL_METRICS)) {
                ps.setBigDecimal(1, averageScore);
                ps.setInt(2, totalRatings);
                ps.setInt(3, id.value());
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            throw new InfrastructureException("Failed to update social metrics for recipe ID: " + id.value(), e);
        } finally {
            closeIfNonTransactional(conn, "updateSocialMetrics recipe id=" + id.value());
        }
    }

    // -------------------------------------------------------------------------
    // Smart diffing for update
    // -------------------------------------------------------------------------

    private void syncIngredients(Connection conn, Recipe recipe) throws SQLException {
        int recipeId = recipe.getId().value();

        Map<Integer, DbIngredientRow> existing = new LinkedHashMap<>();
        try (PreparedStatement ps = conn.prepareStatement(SELECT_INGREDIENTS)) {
            ps.setInt(1, recipeId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int ingId = rs.getInt("ingredient_id");
                    existing.put(ingId, new DbIngredientRow(ingId, rs.getInt("unit_id"), rs.getBigDecimal("quantity")));
                }
            }
        }

        Map<Integer, RecipeIngredient> incoming = new LinkedHashMap<>();
        for (RecipeIngredient ri : recipe.getIngredients()) {
            incoming.put(ri.getIngredientId().value(), ri);
        }

        try (PreparedStatement ps = conn.prepareStatement(DELETE_INGREDIENT)) {
            for (int ingId : existing.keySet()) {
                if (!incoming.containsKey(ingId)) {
                    ps.setInt(1, recipeId);
                    ps.setInt(2, ingId);
                    ps.addBatch();
                }
            }
            ps.executeBatch();
        }

        try (PreparedStatement ps = conn.prepareStatement(INSERT_INGREDIENT)) {
            for (RecipeIngredient ri : recipe.getIngredients()) {
                if (!existing.containsKey(ri.getIngredientId().value())) {
                    ps.setInt(1, recipeId);
                    ps.setInt(2, ri.getIngredientId().value());
                    ps.setInt(3, ri.getUnitId().value());
                    ps.setBigDecimal(4, ri.getQuantity().setScale(2, RoundingMode.HALF_UP));
                    ps.addBatch();
                }
            }
            ps.executeBatch();
        }

        try (PreparedStatement ps = conn.prepareStatement(UPDATE_INGREDIENT)) {
            for (RecipeIngredient ri : recipe.getIngredients()) {
                DbIngredientRow row = existing.get(ri.getIngredientId().value());
                if (row != null && row.isDifferentFrom(ri)) {
                    ps.setInt(1, ri.getUnitId().value());
                    ps.setBigDecimal(2, ri.getQuantity().setScale(2, RoundingMode.HALF_UP));
                    ps.setInt(3, recipeId);
                    ps.setInt(4, ri.getIngredientId().value());
                    ps.addBatch();
                }
            }
            ps.executeBatch();
        }
    }

    private void syncSteps(Connection conn, Recipe recipe) throws SQLException {
        int recipeId = recipe.getId().value();

        Map<Integer, String> existing = new LinkedHashMap<>();
        try (PreparedStatement ps = conn.prepareStatement(SELECT_STEPS)) {
            ps.setInt(1, recipeId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    existing.put(rs.getInt("step_order"), rs.getString("instruction"));
                }
            }
        }

        Map<Integer, RecipeStep> incoming = new LinkedHashMap<>();
        for (RecipeStep s : recipe.getSteps()) {
            incoming.put(s.stepOrder(), s);
        }

        try (PreparedStatement ps = conn.prepareStatement(DELETE_STEP)) {
            for (int order : existing.keySet()) {
                if (!incoming.containsKey(order)) {
                    ps.setInt(1, recipeId);
                    ps.setInt(2, order);
                    ps.addBatch();
                }
            }
            ps.executeBatch();
        }

        try (PreparedStatement ps = conn.prepareStatement(INSERT_STEP)) {
            for (RecipeStep s : recipe.getSteps()) {
                if (!existing.containsKey(s.stepOrder())) {
                    ps.setInt(1, recipeId);
                    ps.setInt(2, s.stepOrder());
                    ps.setString(3, s.instruction());
                    ps.addBatch();
                }
            }
            ps.executeBatch();
        }

        try (PreparedStatement ps = conn.prepareStatement(UPDATE_STEP)) {
            for (RecipeStep s : recipe.getSteps()) {
                String existingInstruction = existing.get(s.stepOrder());
                if (existingInstruction != null && !existingInstruction.equals(s.instruction())) {
                    ps.setString(1, s.instruction());
                    ps.setInt(2, recipeId);
                    ps.setInt(3, s.stepOrder());
                    ps.addBatch();
                }
            }
            ps.executeBatch();
        }
    }

    private void saveRatings(Connection conn, Recipe recipe) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(UPSERT_RATING)) {
            for (Rating rating : recipe.getRatings()) {
                ps.setInt(1, rating.getUserId().value());
                ps.setInt(2, recipe.getId().value());
                ps.setInt(3, rating.getScore().value());
                ps.setString(4, rating.getComment());
                ps.setObject(5, rating.getCreatedAt());
                ps.addBatch();
            }
            ps.executeBatch();
        }
        try (PreparedStatement ps = conn.prepareStatement(UPDATE_SOCIAL_METRICS)) {
            ps.setBigDecimal(1, recipe.getAverageScore());
            ps.setInt(2, recipe.getTotalRatings());
            ps.setInt(3, recipe.getId().value());
            ps.executeUpdate();
        }
    }

    // -------------------------------------------------------------------------
    // Bulk insert helpers for save()
    // -------------------------------------------------------------------------

    private void insertIngredients(Connection conn, Recipe recipe) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(INSERT_INGREDIENT)) {
            for (RecipeIngredient ing : recipe.getIngredients()) {
                ps.setInt(1, recipe.getId().value());
                ps.setInt(2, ing.getIngredientId().value());
                ps.setInt(3, ing.getUnitId().value());
                ps.setBigDecimal(4, ing.getQuantity().setScale(2, RoundingMode.HALF_UP));
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private void insertSteps(Connection conn, Recipe recipe) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(INSERT_STEP)) {
            for (RecipeStep step : recipe.getSteps()) {
                ps.setInt(1, recipe.getId().value());
                ps.setInt(2, step.stepOrder());
                ps.setString(3, step.instruction());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    // -------------------------------------------------------------------------
    // Query methods
    // -------------------------------------------------------------------------

    @Override
    public Optional<Recipe> findById(RecipeId id) {
        Connection conn = null;
        try {
            conn = transactionManager.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(SELECT_FULL_AGGREGATE)) {
                ps.setInt(1, id.value());
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) return Optional.empty();
                    Recipe recipe = RecipeMapper.mapRow(rs);
                    recipe.syncIngredients(RecipeMapper.mapIngredientsJson(rs.getString("ingredients_json")));
                    recipe.syncSteps(RecipeMapper.mapStepsJson(rs.getString("steps_json")));
                    for (Rating rating : RecipeMapper.mapRatingsJson(rs.getString("ratings_json"))) {
                        recipe.hydrateRating(rating);
                    }
                    for (RecipeMedia media : RecipeMapper.mapMediaJson(rs.getString("media_json"), recipe.getId())) {
                        recipe.hydrateMedia(media);
                    }
                    return Optional.of(recipe);
                }
            }
        } catch (SQLException e) {
            throw new InfrastructureException("Failed to load recipe ID: " + id.value(), e);
        } finally {
            closeIfNonTransactional(conn, "findById id=" + id.value());
        }
    }

    @Override
    public PageResponse<RecipeSummaryResponse> findAllSummaries(PageRequest pageRequest) {
        Connection conn = null;
        try {
            conn = transactionManager.getConnection();
            long total = queryCount(conn, COUNT_FROM, List.of());
            String sql = SELECT_SUMMARIES_FROM + " LIMIT ? OFFSET ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, pageRequest.size());
                ps.setInt(2, pageRequest.offset());
                try (ResultSet rs = ps.executeQuery()) {
                    List<RecipeSummaryResponse> results = new ArrayList<>();
                    while (rs.next()) results.add(mapSummaryRow(rs));
                    return PageResponse.of(results, total, pageRequest.size());
                }
            }
        } catch (SQLException e) {
            throw new InfrastructureException("Failed to load recipe summaries.", e);
        } finally {
            closeIfNonTransactional(conn, "findAllSummaries");
        }
    }

    @Override
    public PageResponse<RecipeSummaryResponse> searchSummaries(SearchCriteria criteria, PageRequest pageRequest) {
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
            conditions.add("d.level_name ILIKE ?");
            params.add("%" + criteria.difficultyName().trim() + "%");
        }
        if (criteria.ingredientName() != null && !criteria.ingredientName().isBlank()) {
            conditions.add("EXISTS (SELECT 1 FROM recipe_ingredients ri " +
                    "INNER JOIN ingredients i ON ri.ingredient_id = i.id_ingredient " +
                    "WHERE ri.recipe_id = r.id_recipe AND i.name ILIKE ?)");
            params.add("%" + criteria.ingredientName().trim() + "%");
        }
        if (criteria.authorUsername() != null && !criteria.authorUsername().isBlank()) {
            conditions.add("EXISTS (SELECT 1 FROM users uf " +
                    "WHERE uf.id_user = r.user_id AND uf.username ILIKE ?)");
            params.add("%" + criteria.authorUsername().trim() + "%");
        }

        String whereClause = conditions.isEmpty() ? "" : " WHERE " + String.join(" AND ", conditions);
        String countSql = COUNT_FROM + whereClause;
        String dataSql = SELECT_SUMMARIES_FROM + whereClause + " LIMIT ? OFFSET ?";

        List<Object> dataParams = new ArrayList<>(params);
        dataParams.add(pageRequest.size());
        dataParams.add(pageRequest.offset());

        Connection conn = null;
        try {
            conn = transactionManager.getConnection();
            long total = queryCount(conn, countSql, params);
            try (PreparedStatement ps = conn.prepareStatement(dataSql)) {
                for (int i = 0; i < dataParams.size(); i++) ps.setObject(i + 1, dataParams.get(i));
                try (ResultSet rs = ps.executeQuery()) {
                    List<RecipeSummaryResponse> results = new ArrayList<>();
                    while (rs.next()) results.add(mapSummaryRow(rs));
                    return PageResponse.of(results, total, pageRequest.size());
                }
            }
        } catch (SQLException e) {
            throw new InfrastructureException("Failed to search recipe summaries.", e);
        } finally {
            closeIfNonTransactional(conn, "searchSummaries");
        }
    }

    @Override
    public List<RecipeSummaryResponse> findSummariesByIds(List<RecipeId> ids) {
        if (ids.isEmpty()) return List.of();
        String placeholders = ids.stream().map(id -> "?").collect(Collectors.joining(", "));
        String sql = SELECT_SUMMARIES_FROM + " WHERE r.id_recipe IN (" + placeholders + ")";
        Connection conn = null;
        try {
            conn = transactionManager.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (int i = 0; i < ids.size(); i++) ps.setInt(i + 1, ids.get(i).value());
                try (ResultSet rs = ps.executeQuery()) {
                    List<RecipeSummaryResponse> results = new ArrayList<>();
                    while (rs.next()) results.add(mapSummaryRow(rs));
                    return results;
                }
            }
        } catch (SQLException e) {
            throw new InfrastructureException("Failed to load recipe summaries by ID list.", e);
        } finally {
            closeIfNonTransactional(conn, "findSummariesByIds");
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private void insertMedia(Connection conn, Recipe recipe) throws SQLException {
        if (recipe.getMediaItems().isEmpty()) return;
        try (PreparedStatement ps = conn.prepareStatement(INSERT_MEDIA)) {
            for (RecipeMedia m : recipe.getMediaItems()) {
                ps.setInt(1, recipe.getId().value());
                ps.setString(2, m.storageKey());
                ps.setString(3, m.storageProvider());
                ps.setString(4, m.mimeType());
                ps.setLong(5, m.sizeBytes());
                ps.setBoolean(6, m.isMain());
                ps.setInt(7, m.sortOrder());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private void syncMedia(Connection conn, Recipe recipe) throws SQLException {
        int recipeId = recipe.getId().value();

        Map<Integer, DbMediaRow> existing = new LinkedHashMap<>();
        try (PreparedStatement ps = conn.prepareStatement(SELECT_MEDIA_FOR_DIFF)) {
            ps.setInt(1, recipeId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int mediaId = rs.getInt("id_media");
                    existing.put(mediaId, new DbMediaRow(mediaId, rs.getBoolean("is_main"), rs.getInt("sort_order")));
                }
            }
        }

        Map<Integer, RecipeMedia> incomingById = new LinkedHashMap<>();
        List<RecipeMedia> newItems = new ArrayList<>();
        for (RecipeMedia m : recipe.getMediaItems()) {
            if (m.id() != null) incomingById.put(m.id().value(), m);
            else newItems.add(m);
        }

        // DELETE items removed from the aggregate
        try (PreparedStatement ps = conn.prepareStatement(DELETE_MEDIA_ITEM)) {
            for (int mediaId : existing.keySet()) {
                if (!incomingById.containsKey(mediaId)) {
                    ps.setInt(1, mediaId);
                    ps.addBatch();
                }
            }
            ps.executeBatch();
        }

        // UPDATE pass 1: clear is_main on items losing main status first to avoid
        // violating the partial unique index (only one is_main=true per recipe)
        try (PreparedStatement ps = conn.prepareStatement("UPDATE recipe_media SET is_main = false WHERE id_media = ?")) {
            for (Map.Entry<Integer, RecipeMedia> entry : incomingById.entrySet()) {
                DbMediaRow row = existing.get(entry.getKey());
                if (row != null && row.isMain() && !entry.getValue().isMain()) {
                    ps.setInt(1, entry.getKey());
                    ps.addBatch();
                }
            }
            ps.executeBatch();
        }

        // UPDATE pass 2: apply full changes (gains is_main=true, sort_order changes)
        try (PreparedStatement ps = conn.prepareStatement(UPDATE_MEDIA)) {
            for (Map.Entry<Integer, RecipeMedia> entry : incomingById.entrySet()) {
                DbMediaRow row = existing.get(entry.getKey());
                if (row != null && row.isDifferentFrom(entry.getValue())) {
                    ps.setBoolean(1, entry.getValue().isMain());
                    ps.setInt(2, entry.getValue().sortOrder());
                    ps.setInt(3, entry.getKey());
                    ps.addBatch();
                }
            }
            ps.executeBatch();
        }

        // INSERT new items (null id means not yet persisted)
        if (!newItems.isEmpty()) {
            try (PreparedStatement ps = conn.prepareStatement(INSERT_MEDIA)) {
                for (RecipeMedia m : newItems) {
                    ps.setInt(1, recipeId);
                    ps.setString(2, m.storageKey());
                    ps.setString(3, m.storageProvider());
                    ps.setString(4, m.mimeType());
                    ps.setLong(5, m.sizeBytes());
                    ps.setBoolean(6, m.isMain());
                    ps.setInt(7, m.sortOrder());
                    ps.addBatch();
                }
                ps.executeBatch();
            }
        }
    }

    private record DbMediaRow(int id, boolean isMain, int sortOrder) {
        boolean isDifferentFrom(RecipeMedia m) {
            return isMain != m.isMain() || sortOrder != m.sortOrder();
        }
    }

    @Override
    public boolean hasUserRatedRecipe(UserId userId, RecipeId recipeId) {
        Connection conn = null;
        try {
            conn = transactionManager.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(COUNT_USER_RATING)) {
                ps.setInt(1, userId.value());
                ps.setInt(2, recipeId.value());
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    return rs.getLong(1) > 0;
                }
            }
        } catch (SQLException e) {
            throw new InfrastructureException(
                    "Failed to check rating for user " + userId.value() +
                    " on recipe " + recipeId.value(), e);
        } finally {
            closeIfNonTransactional(conn, "hasUserRatedRecipe");
        }
    }

    private long queryCount(Connection conn, String sql, List<Object> params) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.size(); i++) ps.setObject(i + 1, params.get(i));
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }

    private RecipeSummaryResponse mapSummaryRow(ResultSet rs) throws SQLException {
        return new RecipeSummaryResponse(
                new RecipeId(rs.getInt("id_recipe")),
                rs.getString("title"),
                rs.getString("category_name"),
                rs.getString("difficulty_name"),
                rs.getInt("prep_time_min"),
                rs.getInt("servings"),
                Optional.ofNullable(rs.getBigDecimal("average_score")).orElse(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP),
                rs.getInt("total_ratings"),
                rs.getString("main_media_storage_key"),
                new UserId(rs.getInt("user_id")),
                rs.getString("author_username"));
    }

    @Override
    public PageResponse<RecipeSummaryResponse> findByAuthorId(UserId authorId, PageRequest page) {
        Connection conn = null;
        try {
            conn = transactionManager.getConnection();
            long total = queryCount(conn, COUNT_FROM + " WHERE r.user_id = ?",
                    List.of(authorId.value()));
            String sql = SELECT_SUMMARIES_FROM + " WHERE r.user_id = ? LIMIT ? OFFSET ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, authorId.value());
                ps.setInt(2, page.size());
                ps.setInt(3, page.offset());
                try (ResultSet rs = ps.executeQuery()) {
                    List<RecipeSummaryResponse> results = new ArrayList<>();
                    while (rs.next()) results.add(mapSummaryRow(rs));
                    return PageResponse.of(results, total, page.size());
                }
            }
        } catch (SQLException e) {
            throw new InfrastructureException(
                    "Failed to load recipes for author ID: " + authorId.value(), e);
        } finally {
            closeIfNonTransactional(conn, "findByAuthorId userId=" + authorId.value());
        }
    }

    private record DbIngredientRow(int ingredientId, int unitId, BigDecimal quantity) {
        boolean isDifferentFrom(RecipeIngredient ri) {
            return unitId != ri.getUnitId().value()
                    || quantity.compareTo(ri.getQuantity()) != 0;
        }
    }
}
