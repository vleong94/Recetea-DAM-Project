/* ==========================================================
 * PROJECT: RECETEA — Production Queries
 *
 * Two sections:
 *   1. REFERENCE QUERIES — demonstrative SQL, runs directly in
 *      psql/pgAdmin. Multi-JOINs, aggregations, subqueries,
 *      grouping, HAVING — coverage of the SQL-course grid.
 *   2. APPLICATION QUERIES — the complete catalog of SQL the
 *      Java code executes in production. Source-of-truth: every
 *      query that lives in RecipeSqlRegistry, the table gateways,
 *      the catalogue repositories, and the user/favorite repos.
 *      Parameterised form (`?` placeholders) is shown first so the
 *      JDBC prepared-statement shape is visible; an example with
 *      concrete literals follows each so the query is also runnable
 *      against a seeded database.
 * ==========================================================
 */

-- ============================================================
-- SECTION 1 · REFERENCE QUERIES
-- ============================================================

-- 1.1 MASTER LISTING (multi-JOIN dashboard)
-- Same JOIN topology that the application's summary projection uses.
SELECT
    r.recipe_id        AS "ID",
    r.title            AS "Title",
    u.username         AS "Author",
    c.name             AS "Category",
    d.difficulty_level AS "Difficulty",
    r.prep_time_min    AS "Time (min)",
    r.average_score    AS "Average Score",
    r.total_ratings    AS "Total Votes"
FROM "recipes" r
         JOIN "users" u        ON r.user_id       = u.user_id
         JOIN "categories" c   ON r.category_id   = c.category_id
         JOIN "difficulties" d ON r.difficulty_id = d.difficulty_id
ORDER BY r.created_at DESC;

-- 1.2 ADVANCED SEARCH (combined filters)
-- Quick something with cheese, for more than 2 people, in under an hour.
SELECT title AS "Recipe", prep_time_min AS "Minutes", servings AS "Servings"
FROM "recipes"
WHERE title ILIKE '%Cheese%'
  AND prep_time_min <= 60
  AND servings >= 2;

-- 1.3 BILL OF MATERIALS (N:M relationship)
-- The exact shopping list for recipe ID 2.
SELECT
    i.name           AS "Ingredient",
    ri.quantity      AS "Quantity",
    um.abbreviation  AS "Unit"
FROM "recipe_ingredients" ri
         JOIN "ingredients" i    ON ri.ingredient_id = i.ingredient_id
         JOIN "unit_measures" um ON ri.unit_id       = um.unit_id
WHERE ri.recipe_id = 2;

-- 1.4 SOCIAL RANKING (denormalised columns, O(1))
-- Reads the precomputed average_score / total_ratings — no JOIN to ratings.
SELECT
    title         AS "Recipe",
    average_score AS "Average Score",
    total_ratings AS "Total Votes"
FROM "recipes"
ORDER BY average_score DESC;

-- 1.5 DASHBOARD STATS (GROUP BY strong entity)
-- How many recipes are published in each category?
SELECT
    c.name             AS "Category",
    COUNT(r.recipe_id) AS "Recipe Count"
FROM "categories" c
         LEFT JOIN "recipes" r ON c.category_id = r.category_id
GROUP BY c.category_id, c.name
ORDER BY "Recipe Count" DESC;

-- 1.6 TOP CREATORS (HAVING)
-- Users who have published more than one recipe.
SELECT
    u.username         AS "Top Chef",
    COUNT(r.recipe_id) AS "Published Recipes"
FROM "users" u
         JOIN "recipes" r ON u.user_id = r.user_id
GROUP BY u.user_id, u.username
HAVING COUNT(r.recipe_id) > 1;

-- 1.7 TAG FILTERING (deep N:M JOIN)
-- All recipes tagged 'Vegan'.
SELECT r.title AS "Vegan Options"
FROM "recipes" r
         JOIN "recipe_tags" rt ON r.recipe_id = rt.recipe_id
         JOIN "tags" t         ON rt.tag_id   = t.tag_id
WHERE t.name = 'Vegan';

-- 1.8 INGREDIENT CATEGORY ROLL-UP (LEFT JOIN + COUNT)
-- How many ingredients live in each ingredient category, including empty ones.
SELECT
    ic.name                  AS "Ingredient Category",
    COUNT(i.ingredient_id)   AS "Ingredient Count"
