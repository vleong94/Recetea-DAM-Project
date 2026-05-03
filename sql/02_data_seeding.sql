/* ==========================================================
 * PROJECT: RECETEA
 * DESCRIPTION: Structured data seeding and stress tests.
 *              Master-data expansion: 40 categories, 40 unit
 *              measures, 25 ingredient categories, 297 ingredients,
 *              14 users, 16 recipes, 18 tags. Data values in Spanish.
 *
 *              Existing IDs for the original 18 categories, 18 units,
 *              14 ingredient categories and 85 ingredients are frozen
 *              so the original recipes (1..10) and their recipe_ingredients
 *              references continue to resolve unchanged. New rows are
 *              appended after the originals in every section.
 * ==========================================================
 */

BEGIN;

-- INITIAL CLEANUP (Idempotency) — TRUNCATE block kept at the top so the file
-- is safely re-runnable against any populated test database.
TRUNCATE TABLE
"ratings", "favorites", "recipe_tags", "tags",
"recipe_ingredients", "recipe_media", "steps", "recipes",
"ingredients", "users", "ingredient_categories",
"unit_measures", "categories", "difficulties"
RESTART IDENTITY CASCADE;

-- ----------------------------------------------------------
-- 1. MASTER DATA
-- ----------------------------------------------------------
INSERT INTO "difficulties" ("difficulty_level") VALUES
('Fácil'), ('Medio'), ('Difícil'), ('Muy Difícil');

-- 40 categories. The first six preserve the original IDs (Postres=1, Pasta=2,
-- Ensaladas=3, Carne=4, Guisos=5, Bebidas=6) so existing recipes map unchanged.
-- IDs 7..18 are the original second-batch categories; 19..40 are the new entries.
INSERT INTO "categories" ("name") VALUES
('Postres'), ('Pasta'), ('Ensaladas'), ('Carne'), ('Guisos'), ('Bebidas'),                 -- 1..6
('Panadería'), ('Arroz'), ('Salsas'), ('Aves'), ('Legumbres'), ('Aperitivos'),              -- 7..12
('Mariscos'), ('Vegano'), ('Vegetariano'), ('Desayuno'), ('Entrantes'), ('Sopas'),           -- 13..18
('Pizza'), ('Hamburguesas'), ('Sándwiches'), ('Tacos'), ('Sushi'), ('Curry'),                 -- 19..24
('Barbacoa'), ('A la Parrilla'), ('Asados'), ('Fritos'), ('Al Vapor'), ('Batidos'),           -- 25..30
('Cócteles'), ('Té'), ('Café'), ('Pastelería'), ('Tartas'), ('Galletas'),                     -- 31..36
('Pasteles'), ('Helados'), ('Mermeladas'), ('Mediterránea');                                   -- 37..40

-- 40 unit measures. First six preserve original IDs (Gramo=1, Mililitro=2,
-- Unidad=3, Cucharada=4, Pizca=5, Diente=6).
INSERT INTO "unit_measures" ("name", "abbreviation") VALUES
('Gramo', 'g'), ('Mililitro', 'ml'), ('Unidad', 'ud'), ('Cucharada', 'cda'), ('Pizca', 'pizca'), ('Diente', 'diente'),  -- 1..6
('Kilogramo', 'kg'), ('Litro', 'l'), ('Cucharadita', 'cdta'), ('Rodaja', 'rodaja'),                                       -- 7..10
('Pieza', 'pza'), ('Paquete', 'paq'), ('Manojo', 'manojo'), ('Ramita', 'ramita'),                                          -- 11..14
('Taza', 'taza'), ('Onza', 'oz'), ('Libra', 'lb'), ('Miligramo', 'mg'),                                                    -- 15..18
('Chorrito', 'chorrito'), ('Gota', 'gota'), ('Puñado', 'puñado'), ('Barra', 'barra'),                                      -- 19..22
('Cubo', 'cubo'), ('Lámina', 'lámina'), ('Botella', 'btl'), ('Lata', 'lata'),                                              -- 23..26
('Tarro', 'tarro'), ('Bolsa', 'bolsa'), ('Caja', 'caja'), ('Jarra', 'jarra'),                                              -- 27..30
('Cartón', 'cartón'), ('Tira', 'tira'), ('Tallo', 'tallo'), ('Cabeza', 'cabeza'),                                          -- 31..34
('Cuña', 'cuña'), ('Cuarto', 'qt'), ('Pinta', 'pt'), ('Galón', 'gal'),                                                     -- 35..38
('Onza Líquida', 'oz líq'), ('Media Taza', '½ taza');                                                                       -- 39..40

-- 25 ingredient categories. First five preserve original IDs (Lácteos=1,
-- Verduras=2, Carne=3, Despensa=4, Especias=5).
INSERT INTO "ingredient_categories" ("name") VALUES
('Lácteos'), ('Verduras'), ('Carne'), ('Despensa'), ('Especias'),                                       -- 1..5
('Mariscos'), ('Frutas'), ('Legumbres'), ('Frutos Secos'), ('Aceites'),                                  -- 6..10
('Cereales'), ('Panadería'), ('Dulces'), ('Alcohol'),                                                     -- 11..14
('Hierbas'), ('Queso'), ('Salsas y Condimentos'), ('Vinagres'),                                          -- 15..18
('Bebidas Vegetales'), ('Congelados'), ('Conservas'), ('Pasta Fresca'),                                  -- 19..22
('Té y Café'), ('Edulcorantes'), ('Encurtidos y Conservas');                                              -- 23..25

