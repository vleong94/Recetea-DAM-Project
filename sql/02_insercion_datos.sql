/* ==========================================================
 * PROYECTO: RECETEA
 * DESCRIPCIÓN: Data Seeding estructurado y Tests de Estrés.
 * ==========================================================
 */

BEGIN;

-- LIMPIEZA INICIAL (Idempotencia)
TRUNCATE TABLE
"ratings", "favorites", "recipe_tags", "tags",
"recipe_ingredients", "recipe_media", "steps", "recipes",
"ingredients", "users", "ingredient_categories",
"unit_measures", "categories", "difficulties"
RESTART IDENTITY CASCADE;

-- ----------------------------------------------------------
-- 1. DATOS MAESTROS
-- ----------------------------------------------------------
INSERT INTO "difficulties" ("level_name") VALUES ('Fácil'), ('Media'), ('Difícil'), ('Chef');
INSERT INTO "categories" ("name") VALUES ('Postres'), ('Pastas'), ('Ensaladas'), ('Carnes'), ('Guisos'), ('Bebidas');
INSERT INTO "unit_measures" ("name", "abbreviation") VALUES ('Gramos', 'g'), ('Mililitros', 'ml'), ('Unidades', 'ud'), ('Cucharadas', 'cda'), ('Pizca', 'pzc'), ('Dientes', 'dnt');
INSERT INTO "ingredient_categories" ("name") VALUES ('Lácteos'), ('Verduras'), ('Carnes'), ('Despensa'), ('Especias');

-- ----------------------------------------------------------
-- 2. USUARIOS
-- ----------------------------------------------------------
-- All seed users share the password: password123
-- Hash: BCrypt $2a$12$, 12 rounds
INSERT INTO "users" ("username", "email", "password_hash") VALUES
('chef_arturo',   'arturo@recetea.com',  '$2a$12$5v254X9UAqIj3V/MEb2F.e/yk9zE7Xbp4IZgXd3yxiI0jOcvPMqCG'),
('ana_cocinitas', 'ana@recetea.com',     '$2a$12$5v254X9UAqIj3V/MEb2F.e/yk9zE7Xbp4IZgXd3yxiI0jOcvPMqCG'),
('marcos_foodie', 'marcos@recetea.com',  '$2a$12$5v254X9UAqIj3V/MEb2F.e/yk9zE7Xbp4IZgXd3yxiI0jOcvPMqCG'),
('gourmet_master','gourmet@recetea.com', '$2a$12$5v254X9UAqIj3V/MEb2F.e/yk9zE7Xbp4IZgXd3yxiI0jOcvPMqCG'),
('hater_pro',     'hater@recetea.com',   '$2a$12$5v254X9UAqIj3V/MEb2F.e/yk9zE7Xbp4IZgXd3yxiI0jOcvPMqCG'),
('ghost_reader',  'ghost@recetea.com',   '$2a$12$5v254X9UAqIj3V/MEb2F.e/yk9zE7Xbp4IZgXd3yxiI0jOcvPMqCG');

-- ----------------------------------------------------------
-- 3. INGREDIENTES
-- ----------------------------------------------------------
INSERT INTO "ingredients" ("ing_category_id", "name") VALUES
(1, 'Queso Crema'), (4, 'Azúcar Blanco'), (3, 'Pollo Troceado'), (2, 'Tomate Cherry'), (5, 'Sal Fina'), (1, 'Huevo L'),
(2, 'Cebolla Blanca'), (2, 'Ajo'), (3, 'Ternera para Guiso'), (4, 'Aceite de Oliva'), (5, 'Pimienta Negra'), (2, 'Lechuga Romana'), (1, 'Leche Entera');

-- ----------------------------------------------------------
-- 4. RECETAS, PASOS E INGREDIENTES
-- ----------------------------------------------------------
INSERT INTO "recipes" ("user_id", "category_id", "difficulty_id", "title", "description", "prep_time_min", "servings", "average_score", "total_ratings") VALUES
(1, 1, 1, 'Tarta de Queso Express',          'La famosa tarta de queso de 5 ingredientes.', 45,  8, 4.67, 3),
(4, 5, 3, 'Estofado de Ternera Tradicional', 'Cocción lenta a fuego muy bajo.',              180, 4, 5.00, 2),
(5, 3, 1, 'Ensalada de Lechuga Sola',        'No tenía más cosas en la nevera.',             2,   1, 1.50, 2),
(2, 6, 1, 'Vaso de Leche Caliente',          'Ideal para dormir.',                           2,   1, 0.00, 0);