FROM "ingredient_categories" ic
         LEFT JOIN "ingredients" i ON i.ingredient_category_id = ic.ingredient_category_id
GROUP BY ic.ingredient_category_id, ic.name
ORDER BY "Ingredient Count" DESC;

-- 1.9 USER FAVOURITES PORTFOLIO (correlated read)
-- Every recipe user 1 has favourited, with the author and category alongside.
SELECT
    r.title    AS "Favourite Recipe",
    u.username AS "Author",
    c.name     AS "Category"
FROM "favorites" f
         JOIN "recipes" r    ON f.recipe_id   = r.recipe_id
         JOIN "users" u      ON r.user_id     = u.user_id
         JOIN "categories" c ON r.category_id = c.category_id
WHERE f.user_id = 1
ORDER BY r.title;

-- 1.10 ORPHAN INTEGRITY CHECK (anti-join via NOT EXISTS)
-- Catalogue ingredients that no recipe currently uses — useful for cleanup.
SELECT i.ingredient_id, i.name AS "Unused Ingredient"
FROM "ingredients" i
WHERE NOT EXISTS (
    SELECT 1 FROM "recipe_ingredients" ri
    WHERE ri.ingredient_id = i.ingredient_id
)
ORDER BY i.name;


-- ============================================================
-- SECTION 2 · APPLICATION QUERIES
-- One-to-one mirror of every SQL the Java code executes. Source
-- locations are noted next to each block so the constant in code
-- can be cross-referenced. `?` placeholders mirror the JDBC
-- prepared-statement shape; concrete examples follow.
-- ============================================================


-- ──────────────────────────────────────────────────────────────
-- 2.1 RECIPES — CRUD on the aggregate root
-- Source: RecipeSqlRegistry.INSERT_RECIPE / UPDATE_RECIPE / DELETE_RECIPE
-- ──────────────────────────────────────────────────────────────

-- INSERT (returns the generated recipe_id via Statement.RETURN_GENERATED_KEYS).
INSERT INTO recipes (user_id, category_id, difficulty_id, title, description, prep_time_min, servings)
VALUES (?, ?, ?, ?, ?, ?, ?);
-- Example:
INSERT INTO recipes (user_id, category_id, difficulty_id, title, description, prep_time_min, servings)
VALUES (1, 1, 1, 'Apple Pie', 'A classic homemade apple pie.', 60, 8);

-- UPDATE (metadata only — ingredients/steps/media are diffed by their gateways,
-- and ratings are upserted by saveRatings inside the same transaction).
UPDATE recipes
   SET category_id   = ?,
       difficulty_id = ?,
       title         = ?,
       description   = ?,
       prep_time_min = ?,
       servings      = ?
 WHERE recipe_id = ?;
-- Example:
UPDATE recipes
   SET category_id = 2, difficulty_id = 2, title = 'Updated Apple Pie',
       description = 'Now with cinnamon.', prep_time_min = 75, servings = 6
 WHERE recipe_id = 1;

-- DELETE (FK cascades wipe ratings/favorites/recipe_ingredients/steps/recipe_media/recipe_tags).
DELETE FROM recipes WHERE recipe_id = ?;
-- Example:
DELETE FROM recipes WHERE recipe_id = 99;


-- ──────────────────────────────────────────────────────────────
-- 2.2 RECIPES — Summary projection (listing pages + search)
-- Source: RecipeSqlRegistry.SELECT_SUMMARIES (and SEARCH_BASE_QUERY alias)
-- Used by: findAllSummaries, findSummariesByIds, findByAuthorId,
--          searchSummaries (all in JdbcRecipeRepository / RecipeSearchGateway)
-- ──────────────────────────────────────────────────────────────

SELECT r.recipe_id, r.user_id, r.title,
       c.name AS category_name,
       d.difficulty_level AS difficulty_name,
       r.prep_time_min, r.servings, r.average_score, r.total_ratings,
       rm.storage_key AS main_media_storage_key,
       u.username AS author_username,
       ARRAY(SELECT ri.ingredient_id
               FROM recipe_ingredients ri
              WHERE ri.recipe_id = r.recipe_id) AS ingredient_ids