-- ----------------------------------------------------------
-- 2. USERS — 14 total (9 original + 5 new). All share the password
--             'password123' encoded with BCrypt cost 12.
-- ----------------------------------------------------------
INSERT INTO "users" ("username", "email", "password_hash") VALUES
('chef_arturo',         'arturo@recetea.com',     '$2a$12$5v254X9UAqIj3V/MEb2F.e/yk9zE7Xbp4IZgXd3yxiI0jOcvPMqCG'),
('ana_cocinitas',       'ana@recetea.com',        '$2a$12$5v254X9UAqIj3V/MEb2F.e/yk9zE7Xbp4IZgXd3yxiI0jOcvPMqCG'),
('marcos_gourmet',      'marcos@recetea.com',     '$2a$12$5v254X9UAqIj3V/MEb2F.e/yk9zE7Xbp4IZgXd3yxiI0jOcvPMqCG'),
('maestro_gourmet',     'gourmet@recetea.com',    '$2a$12$5v254X9UAqIj3V/MEb2F.e/yk9zE7Xbp4IZgXd3yxiI0jOcvPMqCG'),
('critico_pro',         'critico@recetea.com',    '$2a$12$5v254X9UAqIj3V/MEb2F.e/yk9zE7Xbp4IZgXd3yxiI0jOcvPMqCG'),
('lector_fantasma',     'fantasma@recetea.com',   '$2a$12$5v254X9UAqIj3V/MEb2F.e/yk9zE7Xbp4IZgXd3yxiI0jOcvPMqCG'),
('explorador_cocina',   'explorador@recetea.com', '$2a$12$5v254X9UAqIj3V/MEb2F.e/yk9zE7Xbp4IZgXd3yxiI0jOcvPMqCG'),
('panadero_casero',     'panadero@recetea.com',   '$2a$12$5v254X9UAqIj3V/MEb2F.e/yk9zE7Xbp4IZgXd3yxiI0jOcvPMqCG'),
('maestro_especias',    'especias@recetea.com',   '$2a$12$5v254X9UAqIj3V/MEb2F.e/yk9zE7Xbp4IZgXd3yxiI0jOcvPMqCG'),
('chef_lucia',          'lucia@recetea.com',      '$2a$12$5v254X9UAqIj3V/MEb2F.e/yk9zE7Xbp4IZgXd3yxiI0jOcvPMqCG'),
('vegano_crudo',        'vegano@recetea.com',     '$2a$12$5v254X9UAqIj3V/MEb2F.e/yk9zE7Xbp4IZgXd3yxiI0jOcvPMqCG'),
('amante_pasta',        'amantepasta@recetea.com','$2a$12$5v254X9UAqIj3V/MEb2F.e/yk9zE7Xbp4IZgXd3yxiI0jOcvPMqCG'),
('goloso',              'goloso@recetea.com',     '$2a$12$5v254X9UAqIj3V/MEb2F.e/yk9zE7Xbp4IZgXd3yxiI0jOcvPMqCG'),
('chef_findesemana',    'findesemana@recetea.com','$2a$12$5v254X9UAqIj3V/MEb2F.e/yk9zE7Xbp4IZgXd3yxiI0jOcvPMqCG');

-- ----------------------------------------------------------
-- 3. INGREDIENTS — 297 total. The original 85 (IDs 1..85) are inserted
--    first, in the same order as before, so existing recipe_ingredients
--    references remain valid. IDs 86..297 are the expansion.
-- ----------------------------------------------------------
INSERT INTO "ingredients" ("ingredient_category_id", "name") VALUES
-- ── Original 85 (frozen IDs 1..85) ────────────────────────────────────────
-- Dairy (1..8)
(1, 'Queso Crema'), (1, 'Yogur Griego'), (1, 'Mantequilla'), (1, 'Parmesano'),
(1, 'Nata para Montar'), (1, 'Mascarpone'), (1, 'Huevo Grande'), (1, 'Leche Entera'),
-- Vegetables (9..21)
(2, 'Cebolla Blanca'), (2, 'Ajo'), (2, 'Tomate Cherry'), (2, 'Lechuga Romana'),
(2, 'Zanahoria'), (2, 'Espinaca'), (2, 'Pimiento'), (2, 'Patata'),
(2, 'Brócoli'), (2, 'Pepino'), (2, 'Calabacín'), (2, 'Champiñón'), (2, 'Apio'),
-- Meat / Poultry (22..29)
(3, 'Pollo en Dados'), (3, 'Carne para Guisar'), (3, 'Pechuga de Pollo'), (3, 'Lomo de Cerdo'),
(3, 'Bacon'), (3, 'Carne Picada'), (3, 'Chuletas de Cordero'), (3, 'Filete de Pavo'),
-- Seafood (30..35)
(6, 'Filete de Salmón'), (6, 'Gambas'), (6, 'Atún en Lata'), (6, 'Bacalao'), (6, 'Mejillones'), (6, 'Vieiras'),
-- Pantry (36..39)
(4, 'Azúcar Blanca'), (4, 'Miel'), (4, 'Salsa de Soja'), (4, 'Pan Rallado'),
-- Spices (40..51)
(5, 'Sal Fina'), (5, 'Pimienta Negra'), (5, 'Orégano'), (5, 'Pimentón'),
(5, 'Canela'), (5, 'Albahaca'), (5, 'Perejil'), (5, 'Comino'),
(5, 'Romero'), (5, 'Tomillo'), (5, 'Nuez Moscada'), (5, 'Copos de Chile'),
-- Fruits (52..59)
(7, 'Limón'), (7, 'Manzana'), (7, 'Plátano'), (7, 'Fresas'),
(7, 'Arándanos'), (7, 'Aguacate'), (7, 'Mango'), (7, 'Naranja'),
-- Legumes (60..62)
(8, 'Garbanzos'), (8, 'Lentejas'), (8, 'Alubias Negras'),
-- Nuts (63..66)
(9, 'Nueces'), (9, 'Almendras'), (9, 'Cacahuetes'), (9, 'Anacardos'),
-- Oils (67..69)
(10, 'Aceite de Oliva'), (10, 'Aceite de Girasol'), (10, 'Aceite de Sésamo'),
-- Grains (70..75)
(11, 'Arroz Blanco'), (11, 'Arroz Integral'), (11, 'Espaguetis'), (11, 'Penne'),
(11, 'Quinoa'), (11, 'Cuscús'),
-- Bakery (76..78)
(12, 'Levadura Química'), (12, 'Levadura'), (12, 'Harina de Trigo'),
-- Sweets (79..82)
(13, 'Chocolate Negro'), (13, 'Extracto de Vainilla'), (13, 'Azúcar Moreno'), (13, 'Cacao en Polvo'),
-- Alcohol (83..85)
(14, 'Vino Tinto'), (14, 'Vino Blanco'), (14, 'Cerveza'),

