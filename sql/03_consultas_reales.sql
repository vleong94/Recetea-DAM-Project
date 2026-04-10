/* ==========================================================
 * PROYECTO: RECETEA
 * DESCRIPCIÓN: Consultas Reales de la Aplicación
 * OBJETIVO: Demostrar dominio de JOINs, Agregaciones,
 * Subqueries y manejo de Nulos.
 * ==========================================================
 */

-- 1. LISTADO MAESTRO (JOIN múltiple)
-- Ideal para la pantalla principal de la App.
SELECT
    r.id_recipe AS "ID",
    r.title AS "Título",
    u.username AS "Autor",
    c.name AS "Categoría",
    d.level_name AS "Dificultad",
    r.prep_time_min AS "Tiempo (min)"
FROM "recipes" r
         JOIN "users" u ON r.user_id = u.id_user
         JOIN "categories" c ON r.category_id = c.id_category
         JOIN "difficulties" d ON r.difficulty_id = d.id_difficulty
ORDER BY r.created_at DESC;

-- 2. BÚSQUEDA AVANZADA (Filtros combinados)
-- El usuario busca algo con queso, rápido y para más de 2 personas.
SELECT title AS "Receta", prep_time_min AS "Minutos", servings AS "Raciones"
FROM "recipes"
WHERE title ILIKE '%Queso%'
  AND prep_time_min <= 60
  AND servings >= 2;

-- 3. EXPLOSIÓN DE MATERIALES (Relación N:M)
-- Extrae la lista de la compra exacta para la receta ID 2 (Estofado).
SELECT
    i.name AS "Ingrediente",
    ri.quantity AS "Cantidad",
    um.abbreviation AS "Unidad"
FROM "recipe_ingredients" ri
         JOIN "ingredients" i ON ri.ingredient_id = i.id_ingredient
         JOIN "unit_measures" um ON ri.unit_id = um.id_unit
WHERE ri.recipe_id = 2;

-- 4. RANKING SOCIAL CON MANEJO DE NULOS (LEFT JOIN)
-- [MEJORA CLAVE]: Muestra TODAS las recetas. Si no tienen votos, devuelve 0 usando COALESCE.
SELECT
    r.title AS "Receta",
    COALESCE(ROUND(AVG(ra.score), 1), 0.0) AS "Puntuación Media",
    COUNT(ra.id_rating) AS "Total Votos"
FROM "recipes" r
         LEFT JOIN "ratings" ra ON r.id_recipe = ra.recipe_id
GROUP BY r.id_recipe, r.title
ORDER BY "Puntuación Media" DESC;

-- 5. ESTADÍSTICAS PARA DASHBOARD (Agrupación por Entidad Fuerte)
-- ¿Cuántas recetas tenemos publicadas en cada categoría?
SELECT
    c.name AS "Categoría",
    COUNT(r.id_recipe) AS "Nº de Recetas"
FROM "categories" c
         LEFT JOIN "recipes" r ON c.id_category = r.category_id
GROUP BY c.id_category, c.name
ORDER BY "Nº de Recetas" DESC;

-- 6. TOP CREATORS (Uso de Subqueries / HAVING)
-- Encuentra a los usuarios que han publicado más de 1 receta.
SELECT
    u.username AS "Top Chef",
    COUNT(r.id_recipe) AS "Recetas Publicadas"
FROM "users" u
         JOIN "recipes" r ON u.id_user = r.user_id
GROUP BY u.id_user, u.username
HAVING COUNT(r.id_recipe) > 1;

-- 7. FILTRADO POR ETIQUETAS (Deep JOIN N:M)
-- Encuentra todas las recetas que tengan el tag 'Vegano'.
SELECT r.title AS "Opciones Veganas"
FROM "recipes" r
         JOIN "recipe_tags" rt ON r.id_recipe = rt.recipe_id
         JOIN "tags" t ON rt.tag_id = t.id_tag
WHERE t.name = 'Vegano';