FROM recipes r
INNER JOIN categories c   ON r.category_id   = c.category_id
INNER JOIN difficulties d ON r.difficulty_id = d.difficulty_id
LEFT JOIN recipe_media rm ON rm.recipe_id    = r.recipe_id AND rm.is_main = true
LEFT JOIN users u         ON u.user_id       = r.user_id;

-- Variant: paginated dashboard listing.
SELECT r.recipe_id, r.user_id, r.title,
       c.name AS category_name, d.difficulty_level AS difficulty_name,
       r.prep_time_min, r.servings, r.average_score, r.total_ratings,
       rm.storage_key AS main_media_storage_key, u.username AS author_username,
       ARRAY(SELECT ri.ingredient_id FROM recipe_ingredients ri WHERE ri.recipe_id = r.recipe_id) AS ingredient_ids
FROM recipes r
INNER JOIN categories c   ON r.category_id   = c.category_id
INNER JOIN difficulties d ON r.difficulty_id = d.difficulty_id
LEFT JOIN recipe_media rm ON rm.recipe_id    = r.recipe_id AND rm.is_main = true
LEFT JOIN users u         ON u.user_id       = r.user_id
LIMIT ? OFFSET ?;
-- Example: first page of 20.
-- (... same SELECT ...) LIMIT 20 OFFSET 0;

-- Variant: by-author profile page.
-- (... same SELECT ...) WHERE r.user_id = ? LIMIT ? OFFSET ?;
-- Example:
-- (... same SELECT ...) WHERE r.user_id = 1 LIMIT 20 OFFSET 0;

-- Variant: hydrate a list of favourited IDs.
-- (... same SELECT ...) WHERE r.recipe_id IN (?, ?, ?);
-- Example:
-- (... same SELECT ...) WHERE r.recipe_id IN (1, 4, 7);


-- ──────────────────────────────────────────────────────────────
-- 2.3 RECIPES — Pagination count (mirrors SELECT_SUMMARIES JOIN topology)
-- Source: RecipeSqlRegistry.COUNT_RECIPES
-- ──────────────────────────────────────────────────────────────

SELECT count(*) FROM recipes r
INNER JOIN categories c   ON r.category_id   = c.category_id
INNER JOIN difficulties d ON r.difficulty_id = d.difficulty_id;

-- Same with author predicate appended:
SELECT count(*) FROM recipes r
INNER JOIN categories c   ON r.category_id   = c.category_id
INNER JOIN difficulties d ON r.difficulty_id = d.difficulty_id
WHERE r.user_id = ?;
-- Example: WHERE r.user_id = 1;


-- ──────────────────────────────────────────────────────────────
-- 2.4 RECIPES — Single-round-trip aggregate fetch
-- Source: RecipeSqlRegistry.SELECT_FULL_AGGREGATE
-- Used by: JdbcRecipeRepository.findById
-- Returns one recipe + its four child collections as JSONB arrays
-- via LEFT JOIN LATERAL — measured budget < 100 ms per fetch even
-- under the JsonbPerformanceTest workload (50 ingredients + 50
-- steps + 1000 ratings).
-- ──────────────────────────────────────────────────────────────

SELECT
    r.*,
    c.name             AS category_name,
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
WHERE r.recipe_id = ?;
-- Example: WHERE r.recipe_id = 1;


-- ──────────────────────────────────────────────────────────────
-- 2.5 RECIPES — Title-prefix autocomplete
-- Source: RecipeSqlRegistry.SUGGEST_TITLES
-- Anchored to the prefix (no leading wildcard) so the title index
-- can serve the lookup; would be a sequential scan otherwise.
-- ──────────────────────────────────────────────────────────────

SELECT DISTINCT title FROM recipes
 WHERE title ILIKE ?
 ORDER BY title
 LIMIT ?;
-- Example: ILIKE 'Apple%' LIMIT 10;


-- ──────────────────────────────────────────────────────────────
-- 2.6 RATINGS — idempotent upsert + denormalised metrics
-- Source: RecipeSqlRegistry.UPSERT_RATING + UPDATE_SOCIAL_METRICS
-- ──────────────────────────────────────────────────────────────

-- Upsert: a re-rate by the same (user_id, recipe_id) overwrites score + comment.
INSERT INTO ratings (user_id, recipe_id, score, comment, created_at)
VALUES (?, ?, ?, ?, ?)
ON CONFLICT (user_id, recipe_id) DO UPDATE
    SET score = EXCLUDED.score, comment = EXCLUDED.comment;