-- ── Expansion to existing categories (86..199) ───────────────────────────
-- Dairy expansion (86..90)
(1, 'Crema Agria'), (1, 'Crème Fraîche'), (1, 'Suero de Mantequilla'), (1, 'Leche Condensada'), (1, 'Leche Evaporada'),
-- Vegetables expansion (91..108)
(2, 'Cebolla Roja'), (2, 'Cebolleta'), (2, 'Puerro'), (2, 'Espárragos'), (2, 'Berenjena'),
(2, 'Boniato'), (2, 'Repollo'), (2, 'Coliflor'), (2, 'Col Rizada'), (2, 'Coles de Bruselas'),
(2, 'Remolacha'), (2, 'Rábano'), (2, 'Alcachofa'), (2, 'Calabaza'), (2, 'Calabaza Moscada'),
(2, 'Maíz Dulce'), (2, 'Quingombó'), (2, 'Jengibre Fresco'),
-- Meat expansion (109..118)
(3, 'Panceta de Cerdo'), (3, 'Costillas de Cerdo'), (3, 'Salchicha Italiana'), (3, 'Chorizo'), (3, 'Panceta'),
(3, 'Escalope de Ternera'), (3, 'Magret de Pato'), (3, 'Codorniz'), (3, 'Conejo'), (3, 'Carne de Venado'),
-- Pantry expansion (119..126)
(4, 'Sal Marina'), (4, 'Maicena'), (4, 'Bicarbonato Sódico'), (4, 'Pastilla de Caldo'),
(4, 'Caldo en Polvo'), (4, 'Hojas de Gelatina'), (4, 'Caldo de Verduras'), (4, 'Caldo de Pollo'),
-- Spices expansion (127..136)
(5, 'Ajo en Polvo'), (5, 'Cebolla en Polvo'), (5, 'Pimentón Ahumado'), (5, 'Azafrán'),
(5, 'Anís Estrellado'), (5, 'Cardamomo'), (5, 'Pimienta de Jamaica'), (5, 'Curry en Polvo'),
(5, 'Clavos'), (5, 'Cúrcuma'),
-- Seafood expansion (137..146)
(6, 'Lubina'), (6, 'Lenguado'), (6, 'Trucha'), (6, 'Sardinas Frescas'), (6, 'Anchoas Frescas'),
(6, 'Calamares'), (6, 'Pulpo'), (6, 'Cangrejo'), (6, 'Langosta'), (6, 'Ostras'),
-- Fruits expansion (147..159)
(7, 'Piña'), (7, 'Pera'), (7, 'Melocotón'), (7, 'Ciruela'), (7, 'Cereza'),
(7, 'Uvas'), (7, 'Sandía'), (7, 'Lima'), (7, 'Kiwi'), (7, 'Granada'),
(7, 'Papaya'), (7, 'Coco'), (7, 'Melón Cantalupo'),
-- Legumes expansion (160..164)
(8, 'Alubias Blancas'), (8, 'Alubias Rojas'), (8, 'Alubias Pintas'), (8, 'Soja'), (8, 'Edamame'),
-- Nuts expansion (165..170)
(9, 'Pistachos'), (9, 'Nueces Pecanas'), (9, 'Avellanas'), (9, 'Piñones'),
(9, 'Semillas de Girasol'), (9, 'Semillas de Calabaza'),
-- Oils expansion (171..175)
(10, 'Aceite de Coco'), (10, 'Aceite de Aguacate'), (10, 'Aceite de Nuez'), (10, 'Aceite de Trufa'), (10, 'Aceite Vegetal'),
-- Grains expansion (176..183)
(11, 'Bulgur'), (11, 'Cebada Perlada'), (11, 'Avena Cortada'), (11, 'Avena en Copos'),
(11, 'Trigo Sarraceno'), (11, 'Farro'), (11, 'Arroz Arborio'), (11, 'Arroz para Sushi'),
-- Bakery expansion (184..188)
(12, 'Masa de Pizza'), (12, 'Hojaldre'), (12, 'Pasta Filo'), (12, 'Masa Madre'), (12, 'Harina de Fuerza'),
-- Sweets expansion (189..194)
(13, 'Chocolate Blanco'), (13, 'Chocolate con Leche'), (13, 'Pepitas de Chocolate'),
(13, 'Nubes'), (13, 'Caramelo'), (13, 'Fideos de Colores'),
-- Alcohol expansion (195..199)
(14, 'Jerez'), (14, 'Vermut'), (14, 'Ron'), (14, 'Whisky'), (14, 'Vodka'),

