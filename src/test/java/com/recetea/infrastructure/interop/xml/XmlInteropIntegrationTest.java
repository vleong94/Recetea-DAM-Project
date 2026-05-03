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
import com.recetea.core.recipe.domain.InvalidIngredientException;
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
import com.recetea.core.shared.domain.utils.ExecutionContext;
import com.recetea.core.user.domain.UserId;
import com.recetea.infrastructure.metrics.NoOpMetricsAdapter;
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

/**
 * End-to-end XML round-trip against a real database — tests the full
 * import → persist → re-export chain, including XSD validation and
 * catalogue resolution. Exercises {@code XmlInteropAdapter},
 * {@code ImportRecipeUseCase}, and {@code ExportRecipeUseCase} together.
 *
 * <p><b>Cross-package {@code @BeforeAll} note.</b> Because this test
 * extends {@link BaseRepositoryTest} from a different package, Surefire
 * needs {@code --add-opens com.recetea/...repositories=ALL-UNNAMED} on
 * its {@code argLine} so JUnit can reflectively invoke the inherited
 * lifecycle hooks. The flag is already wired in {@code pom.xml}.
 *
 * <p>Use case calls are wrapped in {@code ExecutionContext.run / call}
 * to mirror the wrapper's CID scope — without that, the integration
 * path runs without a bound correlation id and any failure logs lack
 * the trace identifier the production handler emits.
 *
 * <p><b>ES — </b>Round-trip XML extremo a extremo contra una base de
 * datos real — comprueba la cadena completa import → persistir →
 * re-export, incluida la validación XSD y la resolución de
 * catálogo. Ejercita juntos a {@code XmlInteropAdapter},
 * {@code ImportRecipeUseCase} y {@code ExportRecipeUseCase}.
 *
 * <p><b>Nota sobre {@code @BeforeAll} entre paquetes.</b> Como este
 * test hereda de {@link BaseRepositoryTest} desde un paquete
 * diferente, Surefire necesita
 * {@code --add-opens com.recetea/...repositories=ALL-UNNAMED} en
 * su {@code argLine} para que JUnit pueda invocar reflectivamente
 * los hooks de ciclo de vida heredados. El flag ya está cableado
 * en {@code pom.xml}.
 *
 * <p>Las llamadas a casos de uso se envuelven en
 * {@code ExecutionContext.run / call} para reflejar el ámbito CID
 * del wrapper — sin eso, la ruta de integración corre sin un
 * correlation id ligado y los logs de cualquier fallo carecen del
 * identificador de traza que emite el handler de producción.
 */
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
        recipeRepository    = new JdbcRecipeRepository(transactionManager, metricsPort);
        categoryRepository  = new JdbcCategoryRepository(transactionManager, metricsPort);
        difficultyRepository = new JdbcDifficultyRepository(transactionManager, metricsPort);
        ingredientRepository = new JdbcIngredientRepository(transactionManager, metricsPort);
        unitRepository       = new JdbcUnitRepository(transactionManager, metricsPort);
        sessionService       = new SessionManager();
        xmlAdapter = new XmlInteropAdapter(
                categoryRepository, difficultyRepository,
                ingredientRepository, unitRepository,
                new NoOpMetricsAdapter());

        exportUseCase = new ExportRecipeUseCase(recipeRepository, xmlAdapter);
        importUseCase = new ImportRecipeUseCase(
                recipeRepository, transactionManager, sessionService, xmlAdapter);

        seedReferenceData();
        sessionService.login(new UserId(1), "test-user");
    }

    // -----------------------------------------------------------------
    // Seed helpers
    // -----------------------------------------------------------------

    private void seedReferenceData() throws SQLException {
        try (Connection conn = dataSource.getConnection(); Statement st = conn.createStatement()) {
            st.execute("INSERT INTO users (user_id, username, email, password_hash) OVERRIDING SYSTEM VALUE " +
                       "VALUES (1, 'chef', 'chef@test.com', 'hash')");
            st.execute("INSERT INTO categories (category_id, name) OVERRIDING SYSTEM VALUE " +
                       "VALUES (1, 'Desserts')");
            st.execute("INSERT INTO difficulties (difficulty_id, difficulty_level) OVERRIDING SYSTEM VALUE " +
                       "VALUES (1, 'Easy')");
            st.execute("INSERT INTO ingredient_categories (ingredient_category_id, name) OVERRIDING SYSTEM VALUE " +
                       "VALUES (1, 'Flours')");
            st.execute("INSERT INTO unit_measures (unit_id, name, abbreviation) OVERRIDING SYSTEM VALUE " +
                       "VALUES (1, 'Gram', 'g')");
            st.execute("INSERT INTO ingredients (ingredient_id, ingredient_category_id, name) OVERRIDING SYSTEM VALUE " +
                       "VALUES (1, 1, 'Flour'), (2, 1, 'Sugar')");
        }
    }

    private Recipe buildRecipe() {
        return new Recipe(
                new UserId(1),
                new Category(new CategoryId(1), "Desserts"),
                new Difficulty(new DifficultyId(1), "Easy"),
                "Lemon Cake",
                "A fluffy sponge cake.",
                new PreparationTime(45),
                new Servings(8));
    }

    // -----------------------------------------------------------------
    // Tests
    // -----------------------------------------------------------------

    @Test
    @DisplayName("Round-trip: export, delete from DB, and import should produce an equivalent aggregate")
    void roundTrip_ExportDeleteImport_ProducesEquivalentRecipe() {
        // 1. BUILD & PERSIST original recipe
        Recipe original = buildRecipe()
                .syncIngredients(List.of(
                        new RecipeIngredient(new IngredientId(1), new UnitId(1), BigDecimal.valueOf(250)),
                        new RecipeIngredient(new IngredientId(2), new UnitId(1), BigDecimal.valueOf(150))))
                .syncSteps(List.of(
                        new RecipeStep(1, "Mix dry ingredients."),
                        new RecipeStep(2, "Add eggs and oil."),
                        new RecipeStep(3, "Bake for 40 minutes at 180 degrees.")));
        RecipeId originalId = transactionManager.execute(() -> recipeRepository.save(original));

        // 2. EXPORT to XML file (ExportRecipeUseCase calls findById internally)
        // Wrapping in ExecutionContext.run mirrors how the production wrapper opens
        // a correlation scope around the use case so its log lines carry the TraceID.
        File xmlFile = tempDir.resolve("lemon-cake.xml").toFile();
        ExecutionContext.run(() -> exportUseCase.execute(originalId, xmlFile));
        assertTrue(xmlFile.exists(), "The XML file should exist after export");
        assertTrue(xmlFile.length() > 0, "The XML file should not be empty");

        // 3. DELETE original from DB
        transactionManager.execute(() -> recipeRepository.delete(originalId));
        assertTrue(recipeRepository.findById(originalId).isEmpty(),
                "The original recipe should have been deleted from the database");

        // 4. IMPORT from XML (session already holds userId=1)
        RecipeId importedId = ExecutionContext.call(() -> importUseCase.execute(xmlFile));
        assertNotNull(importedId, "Import should return a valid RecipeId");
        assertNotEquals(originalId.value(), importedId.value(),
                "The imported ID should differ from the original (new record)");

        // 5. VERIFY state equivalence via findById
        Recipe imported = recipeRepository.findById(importedId).orElseThrow(
                () -> new AssertionError("Imported recipe was not found in the database"));

        assertEquals("Lemon Cake", imported.getTitle(), "Title should match");
        assertEquals("A fluffy sponge cake.", imported.getDescription(), "Description should match");
        assertEquals(45, imported.getPreparationTimeMinutes().value(), "Preparation time should match");
        assertEquals(8, imported.getServings().value(), "Servings should match");
        assertEquals("Desserts", imported.getCategory().name(), "Category should be resolved by name");
        assertEquals("Easy", imported.getDifficulty().name(), "Difficulty should be resolved by name");
        assertEquals(new UserId(1), imported.getAuthorId(),
                "Author should be the active session user, not the original XML author");

        // Ingredient structural equivalence (order is not guaranteed)
        assertEquals(2, imported.getIngredients().size(), "There should be exactly 2 ingredients");
        assertTrue(imported.getIngredients().stream()
                .anyMatch(i -> "Flour".equals(i.ingredientName())
                               && BigDecimal.valueOf(250).compareTo(i.quantity()) == 0),
                "Flour 250g should be present");
        assertTrue(imported.getIngredients().stream()
                .anyMatch(i -> "Sugar".equals(i.ingredientName())
                               && BigDecimal.valueOf(150).compareTo(i.quantity()) == 0),
                "Sugar 150g should be present");

        // Step equivalence (steps are sorted ascending by syncSteps)
        List<RecipeStep> steps = imported.getSteps();
        assertEquals(3, steps.size(), "There should be exactly 3 steps");
        assertEquals(1, steps.get(0).stepOrder());
        assertEquals("Mix dry ingredients.", steps.get(0).instruction());
        assertEquals(2, steps.get(1).stepOrder());
        assertEquals("Add eggs and oil.", steps.get(1).instruction());
        assertEquals(3, steps.get(2).stepOrder());
        assertEquals("Bake for 40 minutes at 180 degrees.", steps.get(2).instruction());
    }

    @Test
    @DisplayName("fromXml should throw XmlInteropException when XML violates the XSD schema")
    void fromXml_ShouldThrow_WhenXmlViolatesSchema() {
        // Missing required fields: preparationTimeMinutes, servings, categoryName, difficultyName, etc.
        String malformedXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <recipe xmlns="https://recetea.com/xml/recipe">
                    <title>Title only</title>
                </recipe>
                """;

        assertThrows(XmlInteropAdapter.XmlInteropException.class,
                () -> xmlAdapter.fromXml(malformedXml),
                "An incomplete XML should throw XmlInteropException before touching the domain");
    }

    @Test
    @DisplayName("importRecipe should throw InvalidIngredientException when an ingredient from the XML does not exist in the catalogue")
    void importRecipe_ShouldThrow_WhenIngredientNotFoundInCatalogue() throws IOException {
        // Build & export a valid recipe so we get a well-formed XML
        Recipe recipe = buildRecipe()
                .syncIngredients(List.of(
                        new RecipeIngredient(new IngredientId(1), new UnitId(1), BigDecimal.valueOf(100))))
                .syncSteps(List.of(new RecipeStep(1, "Single step")));
        RecipeId savedId = transactionManager.execute(() -> recipeRepository.save(recipe));

        File xmlFile = tempDir.resolve("corrupted.xml").toFile();
        ExecutionContext.run(() -> exportUseCase.execute(savedId, xmlFile));

        // Corrupt the exported XML: replace the real ingredient name with one that doesn't exist
        String xml = Files.readString(xmlFile.toPath());
        // ExportRecipeUseCase.findById loads ingredient name "Flour" from DB
        String corrupted = xml.replace("<name>Flour</name>", "<name>GhostIngredient</name>");
        Files.writeString(xmlFile.toPath(), corrupted);

        assertThrows(InvalidIngredientException.class,
                () -> ExecutionContext.call(() -> importUseCase.execute(xmlFile)),
                "Should throw InvalidIngredientException when the ingredient does not exist in the catalogue");
    }
}