-- Example:
INSERT INTO ratings (user_id, recipe_id, score, comment, created_at)
VALUES (2, 1, 5, 'Excellent', NOW())
ON CONFLICT (user_id, recipe_id) DO UPDATE
    SET score = EXCLUDED.score, comment = EXCLUDED.comment;

-- Push domain-computed metrics back to the recipes row.
UPDATE recipes SET average_score = ?, total_ratings = ? WHERE recipe_id = ?;
-- Example:
UPDATE recipes SET average_score = 4.67, total_ratings = 3 WHERE recipe_id = 1;

-- Has user X rated recipe Y? (drives RatingComponent disabled-state).
SELECT COUNT(*) FROM ratings WHERE user_id = ? AND recipe_id = ?;
-- Example:
SELECT COUNT(*) FROM ratings WHERE user_id = 2 AND recipe_id = 1;


-- ──────────────────────────────────────────────────────────────
-- 2.7 RECIPE_INGREDIENTS — per-recipe sync gateway
-- Source: RecipeIngredientTableGateway
-- ──────────────────────────────────────────────────────────────

-- SELECT for the smart-diff pre-image.
SELECT ingredient_id, unit_id, quantity FROM recipe_ingredients WHERE recipe_id = ?;

INSERT INTO recipe_ingredients (recipe_id, ingredient_id, unit_id, quantity)
VALUES (?, ?, ?, ?);

UPDATE recipe_ingredients SET unit_id = ?, quantity = ?
 WHERE recipe_id = ? AND ingredient_id = ?;

DELETE FROM recipe_ingredients WHERE recipe_id = ? AND ingredient_id = ?;


-- ──────────────────────────────────────────────────────────────
-- 2.8 STEPS — per-recipe sync gateway
-- Source: RecipeStepTableGateway
-- ──────────────────────────────────────────────────────────────

SELECT step_order, instruction FROM steps WHERE recipe_id = ?;

INSERT INTO steps (recipe_id, step_order, instruction) VALUES (?, ?, ?);

UPDATE steps SET instruction = ? WHERE recipe_id = ? AND step_order = ?;

DELETE FROM steps WHERE recipe_id = ? AND step_order = ?;


-- ──────────────────────────────────────────────────────────────
-- 2.9 RECIPE_MEDIA — per-recipe sync gateway (two-pass is_main update)
-- Source: RecipeMediaTableGateway
-- The two-pass UPDATE on is_main avoids momentary violation of the
-- partial unique index on (recipe_id) WHERE is_main = TRUE: pass 1
-- clears the flag from media that's losing main status; pass 2
-- applies the full row update including the new main holder.
-- ──────────────────────────────────────────────────────────────

SELECT media_id, is_main, sort_order FROM recipe_media WHERE recipe_id = ?;

INSERT INTO recipe_media
    (recipe_id, storage_key, storage_provider, mime_type, size_bytes, is_main, sort_order)
VALUES (?, ?, ?, ?, ?, ?, ?);

-- Pass 1: clear is_main on the row losing main status.
UPDATE recipe_media SET is_main = false WHERE media_id = ?;

-- Pass 2: full update (gains is_main + sort_order shift).
UPDATE recipe_media SET is_main = ?, sort_order = ? WHERE media_id = ?;

DELETE FROM recipe_media WHERE media_id = ?;


-- ──────────────────────────────────────────────────────────────
-- 2.10 RECIPE_MEDIA — standalone repository (used outside the diff path)
-- Source: JdbcRecipeMediaRepository
-- ──────────────────────────────────────────────────────────────

INSERT INTO recipe_media
    (recipe_id, storage_key, storage_provider, mime_type, size_bytes, is_main, sort_order)
VALUES (?, ?, ?, ?, ?, ?, ?);

SELECT media_id, recipe_id, storage_key, storage_provider, mime_type, size_bytes, is_main, sort_order
FROM recipe_media WHERE media_id = ?;

SELECT media_id, recipe_id, storage_key, storage_provider, mime_type, size_bytes, is_main, sort_order
FROM recipe_media WHERE recipe_id = ?
ORDER BY sort_order ASC;

DELETE FROM recipe_media WHERE media_id = ?;