-- ── New categories (200..297) ────────────────────────────────────────────
-- Herbs (200..214)
(15, 'Cilantro'), (15, 'Menta'), (15, 'Salvia'), (15, 'Estragón'), (15, 'Cebollino'),
(15, 'Eneldo'), (15, 'Hoja de Laurel'), (15, 'Mejorana'), (15, 'Hierba Limón'), (15, 'Lavanda'),
(15, 'Hojas de Hinojo'), (15, 'Perifollo'), (15, 'Melisa'), (15, 'Acedera'), (15, 'Levístico'),
-- Cheese (215..229)
(16, 'Mozzarella'), (16, 'Cheddar'), (16, 'Brie'), (16, 'Camembert'), (16, 'Gouda'),
(16, 'Feta'), (16, 'Ricotta'), (16, 'Queso Azul'), (16, 'Manchego'), (16, 'Gruyère'),
(16, 'Provolone'), (16, 'Pecorino'), (16, 'Halloumi'), (16, 'Requesón'), (16, 'Queso de Cabra'),
-- Sauces & Condiments (230..241)
(17, 'Kétchup'), (17, 'Mostaza Amarilla'), (17, 'Mostaza de Dijon'), (17, 'Mayonesa'),
(17, 'Salsa Picante'), (17, 'Salsa Barbacoa'), (17, 'Salsa Worcestershire'), (17, 'Tahini'),
(17, 'Pesto de Albahaca'), (17, 'Salsa de Tomate'), (17, 'Concentrado de Tomate'), (17, 'Salsa Hoisin'),
-- Vinegars (242..249)
(18, 'Vinagre Balsámico'), (18, 'Vinagre de Manzana'), (18, 'Vinagre de Arroz'),
(18, 'Vinagre de Jerez'), (18, 'Vinagre de Vino Blanco'), (18, 'Vinagre de Vino Tinto'),
(18, 'Vinagre de Champán'), (18, 'Vinagre de Malta'),
-- Plant-Based Beverages (250..256)
(19, 'Bebida de Coco'), (19, 'Leche de Almendras'), (19, 'Leche de Avena'), (19, 'Leche de Soja'),
(19, 'Leche de Anacardos'), (19, 'Leche de Cáñamo'), (19, 'Leche de Arroz'),
-- Frozen (257..263)
(20, 'Guisantes Congelados'), (20, 'Maíz Congelado'), (20, 'Espinacas Congeladas'), (20, 'Frutos Rojos Congelados'),
(20, 'Mango Congelado'), (20, 'Helado de Vainilla'), (20, 'Cerezas Congeladas'),
-- Canned (264..271)
(21, 'Tomate Triturado en Conserva'), (21, 'Maíz Dulce en Conserva'), (21, 'Piña en Conserva'),
(21, 'Aceitunas en Conserva'), (21, 'Atún en Aceite'), (21, 'Sardinas en Aceite'),
(21, 'Leche de Coco en Conserva'), (21, 'Garbanzos en Conserva'),
-- Fresh Pasta (272..277)
(22, 'Linguine Fresco'), (22, 'Fettuccine Fresco'), (22, 'Tortellini'),
(22, 'Ravioli'), (22, 'Ñoquis de Patata'), (22, 'Pappardelle'),
-- Tea & Coffee (278..283)
(23, 'Granos de Espresso'), (23, 'Té Earl Grey'), (23, 'Manzanilla'),
(23, 'Matcha en Polvo'), (23, 'Yerba Mate'), (23, 'Hojas de Té Verde'),
-- Sweeteners (284..291)
(24, 'Sirope de Arce'), (24, 'Sirope de Agave'), (24, 'Stevia'), (24, 'Azúcar de Coco'),
(24, 'Azúcar Glas'), (24, 'Azúcar Extrafino'), (24, 'Melaza'), (24, 'Sirope de Dátil'),
-- Pickles & Preserves (292..297)
(25, 'Pepinillos en Eneldo'), (25, 'Cornichones'), (25, 'Chucrut'),
(25, 'Kimchi'), (25, 'Mermelada de Fresa'), (25, 'Mermelada de Naranja');

