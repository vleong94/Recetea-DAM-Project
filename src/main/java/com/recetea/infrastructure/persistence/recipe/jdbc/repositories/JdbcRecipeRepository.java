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
import java.time.LocalDateTime;
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
    private static final String SELECT_STEPS_ORDERED =
            "SELECT step_order, instruction FROM steps WHERE recipe_id = ? ORDER BY step_order ASC";
    private static final String SELECT_INGREDIENTS_DEEP =
            "SELECT ri.ingredient_id, ri.unit_id, ri.quantity, i.name AS ing_name, u.abbreviation AS unit_abbr " +
            "FROM recipe_ingredients ri " +
            "INNER JOIN ingredients i ON ri.ingredient_id = i.id_ingredient " +
            "INNER JOIN unit_measures u ON ri.unit_id = u.id_unit " +
            "WHERE ri.recipe_id = ?";
    private static final String INSERT_STEP =
            "INSERT INTO steps (recipe_id, step_order, instruction) VALUES (?, ?, ?)";
    private static final String UPDATE_STEP =
            "UPDATE steps SET instruction = ? WHERE recipe_id = ? AND step_order = ?";
    private static final String DELETE_STEP =
            "DELETE FROM steps WHERE recipe_id = ? AND step_order = ?";

    private static final String SELECT_RATINGS =
            "SELECT user_id, score, comment, created_at FROM ratings WHERE recipe_id = ?";
    private static final String COUNT_USER_RATING =
            "SELECT COUNT(*) FROM ratings WHERE user_id = ? AND recipe_id = ?";
    private static final String UPSERT_RATING =
            "INSERT INTO ratings (user_id, recipe_id, score, comment, created_at) VALUES (?, ?, ?, ?, ?) " +
            "ON CONFLICT (user_id, recipe_id) DO UPDATE SET score = EXCLUDED.score, comment = EXCLUDED.comment";

    private static final String SELECT_BY_ID =
            "SELECT r.*, c.name AS category_name, d.level_name AS difficulty_level " +
            "FROM recipes r " +
            "LEFT JOIN categories c ON r.category_id = c.id_category " +
            "LEFT JOIN difficulties d ON r.difficulty_id = d.id_difficulty " +
            "WHERE r.id_recipe = ?";

    private static final String UPDATE_SOCIAL_METRICS =
            "UPDATE recipes SET average_score = ?, total_ratings = ? WHERE id_recipe = ?";

    private static final String SELECT_MEDIA =
            "SELECT id_media, recipe_id, storage_key, storage_provider, mime_type, size_bytes, is_main, sort_order " +
            "FROM recipe_media WHERE recipe_id = ? ORDER BY sort_order ASC";
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
            "rm.storage_key AS main_media_storage_key " +
            "FROM recipes r " +
            "INNER JOIN categories c ON r.category_id = c.id_category " +
            "INNER JOIN difficulties d ON r.difficulty_id = d.id_difficulty " +
            "LEFT JOIN recipe_media rm ON rm.recipe_id = r.id_recipe AND rm.is_main = true";
    private static final String SELECT_SUMMARIES = SELECT_SUMMARIES_FROM;
    private static final String COUNT_FROM =
            "SELECT count(*) FROM recipes r " +
            "INNER JOIN categories c ON r.category_id = c.id_category " +
            "INNER JOIN difficulties d ON r.difficulty_id = d.id_difficulty";

    public JdbcRecipeRepository(JdbcTransactionManager transactionManager) {
        super(transactionManager);
    }

    @Override
    public void save(Recipe recipe) {
        try {
            Connection conn = transactionManager.getConnection();
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
            throw new RuntimeException("Error al persistir la nueva receta.", e);
        }
    }

    @Override
    public void update(Recipe recipe) {
        try {
            Connection conn = transactionManager.getConnection();
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
            throw new RuntimeException("Error al actualizar la receta con ID: " + recipe.getId().value(), e);
        }
    }

    @Override
    public void delete(RecipeId id) {
        try {
            Connection conn = transactionManager.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(DELETE_RECIPE)) {
                ps.setInt(1, id.value());
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al eliminar la receta.", e);
        }
    }

    @Override
    public void updateSocialMetrics(RecipeId id, BigDecimal averageScore, int totalRatings) {
        try {
            Connection conn = transactionManager.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(UPDATE_SOCIAL_METRICS)) {
                ps.setBigDecimal(1, averageScore);
                ps.setInt(2, totalRatings);
                ps.setInt(3, id.value());
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            throw new InfrastructureException("Error al actualizar métricas sociales para la receta ID: " + id.value(), e);
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

            Recipe recipe = null;
            try (PreparedStatement ps = conn.prepareStatement(SELECT_BY_ID)) {
                ps.setInt(1, id.value());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) recipe = RecipeMapper.mapRow(rs);
                }
            }
            if (recipe == null) return Optional.empty();

            try (PreparedStatement ps = conn.prepareStatement(SELECT_INGREDIENTS_DEEP)) {
                ps.setInt(1, id.value());
                try (ResultSet rs = ps.executeQuery()) {
                    List<RecipeIngredient> ingredients = new ArrayList<>();
                    while (rs.next()) ingredients.add(RecipeMapper.mapIngredientRow(rs));
                    recipe.syncIngredients(ingredients);
                }
            }

            try (PreparedStatement ps = conn.prepareStatement(SELECT_STEPS_ORDERED)) {
                ps.setInt(1, id.value());
                try (ResultSet rs = ps.executeQuery()) {
                    List<RecipeStep> steps = new ArrayList<>();
                    while (rs.next()) steps.add(RecipeMapper.mapStepRow(rs));
                    recipe.syncSteps(steps);
                }
            }

            try (PreparedStatement ps = conn.prepareStatement(SELECT_RATINGS)) {
                ps.setInt(1, id.value());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Rating rating = new Rating(
                                new UserId(rs.getInt("user_id")),
                                new Score(rs.getInt("score")),
                                rs.getString("comment"),
                                rs.getObject("created_at", LocalDateTime.class));
                        recipe.hydrateRating(rating);
                    }
                }
            }

            try (PreparedStatement ps = conn.prepareStatement(SELECT_MEDIA)) {
                ps.setInt(1, id.value());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) recipe.hydrateMedia(mapMediaRow(rs));
                }
            }

            return Optional.of(recipe);
        } catch (SQLException e) {
            throw new InfrastructureException("Error al obtener la receta con ID: " + id.value(), e);
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
            String sql = SELECT_SUMMARIES + " LIMIT ? OFFSET ?";
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
            throw new InfrastructureException("Error al obtener los resúmenes de recetas.", e);
        } finally {
            closeIfNonTransactional(conn, "findAllSummaries");
        }
    }

    @Override
    public PageResponse<RecipeSummaryResponse> searchSummaries(SearchCriteria criteria, PageRequest pageRequest) {
        List<Object> params = new ArrayList<>();
        List<String> conditions = new ArrayList<>();

        if (criteria.title() != null && !criteria.title().isBlank()) {
            conditions.add("LOWER(r.title) LIKE LOWER(?)");
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
            throw new InfrastructureException("Error al buscar resúmenes de recetas con los criterios proporcionados.", e);
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
            throw new InfrastructureException("Error al obtener resúmenes por IDs.", e);
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

    private RecipeMedia mapMediaRow(ResultSet rs) throws SQLException {
        return new RecipeMedia(
                new RecipeMediaId(rs.getInt("id_media")),
                new RecipeId(rs.getInt("recipe_id")),
                rs.getString("storage_key"),
                rs.getString("storage_provider"),
                rs.getString("mime_type"),
                rs.getLong("size_bytes"),
                rs.getBoolean("is_main"),
                rs.getInt("sort_order"));
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
                    "Error al verificar valoración del usuario " + userId.value() +
                    " para receta " + recipeId.value(), e);
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
                new UserId(rs.getInt("user_id")));
    }

    private record DbIngredientRow(int ingredientId, int unitId, BigDecimal quantity) {
        boolean isDifferentFrom(RecipeIngredient ri) {
            return unitId != ri.getUnitId().value()
                    || quantity.compareTo(ri.getQuantity()) != 0;
        }
    }
}