-- ──────────────────────────────────────────────────────────────
-- 2.11 USERS — identity repository
-- Source: JdbcUserRepository
-- ──────────────────────────────────────────────────────────────

INSERT INTO users (username, email, password_hash) VALUES (?, ?, ?);
-- Example:
INSERT INTO users (username, email, password_hash)
VALUES ('chef_maria', 'maria@example.com', '$2a$12$hashedhashedhashed');

SELECT user_id, username, email, password_hash FROM users WHERE user_id = ?;
SELECT user_id, username, email, password_hash FROM users WHERE username = ?;
SELECT user_id, username, email, password_hash FROM users WHERE email = ?;


-- ──────────────────────────────────────────────────────────────
-- 2.12 FAVORITES — social pin
-- Source: JdbcFavoriteRepository
-- ──────────────────────────────────────────────────────────────

INSERT INTO favorites (user_id, recipe_id) VALUES (?, ?);

DELETE FROM favorites WHERE user_id = ? AND recipe_id = ?;

-- Atomic cleanup invoked by DeleteRecipeUseCase before deleting the recipe row.
-- Belt-and-braces: the favorites_recipe_id_fkey FK already cascades on recipe delete.
DELETE FROM favorites WHERE recipe_id = ?;

-- Existence probe (drives the per-card star initial state).
SELECT 1 FROM favorites WHERE user_id = ? AND recipe_id = ?;

-- All recipes a given user has favourited (powers the Favourites tab).
SELECT recipe_id FROM favorites WHERE user_id = ?;


-- ──────────────────────────────────────────────────────────────
-- 2.13 CATALOGUE LOOKUPS — read-only master data (cached after first load)
-- Source: JdbcCategoryRepository / JdbcDifficultyRepository
--         / JdbcIngredientRepository / JdbcUnitRepository
-- ──────────────────────────────────────────────────────────────

SELECT category_id, name FROM categories ORDER BY name ASC;

SELECT difficulty_id, difficulty_level FROM difficulties ORDER BY difficulty_id ASC;

SELECT ingredient_id, ingredient_category_id, name FROM ingredients ORDER BY name ASC;

SELECT unit_id, name, abbreviation FROM unit_measures ORDER BY name ASC;


-- ──────────────────────────────────────────────────────────────
-- 2.14 SEARCH — composed predicate (RecipeSearchGateway.buildWhereClause)
-- Built dynamically per request; every clause below is appended to
-- SELECT_SUMMARIES (or COUNT_RECIPES) joined by AND. Clauses with
-- null/blank inputs are simply omitted, so this is the union of
-- everything the gateway can emit.
-- ──────────────────────────────────────────────────────────────

-- Title contains:
-- ... WHERE r.title ILIKE ?     -- '%query%'

-- Maximum prep time:
-- ... AND r.prep_time_min <= ?  -- 60

-- Minimum servings:
-- ... AND r.servings >= ?       -- 4

-- Category name (case-insensitive):
-- ... AND c.name ILIKE ?        -- '%Desserts%'

-- Difficulty (case-insensitive):
-- ... AND d.difficulty_level ILIKE ?  -- '%Easy%'

-- Multi-ingredient AND-match (recipe must contain ALL selected):
-- ... AND EXISTS (
--         SELECT 1 FROM recipe_ingredients ri
--         WHERE ri.recipe_id = r.recipe_id
--           AND ri.ingredient_id IN (?, ?, ?)
--         GROUP BY ri.recipe_id
--         HAVING COUNT(DISTINCT ri.ingredient_id) = ?
--      )

-- Author username (case-insensitive):
-- ... AND EXISTS (
--         SELECT 1 FROM users uf
--         WHERE uf.user_id = r.user_id AND uf.username ILIKE ?
--      )

-- Minimum score:
-- ... AND r.average_score >= ?  -- 4.0

-- Worked example (combines several clauses):
SELECT r.recipe_id, r.user_id, r.title,
       c.name AS category_name, d.difficulty_level AS difficulty_name,
       r.prep_time_min, r.servings, r.average_score, r.total_ratings,
       rm.storage_key AS main_media_storage_key, u.username AS author_username,
       ARRAY(SELECT ri.ingredient_id FROM recipe_ingredients ri WHERE ri.recipe_id = r.recipe_id) AS ingredient_ids