-- ----------------------------------------------------------
-- 4. RECIPES — 16 total (10 originals + 6 new). Originals keep their
--    existing recipe_ingredients references intact (every ID they refer
--    to is in the frozen 1..85 block above).
-- ----------------------------------------------------------
INSERT INTO "recipes" ("user_id", "category_id", "difficulty_id", "title", "description", "prep_time_min", "servings", "average_score", "total_ratings") VALUES
-- ── Originals (1..10) ─────────────────────────────────────────────────────
(1, 1, 1, 'Tarta de Queso Exprés',          'La famosa tarta de queso de cinco ingredientes, lista en menos de una hora.',          45, 8, 4.67, 3),
(4, 5, 3, 'Estofado Tradicional de Ternera', 'Cocido a fuego muy lento para una carne tierna que se deshace al tenedor.',           180, 4, 5.00, 2),
(5, 3, 1, 'Ensalada de Lechuga a Secas',     'No había nada más en la nevera.',                                                       2,   1, 1.50, 2),
(2, 6, 1, 'Vaso de Leche Caliente',          'Perfecto para relajarse antes de dormir.',                                              2,   1, 0.00, 0),
(1, 10, 2, 'Cuscús de Pollo al Limón',       'Cena mediterránea entre semana con notas brillantes de limón.',                         30, 4, 0.00, 0),
(7, 2, 1, 'Espaguetis al Tomate y Albahaca', 'Pasta en veinte minutos con tomates cherry reventados en aceite de oliva.',             20, 2, 0.00, 0),
(2, 16, 1, 'Bowl de Yogur Griego con Frutos Rojos', 'Bowl de desayuno sin cocinar cargado de antioxidantes.',                          5,  1, 0.00, 0),
(3, 13, 2, 'Salmón con Quinoa',              'Salmón a la sartén sobre quinoa esponjosa con un toque de limón.',                      25, 2, 0.00, 0),
(8, 14, 1, 'Ensalada Vegana de Garbanzos',   'Comida vibrante y rica en proteínas lista en diez minutos.',                            10, 4, 0.00, 0),
(9, 7, 3, 'Brownies de Chocolate Negro',     'Brownies cremosos con la parte superior brillante y crujiente.',                        40, 8, 0.00, 0),
-- ── New (11..16) ──────────────────────────────────────────────────────────
(7, 19, 2, 'Pizza Margarita',                'Pizza napolitana clásica con mozzarella fresca, albahaca y tomate.',                    30, 4, 0.00, 0),
(10, 22, 1, 'Tacos de Carne',                'Tacos rápidos entre semana con carne picada sazonada y salsa fresca.',                  25, 4, 0.00, 0),
(12, 8, 2, 'Risotto de Champiñones',         'Risotto cremoso de arroz arborio con champiñones, vino blanco y parmesano.',            35, 4, 0.00, 0),
(4, 3, 1, 'Ensalada César',                  'Lechuga romana crujiente con aliño de anchoas, bacon y parmesano laminado.',            15, 2, 0.00, 0),
(11, 24, 2, 'Curry Tailandés de Coco',       'Curry aromático de pollo con leche de coco y cilantro fresco.',                         40, 4, 0.00, 0),
(13, 35, 1, 'Pan de Plátano Clásico',        'Pan de plátano húmedo con nueces y un toque de canela.',                                60, 8, 0.00, 0);

