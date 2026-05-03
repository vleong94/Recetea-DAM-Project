package com.recetea.infrastructure.persistence.recipe.jdbc.repositories;

/**
 * Centralised registry of recipe-domain SQL fragments. Pulling the long-form
 * statements out of {@link JdbcRecipeRepository} keeps the repository body focused on
 * orchestration and makes the SQL itself reviewable as a unit (every column alias and
 * JSONB shape lines up with what {@code RecipeMapper} expects to read back).
 *
 * <p>Final + private constructor: this is a constants holder, not a type to subclass
 * or instantiate. Call sites import constants by name (e.g.
 * {@code RecipeSqlRegistry.SELECT_FULL_AGGREGATE}) to keep the repository diff small
 * when SQL is tuned.
 */
public final class RecipeSqlRegistry {

    private RecipeSqlRegistry() {}

    // ── Recipe DML ────────────────────────────────────────────────────────────

    public static final String INSERT_RECIPE = """
            INSERT INTO recipes (user_id, category_id, difficulty_id, title, description, prep_time_min, servings)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;

    public static final String UPDATE_RECIPE = """
            UPDATE recipes
               SET category_id = ?, difficulty_id = ?, title = ?, description = ?,
                   prep_time_min = ?, servings = ?
             WHERE recipe_id = ?
            """;

    public static final String DELETE_RECIPE = "DELETE FROM recipes WHERE recipe_id = ?";

    // ── Ratings + denormalised social metrics ────────────────────────────────

    /**
     * Idempotent rating upsert: a re-rate by the same {@code (user_id, recipe_id)} pair
     * overwrites the prior score + comment via the {@code ON CONFLICT} clause. Used by
     * both the new-rating path and the metadata-update path (the latter rewrites every
     * existing rating but the upserts produce the same values that were already stored,
     * so the operation is functionally a no-op at row level).
     */
    public static final String UPSERT_RATING = """
            INSERT INTO ratings (user_id, recipe_id, score, comment, created_at)
            VALUES (?, ?, ?, ?, ?)
            ON CONFLICT (user_id, recipe_id) DO UPDATE
                SET score = EXCLUDED.score, comment = EXCLUDED.comment
            """;

    /** Pushes the domain-computed denormalised metrics into the {@code recipes} table. */
    public static final String UPDATE_SOCIAL_METRICS =
            "UPDATE recipes SET average_score = ?, total_ratings = ? WHERE recipe_id = ?";

    public static final String COUNT_USER_RATING =
            "SELECT COUNT(*) FROM ratings WHERE user_id = ? AND recipe_id = ?";

    // ── Autocomplete / lookup ────────────────────────────────────────────────

    /**
     * Title-prefix autocomplete. Anchored to the prefix (no leading wildcard) so the
     * {@code title} index can serve the lookup — adding a leading {@code %} would force
     * a full sequential scan of {@code recipes} on every keystroke.
     */
    public static final String SUGGEST_TITLES = """
            SELECT DISTINCT title FROM recipes
             WHERE title ILIKE ?
             ORDER BY title
             LIMIT ?
            """;

    /**
     * Single round-trip aggregate fetch: pulls one recipe row plus its four child
     * collections (ingredients, steps, ratings, media) as JSONB arrays via LEFT JOIN
     * LATERAL subqueries. The aliases here are part of the contract with
     * {@code RecipeMapper.mapRow / mapIngredientsJson / mapStepsJson / mapRatingsJson /
     * mapMediaJson} — column-name drift on either side will silently break hydration
     * (the JsonbPerformanceTest catches gross regressions; column-name mismatches
     * surface immediately as missing-key reads in the JSONObject parse).
     *
     * <p>Each LATERAL block emits a {@code COALESCE(JSONB_AGG(...), '[]'::jsonb)} so a
     * recipe with no children produces an empty array rather than NULL — that's what
     * lets {@code mapRow} treat the four child collections uniformly.
     */
    /**
     * Summary projection used by every recipe-listing path: dashboard ({@code findAllSummaries}),
     * favourites ({@code findSummariesByIds}), author profile ({@code findByAuthorId}), and search.
     * Reads denormalised {@code average_score} / {@code total_ratings} columns directly so the
     * listing path never JOINs the {@code ratings} table. The trailing
     * {@code ARRAY(SELECT ingredient_id ...)} subquery powers the dashboard's client-side
     * multi-ingredient predicate without forcing a {@code GROUP BY} across the whole projection.
     *
     * <p>Column aliases here are part of the contract with
     * {@code RecipeSummaryRowSupport.mapSummaryRow} — drift on either side breaks hydration.
     * The constant is closed with a trailing newline so callers can append
     * {@code " WHERE ..." / " LIMIT ? OFFSET ?"} fragments without manual whitespace bookkeeping.
     */
    public static final String SELECT_SUMMARIES = """
            SELECT r.recipe_id, r.user_id, r.title, c.name AS category_name, d.difficulty_level AS difficulty_name,
                   r.prep_time_min, r.servings, r.average_score, r.total_ratings,
                   rm.storage_key AS main_media_storage_key, u.username AS author_username,
                   ARRAY(SELECT ri.ingredient_id FROM recipe_ingredients ri WHERE ri.recipe_id = r.recipe_id) AS ingredient_ids
            FROM recipes r
            INNER JOIN categories c   ON r.category_id   = c.category_id
            INNER JOIN difficulties d ON r.difficulty_id = d.difficulty_id
            LEFT JOIN recipe_media rm ON rm.recipe_id = r.recipe_id AND rm.is_main = true
            LEFT JOIN users u         ON u.user_id   = r.user_id
            """;

    /**
     * Total-count base for paginated listings. Mirrors the JOIN topology of
     * {@link #SELECT_SUMMARIES} so a WHERE clause built once on top can append cleanly to
     * either form and produce identical row sets — that's what keeps the page totals
     * consistent with the page contents in {@code findAllSummaries} / {@code searchSummaries}.
     */
    public static final String COUNT_RECIPES = """
            SELECT count(*) FROM recipes r
            INNER JOIN categories c   ON r.category_id   = c.category_id
            INNER JOIN difficulties d ON r.difficulty_id = d.difficulty_id
            """;

    /**
     * Base query used by {@code RecipeSearchGateway.searchSummaries}. Identical SQL to
     * {@link #SELECT_SUMMARIES} — exposed under a separate name so the search path's
     * call-site reads "search base + composed WHERE clause" while the listing paths read
     * "summary projection + simple WHERE", documenting intent at the read site without
     * forcing the SQL to be duplicated.
     */
    public static final String SEARCH_BASE_QUERY = SELECT_SUMMARIES;

    public static final String SELECT_FULL_AGGREGATE = """
            SELECT
                r.*,
                c.name            AS category_name,
                d.difficulty_level AS difficulty_level,
                ing_agg.ingredients_json,
                step_agg.steps_json,
                rat_agg.ratings_json,
                media_agg.media_json
            FROM recipes r
            LEFT JOIN categories c   ON r.category_id  = c.category_id
            LEFT JOIN difficulties d ON r.difficulty_id = d.difficulty_id
            LEFT JOIN LATERAL (
                SELECT COALESCE(JSONB_AGG(JSONB_BUILD_OBJECT(
                    'ingredient_id', ri.ingredient_id,
                    'unit_id',       ri.unit_id,
                    'quantity',      ri.quantity,
                    'ing_name',      i.name,
                    'unit_abbr',     u.abbreviation
                ) ORDER BY ri.ingredient_id), '[]'::jsonb) AS ingredients_json
                FROM recipe_ingredients ri
                INNER JOIN ingredients i   ON ri.ingredient_id = i.ingredient_id
                INNER JOIN unit_measures u ON ri.unit_id       = u.unit_id
                WHERE ri.recipe_id = r.recipe_id
            ) ing_agg ON true
            LEFT JOIN LATERAL (
                SELECT COALESCE(JSONB_AGG(JSONB_BUILD_OBJECT(
                    'step_order',  s.step_order,
                    'instruction', s.instruction
                ) ORDER BY s.step_order), '[]'::jsonb) AS steps_json
                FROM steps s
                WHERE s.recipe_id = r.recipe_id
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
                LEFT JOIN users u ON u.user_id = rat.user_id
                WHERE rat.recipe_id = r.recipe_id
            ) rat_agg ON true
            LEFT JOIN LATERAL (
                SELECT COALESCE(JSONB_AGG(JSONB_BUILD_OBJECT(
                    'media_id',         rm.media_id,
                    'storage_key',      rm.storage_key,
                    'storage_provider', rm.storage_provider,
                    'mime_type',        rm.mime_type,
                    'size_bytes',       rm.size_bytes,
                    'is_main',          rm.is_main,
                    'sort_order',       rm.sort_order
                ) ORDER BY rm.sort_order), '[]'::jsonb) AS media_json
                FROM recipe_media rm
                WHERE rm.recipe_id = r.recipe_id
            ) media_agg ON true
            WHERE r.recipe_id = ?
            """;
}
