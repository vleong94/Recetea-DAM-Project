package com.recetea.infrastructure.interop.xml;

import com.recetea.core.recipe.application.ports.out.category.ICategoryRepository;
import com.recetea.core.recipe.application.ports.out.difficulty.IDifficultyRepository;
import com.recetea.core.recipe.application.ports.out.ingredient.IIngredientRepository;
import com.recetea.core.recipe.application.ports.out.recipe.IRecipeRepository;
import com.recetea.core.recipe.application.ports.out.unit.IUnitRepository;
import com.recetea.core.recipe.application.usecases.interop.ExportRecipeUseCase;
import com.recetea.core.recipe.application.usecases.interop.ImportRecipeUseCase;
import com.recetea.core.recipe.domain.Category;
import com.recetea.core.recipe.domain.Difficulty;
import com.recetea.core.recipe.domain.Recipe;
import com.recetea.core.recipe.domain.RecipeIngredient;
import com.recetea.core.recipe.domain.RecipeStep;
import com.recetea.core.recipe.domain.vo.CategoryId;
import com.recetea.core.recipe.domain.vo.DifficultyId;
import com.recetea.core.recipe.domain.vo.IngredientId;
import com.recetea.core.recipe.domain.vo.PreparationTime;
import com.recetea.core.recipe.domain.vo.RecipeId;
import com.recetea.core.recipe.domain.vo.Servings;
import com.recetea.core.recipe.domain.vo.UnitId;
import com.recetea.core.user.domain.UserId;
import com.recetea.infrastructure.persistence.recipe.jdbc.JdbcTransactionManager;
import com.recetea.infrastructure.persistence.recipe.jdbc.repositories.BaseRepositoryTest;
import com.recetea.infrastructure.persistence.recipe.jdbc.repositories.JdbcCategoryRepository;
import com.recetea.infrastructure.persistence.recipe.jdbc.repositories.JdbcDifficultyRepository;
import com.recetea.infrastructure.persistence.recipe.jdbc.repositories.JdbcIngredientRepository;
import com.recetea.infrastructure.persistence.recipe.jdbc.repositories.JdbcRecipeRepository;
import com.recetea.infrastructure.persistence.recipe.jdbc.repositories.JdbcUnitRepository;
import com.recetea.infrastructure.security.SessionManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class XmlInteropIntegrationTest extends BaseRepositoryTest {

    @TempDir
    Path tempDir;

    private JdbcTransactionManager transactionManager;
    private IRecipeRepository recipeRepository;
    private ICategoryRepository categoryRepository;
    private IDifficultyRepository difficultyRepository;
    private IIngredientRepository ingredientRepository;
    private IUnitRepository unitRepository;
    private SessionManager sessionService;
    private XmlInteropAdapter xmlAdapter;
    private ExportRecipeUseCase exportUseCase;
    private ImportRecipeUseCase importUseCase;

    @BeforeEach
    void setUp() throws SQLException {
        transactionManager = new JdbcTransactionManager(dataSource);
        recipeRepository    = new JdbcRecipeRepository(transactionManager);
        categoryRepository  = new JdbcCategoryRepository(transactionManager);
        difficultyRepository = new JdbcDifficultyRepository(transactionManager);
        ingredientRepository = new JdbcIngredientRepository(transactionManager);
        unitRepository       = new JdbcUnitRepository(transactionManager);
        sessionService       = new SessionManager();
        xmlAdapter           = new XmlInteropAdapter();

        exportUseCase = new ExportRecipeUseCase(recipeRepository, xmlAdapter);
        importUseCase = new ImportRecipeUseCase(
                recipeRepository, categoryRepository, difficultyRepository,
                ingredientRepository, unitRepository,
                transactionManager, sessionService, xmlAdapter);

        seedReferenceData();
        sessionService.login(new UserId(1));
    }

    // -----------------------------------------------------------------
    // Seed helpers
    // -----------------------------------------------------------------

    private void seedReferenceData() throws SQLException {
        try (Connection conn = dataSource.getConnection(); Statement st = conn.createStatement()) {
            st.execute("INSERT INTO users (id_user, username, email, password_hash) OVERRIDING SYSTEM VALUE " +
                       "VALUES (1, 'chef', 'chef@test.com', 'hash')");
            st.execute("INSERT INTO categories (id_category, name) OVERRIDING SYSTEM VALUE " +
                       "VALUES (1, 'Postres')");
            st.execute("INSERT INTO difficulties (id_difficulty, level_name) OVERRIDING SYSTEM VALUE " +
                       "VALUES (1, 'Facil')");
            st.execute("INSERT INTO ingredient_categories (id_ing_category, name) OVERRIDING SYSTEM VALUE " +
                       "VALUES (1, 'Harinas')");
            st.execute("INSERT INTO unit_measures (id_unit, name, abbreviation) OVERRIDING SYSTEM VALUE " +
                       "VALUES (1, 'Gramo', 'g')");
            st.execute("INSERT INTO ingredients (id_ingredient, ing_category_id, name) OVERRIDING SYSTEM VALUE " +
                       "VALUES (1, 1, 'Harina'), (2, 1, 'Azucar')");
        }
    }

    private Recipe buildRecipe() {
        return new Recipe(
                new UserId(1),
                new Category(new CategoryId(1), "Postres"),
                new Difficulty(new DifficultyId(1), "Facil"),
                "Bizcocho de Limon",
                "Un bizcocho esponjoso.",
                new PreparationTime(45),
                new Servings(8));
    }

    // -----------------------------------------------------------------
    // Tests
    // -----------------------------------------------------------------

    @Test
    @DisplayName("Round-trip: Exportar, eliminar de BD e importar produce un agregado equivalente")
    void roundTrip_ExportDeleteImport_ProducesEquivalentRecipe() {
        // 1. BUILD & PERSIST original recipe
        Recipe original = buildRecipe();
        original.syncIngredients(List.of(
                new RecipeIngredient(new IngredientId(1), new UnitId(1), BigDecimal.valueOf(250)),
                new RecipeIngredient(new IngredientId(2), new UnitId(1), BigDecimal.valueOf(150))));
        original.syncSteps(List.of(
                new RecipeStep(1, "Mezclar los ingredientes secos."),
                new RecipeStep(2, "Agregar huevos y aceite."),
                new RecipeStep(3, "Hornear 40 minutos a 180 grados.")));
        transactionManager.execute(() -> recipeRepository.save(original));
        RecipeId originalId = original.getId();

        // 2. EXPORT to XML file (ExportRecipeUseCase calls findById internally)
        File xmlFile = tempDir.resolve("bizcocho.xml").toFile();
        exportUseCase.execute(originalId, xmlFile);
        assertTrue(xmlFile.exists(), "El archivo XML debe existir tras la exportacion");
        assertTrue(xmlFile.length() > 0, "El archivo XML no debe estar vacio");

        // 3. DELETE original from DB
        transactionManager.execute(() -> recipeRepository.delete(originalId));
        assertTrue(recipeRepository.findById(originalId).isEmpty(),
                "La receta original debe haber sido borrada de la BD");

        // 4. IMPORT from XML (session already holds userId=1)
        RecipeId importedId = importUseCase.execute(xmlFile);
        assertNotNull(importedId, "La importacion debe retornar un RecipeId valido");
        assertNotEquals(originalId.value(), importedId.value(),
                "El ID importado debe ser distinto al ID original (nuevo registro)");

        // 5. VERIFY state equivalence via findById
        Recipe imported = recipeRepository.findById(importedId).orElseThrow(
                () -> new AssertionError("La receta importada no se encontro en la BD"));

        assertEquals("Bizcocho de Limon", imported.getTitle(), "El titulo debe coincidir");
        assertEquals("Un bizcocho esponjoso.", imported.getDescription(), "La descripcion debe coincidir");
        assertEquals(45, imported.getPreparationTimeMinutes().value(), "El tiempo de preparacion debe coincidir");
        assertEquals(8, imported.getServings().value(), "Las raciones deben coincidir");
        assertEquals("Postres", imported.getCategory().getName(), "La categoria debe resolverse por nombre");
        assertEquals("Facil", imported.getDifficulty().getName(), "La dificultad debe resolverse por nombre");
        assertEquals(new UserId(1), imported.getAuthorId(),
                "El autor debe ser el usuario de la sesion activa, no el autor original del XML");

        // Ingredient structural equivalence (order is not guaranteed)
        assertEquals(2, imported.getIngredients().size(), "Deben existir exactamente 2 ingredientes");
        assertTrue(imported.getIngredients().stream()
                .anyMatch(i -> "Harina".equals(i.getIngredientName())
                               && BigDecimal.valueOf(250).compareTo(i.getQuantity()) == 0),
                "Harina 250g debe estar presente");
        assertTrue(imported.getIngredients().stream()
                .anyMatch(i -> "Azucar".equals(i.getIngredientName())
                               && BigDecimal.valueOf(150).compareTo(i.getQuantity()) == 0),
                "Azucar 150g debe estar presente");

        // Step equivalence (steps are sorted ascending by syncSteps)
        List<RecipeStep> steps = imported.getSteps();
        assertEquals(3, steps.size(), "Deben existir exactamente 3 pasos");
        assertEquals(1, steps.get(0).stepOrder());
        assertEquals("Mezclar los ingredientes secos.", steps.get(0).instruction());
        assertEquals(2, steps.get(1).stepOrder());
        assertEquals("Agregar huevos y aceite.", steps.get(1).instruction());
        assertEquals(3, steps.get(2).stepOrder());
        assertEquals("Hornear 40 minutos a 180 grados.", steps.get(2).instruction());
    }

    @Test
    @DisplayName("fromXml debe lanzar XmlInteropException si el XML viola el esquema XSD")
    void fromXml_ShouldThrow_WhenXmlViolatesSchema() {
        // Missing required fields: preparationTimeMinutes, servings, categoryName, difficultyName, etc.
        String malformedXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <recipe xmlns="https://recetea.com/xml/recipe">
                    <title>Solo titulo</title>
                </recipe>
                """;

        assertThrows(XmlInteropAdapter.XmlInteropException.class,
                () -> xmlAdapter.fromXml(malformedXml),
                "Un XML incompleto debe lanzar XmlInteropException antes de tocar el dominio");
    }

    @Test
    @DisplayName("importRecipe debe lanzar XmlInteropException si el ingrediente del XML no existe en el catalogo")
    void importRecipe_ShouldThrow_WhenIngredientNotFoundInCatalogue() throws IOException {
        // Build & export a valid recipe so we get a well-formed XML
        Recipe recipe = buildRecipe();
        recipe.syncIngredients(List.of(
                new RecipeIngredient(new IngredientId(1), new UnitId(1), BigDecimal.valueOf(100))));
        recipe.syncSteps(List.of(new RecipeStep(1, "Paso unico")));
        transactionManager.execute(() -> recipeRepository.save(recipe));

        File xmlFile = tempDir.resolve("corrupted.xml").toFile();
        exportUseCase.execute(recipe.getId(), xmlFile);

        // Corrupt the exported XML: replace the real ingredient name with one that doesn't exist
        String xml = Files.readString(xmlFile.toPath());
        // ExportRecipeUseCase.findById loads ingredient name "Harina" from DB
        String corrupted = xml.replace("<name>Harina</name>", "<name>IngredienteFantasma</name>");
        Files.writeString(xmlFile.toPath(), corrupted);

        assertThrows(XmlInteropAdapter.XmlInteropException.class,
                () -> importUseCase.execute(xmlFile),
                "Debe lanzar XmlInteropException cuando el ingrediente no existe en el catalogo");
    }
}