-- ----------------------------------------------------------
-- 5. STEPS — instructions in technical English.
-- ----------------------------------------------------------
INSERT INTO "steps" ("recipe_id", "step_order", "instruction") VALUES
-- 1. Cheese Cake Express
(1, 1, 'Precalienta el horno a 200 °C.'),
(1, 2, 'Bate el queso crema con el azúcar hasta que esté suave.'),
(1, 3, 'Añade los huevos uno a uno, mezclando constantemente.'),
-- 2. Traditional Beef Stew
(2, 1, 'Sella la carne en aceite de oliva a fuego alto.'),
(2, 2, 'Pica la cebolla y el ajo y sofríe a fuego bajo.'),
(2, 3, 'Añade la carne, cubre con agua y cuece a fuego lento durante 3 horas.'),
-- 3. Plain Lettuce Salad
(3, 1, 'Lava la lechuga.'),
(3, 2, 'Colócala en un bol.'),
-- 4. Hot Milk Glass
(4, 1, 'Vierte la leche en el vaso.'),
(4, 2, 'Calienta durante 1 minuto.'),
-- 5. Lemon Chicken Couscous
(5, 1, 'Marina el pollo en zumo de limón, aceite de oliva y ajo picado durante 20 minutos.'),
(5, 2, 'Sella el pollo en una sartén caliente hasta que esté dorado por ambos lados.'),
(5, 3, 'Cocina el cuscús siguiendo las instrucciones del paquete.'),
(5, 4, 'Corta el pollo en lonchas y sirve sobre el cuscús con un chorrito de limón fresco.'),
-- 6. Tomato Basil Spaghetti
(6, 1, 'Lleva a ebullición una olla grande de agua con sal.'),
(6, 2, 'Cocina los espaguetis hasta que estén al dente, unos 9 minutos.'),
(6, 3, 'Sofríe los tomates cherry en aceite de oliva con ajo machacado.'),
(6, 4, 'Mezcla la pasta con los tomates y termina con albahaca rasgada y parmesano rallado.'),
-- 7. Greek Yogurt Berry Bowl
(7, 1, 'Pon el yogur griego en un bol amplio.'),
(7, 2, 'Cubre con las fresas y los arándanos.'),
(7, 3, 'Riega con miel y esparce nueces picadas por encima.'),
-- 8. Salmon with Quinoa
(8, 1, 'Enjuaga bien la quinoa con agua fría.'),
(8, 2, 'Cocina la quinoa en el doble de su volumen de agua hasta que esté tierna.'),
(8, 3, 'Sella el salmón con la piel hacia abajo en una sartén con una pizca de sal durante 4 minutos.'),
(8, 4, 'Dale la vuelta y cocina 2 minutos más; sirve sobre la quinoa con un cuarto de limón.'),
-- 9. Chickpea Vegan Salad
(9, 1, 'Escurre y enjuaga los garbanzos.'),
(9, 2, 'Corta el pepino y el pimiento en dados.'),
(9, 3, 'Mezcla todos los ingredientes en un bol con aceite de oliva y zumo de limón.'),
(9, 4, 'Sazona al gusto y deja reposar 10 minutos antes de servir.'),
-- 10. Dark Chocolate Brownies
(10, 1, 'Precalienta el horno a 180 °C.'),
(10, 2, 'Derrite el chocolate negro con la mantequilla al baño maría.'),
(10, 3, 'Bate los huevos con el azúcar moreno hasta que estén pálidos y espesos.'),
(10, 4, 'Incorpora la mezcla de chocolate a los huevos, luego tamiza la harina y el cacao en polvo.'),
(10, 5, 'Vierte en un molde forrado y hornea durante 25 minutos hasta que la superficie esté firme.'),
-- 11. Margherita Pizza
(11, 1, 'Precalienta el horno a 250 °C con una piedra para pizza si tienes.'),
(11, 2, 'Estira la masa de pizza sobre una superficie enharinada hasta que esté fina.'),
(11, 3, 'Extiende el concentrado de tomate uniformemente, dejando un borde de 1 cm.'),
(11, 4, 'Trocea la mozzarella por encima y esparce albahaca fresca.'),
(11, 5, 'Hornea de 8 a 10 minutos hasta que la masa esté dorada y el queso burbujee.'),
-- 12. Beef Tacos
(12, 1, 'Dora la carne picada en una sartén a fuego medio-alto.'),
(12, 2, 'Sazona generosamente con sal, pimentón y comino.'),
(12, 3, 'Calienta las tortillas en una sartén seca durante 30 segundos por cada lado.'),
(12, 4, 'Rellena cada tortilla con carne, cheddar, salsa y cilantro picado.'),
(12, 5, 'Termina con un chorrito de lima y sirve inmediatamente.'),
-- 13. Mushroom Risotto
(13, 1, 'Sofríe el ajo picado y los champiñones laminados en mantequilla hasta que estén dorados.'),
(13, 2, 'Añade el arroz arborio y tuesta durante 1 minuto, removiendo.'),
(13, 3, 'Desglasa con vino blanco y deja que se absorba.'),
(13, 4, 'Añade caldo caliente cucharón a cucharón, removiendo hasta que cada uno se absorba.'),
(13, 5, 'Termina con parmesano rallado y una nuez de mantequilla.'),
-- 14. Caesar Salad
(14, 1, 'Tuesta el bacon en una sartén seca y desmenúzalo.'),
(14, 2, 'Bate la mayonesa, las anchoas picadas, el ajo y el limón para el aliño.'),
(14, 3, 'Mezcla la lechuga romana rasgada con el aliño hasta que esté uniformemente cubierta.'),
(14, 4, 'Cubre con bacon, parmesano laminado y un toque final de pimienta negra.'),
-- 15. Thai Coconut Curry
(15, 1, 'Sofríe el jengibre y el ajo picados en aceite hasta que desprendan aroma.'),
(15, 2, 'Añade el pollo en dados y dora por todos lados.'),
(15, 3, 'Incorpora el curry en polvo y cocina durante 30 segundos para realzar las especias.'),
(15, 4, 'Vierte la leche de coco y cuece a fuego lento durante 15 minutos.'),
(15, 5, 'Termina con un puñado generoso de cilantro y un chorrito de lima.'),
-- 16. Classic Banana Bread
(16, 1, 'Precalienta el horno a 175 °C y forra un molde rectangular.'),
(16, 2, 'Tritura los plátanos con el azúcar moreno y la mantequilla derretida.'),
(16, 3, 'Incorpora los huevos batiendo, luego añade la harina, la levadura química y la canela.'),
(16, 4, 'Incorpora las nueces picadas y vierte en el molde.'),
(16, 5, 'Hornea durante 50 minutos hasta que un palillo salga limpio.');