INSERT INTO "steps" ("recipe_id", "step_order", "instruction") VALUES
(1, 1, 'Precalentar el horno a 200°C.'), (1, 2, 'Mezclar el queso crema con el azúcar hasta que esté suave.'), (1, 3, 'Añadir los huevos uno a uno mientras se bate.'),
(2, 1, 'Sellar la carne con aceite de oliva.'), (2, 2, 'Picar la cebolla y el ajo y pochar a fuego lento.'), (2, 3, 'Añadir la carne, cubrir con agua y dejar cocer 3 horas.'),
(3, 1, 'Lavar la lechuga.'), (3, 2, 'Poner en un bol.'),
(4, 1, 'Verter leche en vaso.'), (4, 2, 'Calentar 1 minuto.');

INSERT INTO "recipe_media" ("recipe_id", "url", "is_main", "sort_order") VALUES
(1, 'assets/img/tarta_queso_final.jpg', TRUE, 1), (1, 'assets/img/tarta_queso_horno.jpg', FALSE, 2);

INSERT INTO "recipe_ingredients" ("recipe_id", "ingredient_id", "unit_id", "quantity") VALUES
(1, 1, 1, 600.00), (1, 2, 1, 150.50), (1, 6, 3, 4.00),
(2, 9, 1, 800.00), (2, 7, 3, 2.00), (2, 8, 6, 3.00), (2, 10, 4, 4.00),
(3, 12, 1, 100.00), (4, 13, 2, 250.00);

-- ----------------------------------------------------------
-- 5. SOCIAL (TAGS, FAVORITOS Y RATINGS)
-- ----------------------------------------------------------
INSERT INTO "tags" ("name") VALUES ('Vegano'), ('GlutenFree'), ('Horno'), ('Rápido'), ('Tradicional');
INSERT INTO "recipe_tags" ("recipe_id", "tag_id") VALUES (1, 3), (1, 4), (2, 5), (3, 1), (3, 2);
INSERT INTO "favorites" ("user_id", "recipe_id") VALUES (6, 1), (6, 2), (2, 2);

INSERT INTO "ratings" ("user_id", "recipe_id", "score", "comment") VALUES
(2, 1, 5, 'Espectacular, muy cremosa.'), (3, 1, 4, 'Rica, pero prefiero menos dulce.'), (4, 1, 5, 'Técnica impecable para ser express.'),
(1, 2, 5, 'Me recuerda a mi abuela.'), (3, 2, 5, 'La carne se deshace.'),
(1, 3, 1, 'Esto no es una receta, es un insulto.'), (4, 3, 2, 'Le falta aliño y dignidad.');

COMMIT;

-- ==========================================================
-- LABORATORIO DE PRUEBAS DE ESTRÉS
-- (Ejecutar manualmente línea por línea tras el COMMIT)
-- ==========================================================

-- TEST 1: Violación del TRIGGER de No Autovaloración
-- INSERT INTO "ratings" ("user_id", "recipe_id", "score") VALUES (1, 1, 5);

-- TEST 2: Violación de CHECK (Puntuación fuera de rango 1-5)
-- INSERT INTO "ratings" ("user_id", "recipe_id", "score") VALUES (2, 1, 7);

-- TEST 3: Violación de PARTIAL INDEX (Dos fotos principales en una receta)
-- INSERT INTO "recipe_media" ("recipe_id", "url", "is_main") VALUES (1, 'error.jpg', TRUE);

-- TEST 4: Violación de FK RESTRICT (Borrar categoría en uso)
-- DELETE FROM "categories" WHERE "id_category" = 1;

-- TEST 5: Violación de CHECK (Raciones negativas)
-- UPDATE "recipes" SET "servings" = -2 WHERE "id_recipe" = 1;