FROM recipes r
INNER JOIN categories c   ON r.category_id   = c.category_id
INNER JOIN difficulties d ON r.difficulty_id = d.difficulty_id
LEFT JOIN recipe_media rm ON rm.recipe_id    = r.recipe_id AND rm.is_main = true
LEFT JOIN users u         ON u.user_id       = r.user_id
WHERE r.title ILIKE '%pie%'
  AND r.prep_time_min <= 60
  AND r.average_score >= 4.0
ORDER BY r.average_score DESC
LIMIT 20 OFFSET 0;


-- ============================================================
-- SECTION 3 · ADMIN / INSPECTION QUERIES
-- Paste-and-run in pgAdmin (or psql). Every query is fully literal
-- — no `?` placeholders. Substitute the inline literal where indicated
-- (search by username, recipe id, etc.).
-- ============================================================


-- ──────────────────────────────────────────────────────────────
-- 3.1 USER INSPECTION
-- ──────────────────────────────────────────────────────────────

-- All users (most recent first if there's no created_at, fallback to id).
SELECT user_id, username, email
FROM users
ORDER BY user_id DESC;

-- Find a specific user by username (case-insensitive).
SELECT user_id, username, email, password_hash
FROM users
WHERE username ILIKE 'chef_maria';

-- Find a specific user by email (case-insensitive).
SELECT user_id, username, email
FROM users
WHERE email ILIKE 'maria@example.com';

-- Sanity-check a user's password-hash shape (BCrypt 12-rounds = 60 chars,
-- starts with `$2a$12$`, `$2b$12$` or `$2y$12$`). Any row that fails this
-- pattern was inserted outside the standard PasswordHasher path.
SELECT user_id, username,
       LENGTH(password_hash)                      AS hash_length,
       LEFT(password_hash, 7)                     AS hash_prefix,
       password_hash ~ '^\$2[ayb]\$12\$.{53}$'    AS is_valid_bcrypt12
FROM users
WHERE username ILIKE 'chef_maria';

-- Full activity summary for one user (recipes authored, favourites pinned,
-- ratings given). Single round-trip via correlated subqueries.
SELECT
    u.user_id,
    u.username,
    u.email,
    (SELECT COUNT(*) FROM recipes   r WHERE r.user_id = u.user_id) AS recipes_authored,
    (SELECT COUNT(*) FROM favorites f WHERE f.user_id = u.user_id) AS favourites_pinned,
    (SELECT COUNT(*) FROM ratings   r WHERE r.user_id = u.user_id) AS ratings_given
FROM users u
WHERE u.username ILIKE 'chef_maria';

-- Same shape for ALL users — quick "who's most active" board.
SELECT
    u.user_id,
    u.username,
    (SELECT COUNT(*) FROM recipes   r WHERE r.user_id = u.user_id) AS recipes_authored,
    (SELECT COUNT(*) FROM favorites f WHERE f.user_id = u.user_id) AS favourites_pinned,
    (SELECT COUNT(*) FROM ratings   r WHERE r.user_id = u.user_id) AS ratings_given
FROM users u
ORDER BY recipes_authored DESC, ratings_given DESC, u.username;

-- Inactive users (no recipes, no ratings, no favourites — a candidate cleanup list).
SELECT u.user_id, u.username, u.email
FROM users u
WHERE NOT EXISTS (SELECT 1 FROM recipes   r WHERE r.user_id = u.user_id)
  AND NOT EXISTS (SELECT 1 FROM favorites f WHERE f.user_id = u.user_id)
  AND NOT EXISTS (SELECT 1 FROM ratings   r WHERE r.user_id = u.user_id);

-- Detect duplicate emails / usernames (the unique constraints should make
-- this empty — running this is a fast check that the constraint is healthy).
SELECT email, COUNT(*) AS hits
FROM users
GROUP BY email
HAVING COUNT(*) > 1;

SELECT username, COUNT(*) AS hits
FROM users
GROUP BY username
HAVING COUNT(*) > 1;


-- ──────────────────────────────────────────────────────────────
-- 3.2 RECIPE INSPECTION
-- ──────────────────────────────────────────────────────────────

-- Full row + author for one recipe.
SELECT
    r.recipe_id,
    r.title,
    r.description,
    u.username       AS author,
    c.name           AS category,
    d.difficulty_level AS difficulty,
    r.prep_time_min,
    r.servings,
    r.average_score,
    r.total_ratings,
    r.created_at