-- ----------------------------------------------------------
-- 6. RECIPE INGREDIENTS — every row references the master IDs above.
--    Originals (recipes 1..10) keep their semantic mapping; new IDs
--    target both legacy ingredients and the expansion catalogue.
-- ----------------------------------------------------------
INSERT INTO "recipe_ingredients" ("recipe_id", "ingredient_id", "unit_id", "quantity") VALUES
-- 1. Cheese Cake Express:  600 g Cream Cheese, 150.5 g White Sugar, 4 Large Eggs
(1,  1,  1, 600.00),  (1, 36, 1, 150.50),  (1,  7,  3,   4.00),
-- 2. Traditional Beef Stew: 800 g Beef Stew Meat, 2 White Onions, 3 Garlic Cloves, 4 tbsp Olive Oil
(2, 23, 1, 800.00),  (2,  9, 3,   2.00),  (2, 10,  6,   3.00),  (2, 67, 4, 4.00),
-- 3. Plain Lettuce Salad: 100 g Romaine Lettuce
(3, 12, 1, 100.00),
-- 4. Hot Milk Glass: 250 ml Whole Milk
(4,  8,  2, 250.00),
-- 5. Lemon Chicken Couscous: 1.10 lb Chicken Breast, 1 cup Couscous, 2 Lemons,
--    3 tbsp Olive Oil, 2 Garlic cloves, 1 tsp Fine Salt
(5, 24, 17,  1.10),  (5, 75, 15, 1.00),  (5, 52, 11,  2.00),
(5, 67,  4,  3.00),  (5, 10,  6, 2.00),  (5, 40,  9,  1.00),
-- 6. Tomato Basil Spaghetti: 200 g Spaghetti, 250 g Cherry Tomato, 1 Basil sprig,
--    2 tbsp Olive Oil, 30 g Parmesan, 1 Garlic clove
(6, 72,  1, 200.00),  (6, 11,  1, 250.00),  (6, 45, 14,  1.00),
(6, 67,  4,   2.00),  (6,  4,  1,  30.00),  (6, 10,  6,  1.00),
-- 7. Greek Yogurt Berry Bowl: 1 cup Greek Yogurt, 0.5 cup Strawberries,
--    0.5 cup Blueberries, 1 tbsp Honey, 1 oz Walnuts
(7,  2, 15, 1.00),  (7, 55, 15, 0.50),  (7, 56, 15, 0.50),
(7, 37,  4, 1.00),  (7, 63, 16, 1.00),
-- 8. Salmon with Quinoa: 1 lb Salmon, 1 cup Quinoa, 1 Lemon, 1 tsp Fine Salt, 1 tbsp Olive Oil
(8, 30, 17, 1.00),  (8, 74, 15, 1.00),  (8, 52, 11, 1.00),
(8, 40,  9, 1.00),  (8, 67,  4, 1.00),
-- 9. Chickpea Vegan Salad: 400 g Chickpeas, 1 Cucumber, 1 Bell Pepper,
--    3 tbsp Olive Oil, 1 Lemon, 0.5 tsp Fine Salt
(9, 60,  1, 400.00),  (9, 18, 11, 1.00),  (9, 15, 11, 1.00),
(9, 67,  4,   3.00),  (9, 52, 11, 1.00),  (9, 40,  9, 0.50),
-- 10. Dark Chocolate Brownies: 200 g Dark Chocolate, 0.5 cup Wheat Flour,
--     3 Eggs, 150 g Butter, 0.75 cup Brown Sugar, 1 tsp Vanilla Extract,
--     2 tbsp Cocoa Powder
(10, 79,  1, 200.00),  (10, 78, 15, 0.50),  (10,  7, 11, 3.00),
(10,  3,  1, 150.00),  (10, 81, 15, 0.75),  (10, 80,  9, 1.00),
(10, 82,  4,   2.00),
-- 11. Margherita Pizza: 1 sheet Pizza Dough, 250 g Mozzarella (215),
--     3 tbsp Tomato Paste (240), 1 Basil sprig, 2 tbsp Olive Oil
(11, 184, 24, 1.00),  (11, 215,  1, 250.00),  (11, 240,  4,   3.00),
(11,  45, 14, 1.00),  (11,  67,  4,   2.00),
-- 12. Beef Tacos: 500 g Ground Beef, 100 g Cheddar (216), 4 tbsp Tomato Salsa (239),
--     1 Avocado, 1 Lime (154), 1 handful Cilantro (200)
(12,  27,  1, 500.00),  (12, 216,  1, 100.00),  (12, 239,  4,   4.00),
(12,  57,  3, 1.00),    (12, 154,  3, 1.00),    (12, 200, 21,   1.00),
-- 13. Mushroom Risotto: 300 g Arborio Rice (182), 200 g Mushroom, 60 g Parmesan,
--     150 ml White Wine, 50 g Butter, 2 Garlic cloves, 750 ml Chicken Stock (126)
(13, 182,  1, 300.00),  (13,  20,  1, 200.00),  (13,   4,  1,  60.00),
(13,  84,  2, 150.00),  (13,   3,  1,  50.00),  (13,  10,  6,   2.00),
(13, 126,  2, 750.00),
-- 14. Caesar Salad: 1 head Romaine Lettuce, 50 g Parmesan, 80 g Bacon,
--     2 Fresh Anchovies (141), 3 tbsp Mayonnaise (233), 1 Garlic clove, 1 Lemon
(14,  12, 34, 1.00),    (14,   4,  1,  50.00),  (14,  26,  1,  80.00),
(14, 141,  3, 2.00),    (14, 233,  4,   3.00),  (14,  10,  6,   1.00),
(14,  52,  3, 1.00),
-- 15. Thai Coconut Curry: 600 g Diced Chicken, 1 can Canned Coconut Milk (270),
--     2 tbsp Curry Powder (134), 1 handful Cilantro, 1 Lime, 30 g Fresh Ginger (108),
--     2 Garlic cloves
(15,  22,  1, 600.00),  (15, 270, 26,   1.00),  (15, 134,  4,   2.00),
(15, 200, 21, 1.00),    (15, 154,  3,   1.00),  (15, 108,  1,  30.00),
(15,  10,  6, 2.00),
-- 16. Classic Banana Bread: 3 Banana, 250 g Wheat Flour, 2 Eggs, 150 g Brown Sugar,
--     100 g Butter, 100 g Walnuts, 1 tsp Cinnamon, 1 tsp Baking Powder
(16,  54,  3, 3.00),    (16,  78,  1, 250.00),  (16,   7,  3,   2.00),
(16,  81,  1, 150.00),  (16,   3,  1, 100.00),  (16,  63,  1, 100.00),
(16,  44,  9, 1.00),    (16,  76,  9,   1.00);

