package com.recetea.infrastructure.interop.xml;

import com.recetea.core.recipe.application.ports.out.category.ICategoryRepository;
import com.recetea.core.recipe.application.ports.out.difficulty.IDifficultyRepository;
import com.recetea.core.recipe.application.ports.out.ingredient.IIngredientRepository;
import com.recetea.core.recipe.application.ports.out.interop.IRecipeInteropPort;
import com.recetea.core.shared.application.ports.out.IMetricsPort;
import com.recetea.core.shared.domain.TechnicalException;
import com.recetea.infrastructure.interop.xml.dto.XmlIngredientDto;
import com.recetea.infrastructure.interop.xml.dto.XmlRecipeDto;
import com.recetea.infrastructure.interop.xml.dto.XmlStepDto;
import com.recetea.core.recipe.application.ports.out.unit.IUnitRepository;
import com.recetea.core.recipe.domain.Category;
import com.recetea.core.recipe.domain.Difficulty;
import com.recetea.core.recipe.domain.Ingredient;
import com.recetea.core.recipe.domain.InvalidIngredientException;
import com.recetea.core.recipe.domain.Recipe;
import com.recetea.core.recipe.domain.RecipeIngredient;
import com.recetea.core.recipe.domain.RecipeStep;
import com.recetea.core.recipe.domain.Unit;
import com.recetea.core.recipe.domain.vo.PreparationTime;
import com.recetea.core.recipe.domain.vo.Servings;
import com.recetea.core.user.domain.UserId;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.sax.SAXSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * JAXB-backed adapter for {@link IRecipeInteropPort} — owns the full
 * round-trip between {@link Recipe} aggregates and the
 * {@code https://recetea.com/xml/recipe} XML namespace.
 *
 * <p><b>Hardening (import side).</b> Every input is size-capped at
 * {@link #MAX_XML_BYTES} (10 MB) <em>before</em> the parser is allocated.
 * The SAX source is built with {@link #hardenedSource(InputSource)} which
 * disables external entities, parameter entities, and DOCTYPE declarations
 * (XXE defence) plus enables {@code FEATURE_SECURE_PROCESSING} and
 * additional JDK property limits as defence-in-depth.
 *
 * <p><b>Catalogue resolution.</b> The XML carries display names for
 * categories, difficulties, ingredients, and units; {@code toDomain(...)}
 * resolves each through one of four lazy {@code AtomicReference<Map>}
 * lookup tables. Maps are built once on first import via
 * {@code compareAndSet} and reused thereafter — catalogues are immutable
 * at runtime per {@code CLAUDE.md}, so no invalidation is needed.
 *
 * <p><b>Locale coupling.</b> Lookups are case-insensitive but exact-match
 * — the catalogue must be seeded in the same language as the document.
 * Importing an English-locale export into a Spanish-seeded DB fails at
 * the catalogue lookup; this is intentional (renaming a catalogue entry
 * post-import would silently replace the wrong row).
 *
 * <p><b>ES — </b>Adaptador respaldado por JAXB para
 * {@link IRecipeInteropPort} — es dueño del ida-y-vuelta completo
 * entre los agregados {@link Recipe} y el espacio de nombres XML
 * {@code https://recetea.com/xml/recipe}.
 *
 * <p><b>Endurecimiento (lado de import).</b> Toda entrada se acota a
 * {@link #MAX_XML_BYTES} (10 MB) <em>antes</em> de asignar el parser.
 * La fuente SAX se construye con
 * {@link #hardenedSource(InputSource)}, que deshabilita las
 * entidades externas, las entidades de parámetro y las declaraciones
 * DOCTYPE (defensa XXE), y además habilita
 * {@code FEATURE_SECURE_PROCESSING} y límites adicionales en
 * propiedades del JDK como defensa en profundidad.
 *
 * <p><b>Resolución de catálogo.</b> El XML lleva los nombres de
 * presentación de categorías, dificultades, ingredientes y unidades;
 * {@code toDomain(...)} resuelve cada uno mediante una de cuatro
 * tablas perezosas {@code AtomicReference<Map>}. Los mapas se
 * construyen una vez en el primer import vía {@code compareAndSet}
 * y se reutilizan después — los catálogos son inmutables en runtime
 * según {@code CLAUDE.md}, así que no hace falta invalidación.
 *
 * <p><b>Acoplamiento por idioma.</b> Las búsquedas son insensibles
 * a mayúsculas pero de coincidencia exacta — el catálogo debe estar
 * sembrado en el mismo idioma que el documento. Importar una
 * exportación en locale inglés a una BD sembrada en español falla
 * en el lookup de catálogo; es intencional (renombrar una entrada
 * de catálogo después del import reemplazaría silenciosamente la
 * fila equivocada).
 */
public class XmlInteropAdapter implements IRecipeInteropPort {

    private static final String SCHEMA_RESOURCE = "/com/recetea/infrastructure/interop/xml/recipe.xsd";

    /** Hard upper bound on accepted XML payload size (bytes for files, characters for strings). */
    private static final long MAX_XML_BYTES = 10L * 1024 * 1024;

    private final JAXBContext jaxbContext;
    private final Schema schema;
    private final ICategoryRepository categoryRepository;
    private final IDifficultyRepository difficultyRepository;
    private final IIngredientRepository ingredientRepository;
    private final IUnitRepository unitRepository;
    private final IMetricsPort metricsPort;

    // Catalogue lookup tables — built lazily on first import, reused thereafter.
    // Catalogues are immutable at runtime (see CLAUDE.md), so cache invalidation is unnecessary.
    private final AtomicReference<Map<String, Category>>   categoriesByNameRef     = new AtomicReference<>();
    private final AtomicReference<Map<String, Difficulty>> difficultiesByNameRef   = new AtomicReference<>();
    private final AtomicReference<Map<String, Ingredient>> ingredientsByNameRef    = new AtomicReference<>();
    private final AtomicReference<Map<String, Unit>>       unitsByAbbreviationRef  = new AtomicReference<>();

    public XmlInteropAdapter(ICategoryRepository categoryRepository,
                             IDifficultyRepository difficultyRepository,
                             IIngredientRepository ingredientRepository,
                             IUnitRepository unitRepository,
                             IMetricsPort metricsPort) {
        this.categoryRepository   = categoryRepository;
        this.difficultyRepository = difficultyRepository;
        this.ingredientRepository = ingredientRepository;
        this.unitRepository       = unitRepository;
        this.metricsPort          = metricsPort;
        try {
            jaxbContext = JAXBContext.newInstance(XmlRecipeDto.class);
            URL schemaUrl = XmlInteropAdapter.class.getResource(SCHEMA_RESOURCE);
            if (schemaUrl == null) {
                throw new XmlInteropException("XSD schema not found at: " + SCHEMA_RESOURCE);
            }
            SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            schema = sf.newSchema(schemaUrl);
        } catch (JAXBException e) {
            throw new XmlInteropException("Failed to initialise JAXB context.", e);
        } catch (XmlInteropException e) {
            throw e;
        } catch (Exception e) {
            throw new XmlInteropException("Failed to load XSD schema.", e);
        }
    }

    @Override
    public void exportRecipe(Recipe recipe, File destination) {
        long start = System.nanoTime();
        try {
            XmlRecipeDto dto = toDto(recipe);
            try {
                Marshaller m = jaxbContext.createMarshaller();
                m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
                m.marshal(dto, destination);
                metricsPort.recordFileOperation("xml.write");
            } catch (JAXBException e) {
                throw new XmlInteropException(
                        "Failed to write recipe to file: " + destination.getAbsolutePath(), e);
            }
        } finally {
            metricsPort.recordQueryTime("xml.export", System.nanoTime() - start);
        }
    }

    @Override
    public Recipe importFromSource(File source, UserId authorId) {
        long start = System.nanoTime();
        try {
            metricsPort.recordFileOperation("xml.read");
            XmlRecipeDto dto = unmarshal(source);
            return toDomain(dto, authorId);
        } finally {
            metricsPort.recordQueryTime("xml.import", System.nanoTime() - start);
        }
    }

    /** Schema-validates a raw XML string and returns the unmarshalled DTO. */
    public XmlRecipeDto fromXml(String xml) {
        if (xml == null) {
            throw new XmlInteropException("XML string must not be null.");
        }
        if (xml.length() > MAX_XML_BYTES) {
            throw new XmlInteropException(
                    "XML string exceeds " + MAX_XML_BYTES + "-character size limit.");
        }
        try {
            Unmarshaller u = jaxbContext.createUnmarshaller();
            u.setSchema(schema);
            return (XmlRecipeDto) u.unmarshal(hardenedSource(new InputSource(new StringReader(xml))));
        } catch (JAXBException e) {
            throw new XmlInteropException("XML string is invalid or does not conform to XSD schema.", e);
        }
    }

    private XmlRecipeDto unmarshal(File source) {
        if (source.length() > MAX_XML_BYTES) {
            throw new XmlInteropException(
                    "XML file exceeds " + MAX_XML_BYTES + "-byte size limit: "
                            + source.getAbsolutePath() + " (" + source.length() + " bytes)");
        }
        try (FileInputStream fis = new FileInputStream(source)) {
            Unmarshaller u = jaxbContext.createUnmarshaller();
            u.setSchema(schema);
            return (XmlRecipeDto) u.unmarshal(hardenedSource(new InputSource(fis)));
        } catch (JAXBException | IOException e) {
            throw new XmlInteropException(
                    "XML file is invalid or does not conform to XSD schema: " + source.getAbsolutePath(), e);
        }
    }

    /**
     * Builds a SAX source whose underlying parser refuses DOCTYPE declarations and
     * external entities — closing every XXE vector before JAXB sees the document.
     * The OWASP-recommended trio of features handles general entities, parameter
     * entities, and the secure-processing master switch in one go.
     *
     * <p>Defence-in-depth: the JDK XML limits below cap entity expansion, attributes
     * per element, and {@code maxOccurs} amplification at numbers far below their
     * default ceilings. The {@code disallow-doctype-decl=true} feature already prevents
     * any DOCTYPE / DTD / entity construct from reaching the parser, so the entity
     * limit is technically redundant — it stays as a backstop in case a future code
     * change relaxes the doctype guard. The element-attribute and max-occur limits
     * harden against schema-validation amplification attacks that would slip past
     * the doctype guard.
     */
    private SAXSource hardenedSource(InputSource input) {
        try {
            SAXParserFactory spf = SAXParserFactory.newInstance();
            spf.setNamespaceAware(true);
            spf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            spf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            spf.setFeature("http://xml.org/sax/features/external-general-entities", false);
            spf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);

            SAXParser parser = spf.newSAXParser();
            applyJdkXmlLimits(parser);
            return new SAXSource(parser.getXMLReader(), input);
        } catch (Exception e) {
            throw new XmlInteropException("Failed to create XXE-hardened XML reader.", e);
        }
    }

    /**
     * Applies the JDK-specific {@code jdk.xml.*} property ceilings to {@code parser}.
     * These properties are honoured by the JDK's bundled SAX implementation. On a non-JDK
     * parser (e.g. a third-party Xerces variant) the {@code SAXNotRecognizedException}
     * is swallowed — {@code FEATURE_SECURE_PROCESSING} default ceilings still apply.
     */
    private static void applyJdkXmlLimits(SAXParser parser) {
        // Hard caps deliberately set well below the JDK secure-processing defaults
        // (64000 / 10000 / 5000) — a legitimate recipe import nowhere near these.
        trySetProperty(parser, "jdk.xml.entityExpansionLimit",  "100");
        trySetProperty(parser, "jdk.xml.elementAttributeLimit", "50");
        trySetProperty(parser, "jdk.xml.maxOccurLimit",         "1000");
    }

    private static void trySetProperty(SAXParser parser, String name, String value) {
        try {
            parser.setProperty(name, value);
        } catch (org.xml.sax.SAXNotRecognizedException | org.xml.sax.SAXNotSupportedException ignored) {
            // Non-JDK parser — secure-processing defaults remain in effect.
        }
    }

    private Recipe toDomain(XmlRecipeDto dto, UserId authorId) {
        Category category = categoriesByName().get(dto.getCategoryName().toLowerCase());
        if (category == null) throw new InvalidIngredientException(
                "Category not found in catalogue: '" + dto.getCategoryName() + "'.");

        Difficulty difficulty = difficultiesByName().get(dto.getDifficultyName().toLowerCase());
        if (difficulty == null) throw new InvalidIngredientException(
                "Difficulty not found in catalogue: '" + dto.getDifficultyName() + "'.");

        List<RecipeIngredient> domainIngredients = dto.getIngredients().stream()
                .map(this::resolveIngredient)
                .toList();
        List<RecipeStep> domainSteps = dto.getSteps().stream()
                .map(s -> new RecipeStep(s.getOrder(), s.getInstruction()))
                .toList();

        return new Recipe(
                authorId, category, difficulty,
                dto.getTitle(), dto.getDescription(),
                new PreparationTime(dto.getPreparationTimeMinutes()),
                new Servings(dto.getServings()))
                .syncIngredients(domainIngredients)
                .syncSteps(domainSteps);
    }

    private RecipeIngredient resolveIngredient(XmlIngredientDto xmlIng) {
        Ingredient ingredient = ingredientsByName().get(xmlIng.getName().toLowerCase());
        if (ingredient == null) {
            throw new InvalidIngredientException(
                    "Ingredient not found in catalogue: '" + xmlIng.getName() + "'.");
        }
        Unit unit = unitsByAbbreviation().get(xmlIng.getUnit().toLowerCase());
        if (unit == null) {
            throw new InvalidIngredientException(
                    "Unit of measure not found by abbreviation: '" + xmlIng.getUnit() + "'.");
        }
        return new RecipeIngredient(
                ingredient.id(), unit.id(), xmlIng.getQuantity(),
                ingredient.name(), unit.abbreviation());
    }

    // ── Lazy catalogue lookup tables ────────────────────────────────────────
    // First call builds the unmodifiable map; subsequent calls return the cached reference.
    // compareAndSet ensures at most one map is published; concurrent racers discard
    // their duplicate (harmless because the result would be equivalent).

    private Map<String, Category> categoriesByName() {
        Map<String, Category> cached = categoriesByNameRef.get();
        if (cached != null) return cached;
        Map<String, Category> built = categoryRepository.findAll().stream()
                .collect(Collectors.toUnmodifiableMap(
                        c -> c.name().toLowerCase(), Function.identity(), (a, b) -> a));
        categoriesByNameRef.compareAndSet(null, built);
        return categoriesByNameRef.get();
    }

    private Map<String, Difficulty> difficultiesByName() {
        Map<String, Difficulty> cached = difficultiesByNameRef.get();
        if (cached != null) return cached;
        Map<String, Difficulty> built = difficultyRepository.findAll().stream()
                .collect(Collectors.toUnmodifiableMap(
                        d -> d.name().toLowerCase(), Function.identity(), (a, b) -> a));
        difficultiesByNameRef.compareAndSet(null, built);
        return difficultiesByNameRef.get();
    }

    private Map<String, Ingredient> ingredientsByName() {
        Map<String, Ingredient> cached = ingredientsByNameRef.get();
        if (cached != null) return cached;
        Map<String, Ingredient> built = ingredientRepository.findAll().stream()
                .collect(Collectors.toUnmodifiableMap(
                        i -> i.name().toLowerCase(), Function.identity(), (a, b) -> a));
        ingredientsByNameRef.compareAndSet(null, built);
        return ingredientsByNameRef.get();
    }

    private Map<String, Unit> unitsByAbbreviation() {
        Map<String, Unit> cached = unitsByAbbreviationRef.get();
        if (cached != null) return cached;
        Map<String, Unit> built = unitRepository.findAll().stream()
                .collect(Collectors.toUnmodifiableMap(
                        u -> u.abbreviation().toLowerCase(), Function.identity(), (a, b) -> a));
        unitsByAbbreviationRef.compareAndSet(null, built);
        return unitsByAbbreviationRef.get();
    }

    private XmlRecipeDto toDto(Recipe recipe) {
        XmlRecipeDto dto = new XmlRecipeDto();
        dto.setTitle(recipe.getTitle());
        dto.setDescription(recipe.getDescription());
        dto.setPreparationTimeMinutes(recipe.getPreparationTimeMinutes().value());
        dto.setServings(recipe.getServings().value());
        dto.setCategoryName(recipe.getCategory().name());
        dto.setDifficultyName(recipe.getDifficulty().name());

        List<XmlIngredientDto> xmlIngredients = recipe.getIngredients().stream()
                .map(ri -> new XmlIngredientDto(
                        ri.quantity(),
                        ri.unitAbbreviation() != null ? ri.unitAbbreviation() : "",
                        ri.ingredientName()   != null ? ri.ingredientName()   : ""))
                .toList();
        dto.setIngredients(xmlIngredients);

        List<XmlStepDto> xmlSteps = recipe.getSteps().stream()
                .map(s -> new XmlStepDto(s.stepOrder(), s.instruction()))
                .toList();
        dto.setSteps(xmlSteps);

        return dto;
    }

    /**
     * XML import / export failure. Extends {@link TechnicalException} with the stable
     * code {@code "INT-400"} so the application layer never needs to import the JAXB
     * stack to handle an XSD-validation or schema-violation failure — observing
     * {@code TechnicalException} is enough.
     */
    public static class XmlInteropException extends TechnicalException {
        private static final String CODE = "INT-400";
        public XmlInteropException(String message)                  { super(message, CODE); }
        public XmlInteropException(String message, Throwable cause) { super(message, CODE, cause); }
    }
}