FROM recipes r
JOIN users u        ON r.user_id     = u.user_id
JOIN categories c   ON r.category_id = c.category_id
JOIN difficulties d ON r.difficulty_id = d.difficulty_id
WHERE r.recipe_id = 1;

-- All recipes by a given user.
SELECT r.recipe_id, r.title, r.average_score, r.total_ratings, r.created_at
FROM recipes r
WHERE r.user_id = (SELECT user_id FROM users WHERE username ILIKE 'chef_maria')
ORDER BY r.created_at DESC;

-- Full ingredient list for one recipe (mirrors the dialog the UI shows).
SELECT
    i.name           AS ingredient,
    ri.quantity,
    um.abbreviation  AS unit,
    ic.name          AS ingredient_category
FROM recipe_ingredients ri
JOIN ingredients i             ON ri.ingredient_id          = i.ingredient_id
JOIN unit_measures um          ON ri.unit_id                = um.unit_id
LEFT JOIN ingredient_categories ic ON i.ingredient_category_id = ic.ingredient_category_id
WHERE ri.recipe_id = 1
ORDER BY ic.name, i.name;

-- All steps for one recipe, in order.
SELECT step_order, instruction
FROM steps
WHERE recipe_id = 1
ORDER BY step_order;

-- All ratings for one recipe with the voter's username.
SELECT
    u.username,
    r.score,
    r.comment,
    r.created_at
FROM ratings r
JOIN users u ON u.user_id = r.user_id
WHERE r.recipe_id = 1
ORDER BY r.created_at DESC;

-- Media attached to one recipe (main first).
SELECT media_id, storage_key, storage_provider, mime_type, size_bytes, is_main, sort_order
FROM recipe_media
WHERE recipe_id = 1
ORDER BY is_main DESC, sort_order ASC;

-- Verify the denormalised social metrics match the live aggregate.
-- Any non-zero `delta_*` row indicates the cache drifted from the source
-- (should never happen — addRating + saveRatings recompute on every write).
SELECT
    r.recipe_id,
    r.title,
    r.average_score                                   AS cached_score,
    ROUND(AVG(rt.score)::numeric, 2)                  AS live_score,
    r.total_ratings                                   AS cached_total,
    COUNT(rt.score)                                   AS live_total,
    ROUND(r.average_score - COALESCE(AVG(rt.score), 0)::numeric, 2) AS delta_score,
    r.total_ratings - COUNT(rt.score)                 AS delta_total
FROM recipes r
LEFT JOIN ratings rt ON rt.recipe_id = r.recipe_id
GROUP BY r.recipe_id, r.title, r.average_score, r.total_ratings
HAVING ROUND(r.average_score - COALESCE(AVG(rt.score), 0)::numeric, 2) <> 0
    OR r.total_ratings - COUNT(rt.score) <> 0;


-- ──────────────────────────────────────────────────────────────
-- 3.3 DATABASE HEALTH / INTEGRITY CHECKS
-- ──────────────────────────────────────────────────────────────

-- Row counts across every table — quick "is the DB seeded" check.
SELECT 'users'                  AS table_name, COUNT(*) AS rows FROM users
UNION ALL SELECT 'recipes',                    COUNT(*) FROM recipes
UNION ALL SELECT 'recipe_ingredients',         COUNT(*) FROM recipe_ingredients
UNION ALL SELECT 'steps',                      COUNT(*) FROM steps
UNION ALL SELECT 'recipe_media',               COUNT(*) FROM recipe_media
UNION ALL SELECT 'ratings',                    COUNT(*) FROM ratings
UNION ALL SELECT 'favorites',                  COUNT(*) FROM favorites
UNION ALL SELECT 'tags',                       COUNT(*) FROM tags
UNION ALL SELECT 'recipe_tags',                COUNT(*) FROM recipe_tags
UNION ALL SELECT 'categories',                 COUNT(*) FROM categories
UNION ALL SELECT 'difficulties',               COUNT(*) FROM difficulties
UNION ALL SELECT 'ingredients',                COUNT(*) FROM ingredients
UNION ALL SELECT 'ingredient_categories',      COUNT(*) FROM ingredient_categories
UNION ALL SELECT 'unit_measures',              COUNT(*) FROM unit_measures
ORDER BY table_name;