-- ----------------------------------------------------------
-- 7. SOCIAL — TAGS, RECIPE_TAGS, FAVORITES, RATINGS
-- ----------------------------------------------------------
INSERT INTO "tags" ("name") VALUES
('Vegano'), ('SinGluten'), ('AlHorno'), ('Rapido'), ('Tradicional'),
('Saludable'), ('ParaNinos'), ('Mediterranea'), ('Picante'), ('PocosCarbohidratos'),
('AltaProteina'), ('BajaGrasa'), ('Reconfortante'), ('UnaOlla'), ('SinHorno'),
('Brunch'), ('Festivo'), ('Invierno');

INSERT INTO "recipe_tags" ("recipe_id", "tag_id") VALUES
-- Cheese Cake Express → Oven, Quick
(1, 3),  (1, 4),
-- Traditional Beef Stew → Traditional, Comfort, Winter
(2, 5),  (2, 13), (2, 18),
-- Plain Lettuce Salad → Vegan, GlutenFree
(3, 1),  (3, 2),
-- Lemon Chicken Couscous → Mediterranean, Healthy
(5, 8),  (5, 6),
-- Tomato Basil Spaghetti → Mediterranean, Quick
(6, 8),  (6, 4),
-- Greek Yogurt Berry Bowl → Healthy, KidFriendly, Brunch, NoBake
(7, 6),  (7, 7),  (7, 16), (7, 15),
-- Salmon with Quinoa → Healthy, LowCarb, HighProtein
(8, 6),  (8, 10), (8, 11),
-- Chickpea Vegan Salad → Vegan, Healthy, GlutenFree
(9, 1),  (9, 6),  (9, 2),
-- Dark Chocolate Brownies → Oven
(10, 3),
-- Margherita Pizza → Oven, Mediterranean
(11, 3), (11, 8),
-- Beef Tacos → Quick, Spicy
(12, 4), (12, 9),
-- Mushroom Risotto → Comfort, Vegetarian-ish via OnePot
(13, 13), (13, 14),
-- Caesar Salad → Quick, HighProtein
(14, 4), (14, 11),
-- Thai Coconut Curry → Spicy, Comfort
(15, 9), (15, 13),
-- Classic Banana Bread → Oven, KidFriendly, Brunch
(16, 3), (16, 7), (16, 16);

INSERT INTO "favorites" ("user_id", "recipe_id") VALUES
(6, 1), (6, 2), (2, 2),       -- originals
(7, 5), (8, 7), (9, 10),      -- mid-batch — each new user favourites a new recipe
(2, 8), (3, 9),                -- existing users sample the new catalogue
(10, 11), (11, 9), (12, 13),  -- new users pin new recipes
(13, 16), (14, 14), (10, 15);

INSERT INTO "ratings" ("user_id", "recipe_id", "score", "comment") VALUES
-- Originals
(2, 1, 5, 'Espectacular, muy cremoso.'),
(3, 1, 4, 'Sabroso, pero preferiría menos azúcar.'),
(4, 1, 5, 'Técnica impecable para una receta exprés.'),
(1, 2, 5, 'Me recuerda a mi abuela.'),
(3, 2, 5, 'La carne se deshace con el tenedor.'),
(1, 3, 1, 'Esto no es una receta, es un insulto.'),
(4, 3, 2, 'Le falta aliño y dignidad.'),
-- New user feedback on new recipes
(10, 11, 5, 'La masa quedó perfecta sobre una piedra para pizza.'),
(13, 11, 4, 'Me encantó la sencillez, añadiría un poco más de sal.'),
(14, 12, 5, 'Mi receta de cabecera entre semana a partir de ahora.'),
(11, 13, 5, 'Risotto de calidad de restaurante en casa.'),
(2,  14, 4, 'Crujiente, salado, exactamente como debe ser una César.'),
(7,  15, 5, 'Aromático, reconfortante y sorprendentemente fácil.'),
(3,  16, 4, 'Genial con nueces extra por encima.');

COMMIT;

-- ==========================================================
-- STRESS TEST LAB
-- (Execute manually line by line after COMMIT)
-- ==========================================================

-- TEST 1: TRIGGER violation — self-rating prevention
-- INSERT INTO "ratings" ("user_id", "recipe_id", "score") VALUES (1, 1, 5);

-- TEST 2: CHECK violation — score out of range (1-5)
-- INSERT INTO "ratings" ("user_id", "recipe_id", "score") VALUES (2, 1, 7);

-- TEST 3: PARTIAL INDEX violation — two main photos on one recipe
-- INSERT INTO "recipe_media" ("recipe_id", "storage_key", "storage_provider", "mime_type", "size_bytes", "is_main") VALUES (1, 'error.jpg', 'LOCAL', 'image/jpeg', 0, TRUE);

-- TEST 4: FK RESTRICT violation — delete a category that is in use
-- DELETE FROM "categories" WHERE "category_id" = 1;

-- TEST 5: CHECK violation — negative servings
-- UPDATE "recipes" SET "servings" = -2 WHERE "recipe_id" = 1;