-- Recipes missing required children (these states should never persist —
-- save() and update() reject empty ingredient/step lists at the domain layer).
SELECT r.recipe_id, r.title, 'no ingredients' AS issue
FROM recipes r
WHERE NOT EXISTS (SELECT 1 FROM recipe_ingredients ri WHERE ri.recipe_id = r.recipe_id)
UNION ALL
SELECT r.recipe_id, r.title, 'no steps'
FROM recipes r
WHERE NOT EXISTS (SELECT 1 FROM steps s WHERE s.recipe_id = r.recipe_id);

-- Recipes with no media (legitimate — media is optional). Useful for picking
-- which recipes still need an image upload.
SELECT r.recipe_id, r.title, u.username AS author
FROM recipes r
JOIN users u ON u.user_id = r.user_id
WHERE NOT EXISTS (SELECT 1 FROM recipe_media m WHERE m.recipe_id = r.recipe_id)
ORDER BY r.created_at DESC;

-- Recipes with media but NO main image (would render as a placeholder card).
-- Should never exist — Recipe.addMedia auto-promotes the first item to isMain.
SELECT r.recipe_id, r.title
FROM recipes r
WHERE EXISTS     (SELECT 1 FROM recipe_media m WHERE m.recipe_id = r.recipe_id)
  AND NOT EXISTS (SELECT 1 FROM recipe_media m WHERE m.recipe_id = r.recipe_id AND m.is_main = true);

-- Recipes with MORE THAN ONE main image (the partial unique index forbids this
-- — running this is a fast verification that the index is in place).
SELECT recipe_id, COUNT(*) AS main_count
FROM recipe_media
WHERE is_main = true
GROUP BY recipe_id
HAVING COUNT(*) > 1;

-- Self-rating attempts (forbidden by Recipe.addRating; the trigger in
-- 01_schema_definition.sql also rejects them at the DB layer).
SELECT r.recipe_id, r.title, u.username AS attempted_self_rater
FROM ratings rt
JOIN recipes r ON r.recipe_id = rt.recipe_id
JOIN users u   ON u.user_id   = rt.user_id
WHERE r.user_id = rt.user_id;


-- ──────────────────────────────────────────────────────────────
-- 3.4 ACTIVITY DASHBOARDS
-- ──────────────────────────────────────────────────────────────

-- 10 most recent recipes with their author.
SELECT r.recipe_id, r.title, u.username AS author, r.created_at
FROM recipes r
JOIN users u ON u.user_id = r.user_id
ORDER BY r.created_at DESC
LIMIT 10;

-- 10 most recent ratings (review feed).
SELECT
    r.title         AS recipe,
    u.username      AS reviewer,
    rt.score,
    rt.comment,
    rt.created_at
FROM ratings rt
JOIN recipes r ON r.recipe_id = rt.recipe_id
JOIN users u   ON u.user_id   = rt.user_id
ORDER BY rt.created_at DESC
LIMIT 10;

-- Top-rated recipes (only those with at least 3 reviews to filter out
-- single-vote outliers).
SELECT
    r.title         AS recipe,
    u.username      AS author,
    r.average_score,
    r.total_ratings
FROM recipes r
JOIN users u ON u.user_id = r.user_id
WHERE r.total_ratings >= 3
ORDER BY r.average_score DESC, r.total_ratings DESC
LIMIT 10;

-- Most-favourited recipes.
SELECT
    r.title              AS recipe,
    u.username           AS author,
    COUNT(f.user_id)     AS favourite_count
FROM recipes r
JOIN users u    ON u.user_id   = r.user_id
LEFT JOIN favorites f ON f.recipe_id = r.recipe_id
GROUP BY r.recipe_id, r.title, u.username
ORDER BY favourite_count DESC, r.title
LIMIT 10;

-- Average rating per category (with sample size).
SELECT
    c.name                                   AS category,
    COUNT(r.recipe_id)                       AS recipes,
    ROUND(AVG(NULLIF(r.average_score, 0))::numeric, 2) AS category_avg_score,
    SUM(r.total_ratings)                     AS total_votes
FROM categories c
LEFT JOIN recipes r ON r.category_id = c.category_id
GROUP BY c.category_id, c.name
ORDER BY category_avg_score DESC NULLS LAST;
