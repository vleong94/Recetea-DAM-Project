package com.recetea.infrastructure.interop.xml;

import com.recetea.core.recipe.application.ports.out.category.ICategoryRepository;
import com.recetea.core.recipe.application.ports.out.difficulty.IDifficultyRepository;
import com.recetea.core.recipe.application.ports.out.ingredient.IIngredientRepository;
import com.recetea.core.recipe.application.ports.out.interop.IRecipeInteropPort;
import com.recetea.core.recipe.application.ports.out.unit.IUnitRepository;
import com.recetea.core.recipe.domain.Category;
import com.recetea.core.recipe.domain.Difficulty;
import com.recetea.core.recipe.domain.Ingredient;
import com.recetea.core.recipe.domain.Recipe;
import com.recetea.core.recipe.domain.RecipeIngredient;
import com.recetea.core.recipe.domain.RecipeStep;
import com.recetea.core.recipe.domain.Unit;
import com.recetea.core.recipe.domain.vo.PreparationTime;
import com.recetea.core.recipe.domain.vo.Servings;
import com.recetea.core.user.domain.UserId;
import com.recetea.infrastructure.interop.xml.dto.XmlIngredientDto;
import com.recetea.infrastructure.interop.xml.dto.XmlRecipeDto;
import com.recetea.infrastructure.interop.xml.dto.XmlStepDto;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.File;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Translates between the XML interop layer (XmlRecipeDto) and the domain layer (Recipe aggregate).
 * Implements {@link IRecipeInteropPort} so the application core depends only on the port interface.
 *
 * Export path: Recipe → XmlRecipeDto → XML string/file.
 * Import path: XML file → XmlRecipeDto (schema-validated) → catalogue resolution → Recipe.
 */
public class XmlInteropAdapter implements IRecipeInteropPort {

    private static final String SCHEMA_RESOURCE = "/com/recetea/infrastructure/interop/xml/recipe.xsd";

    private final JAXBContext jaxbContext;
    private final Schema schema;

    public XmlInteropAdapter() {
        try {
            jaxbContext = JAXBContext.newInstance(XmlRecipeDto.class);
            URL schemaUrl = XmlInteropAdapter.class.getResource(SCHEMA_RESOURCE);
            if (schemaUrl == null) {
                throw new XmlInteropException("No se encontró el esquema XSD en: " + SCHEMA_RESOURCE);
            }
            SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            schema = sf.newSchema(schemaUrl);
        } catch (JAXBException e) {
            throw new XmlInteropException("Error al inicializar el contexto JAXB.", e);
        } catch (Exception e) {
            throw new XmlInteropException("Error al cargar el esquema XSD.", e);
        }
    }

    // -------------------------------------------------------------------------
    // IRecipeInteropPort — Export
    // -------------------------------------------------------------------------

    @Override
    public void exportRecipe(Recipe recipe, File destination) {
        XmlRecipeDto dto = toDto(recipe);
        try {
            Marshaller m = jaxbContext.createMarshaller();
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            m.marshal(dto, destination);
        } catch (JAXBException e) {
            throw new XmlInteropException(
                    "Error al escribir la receta en el archivo: " + destination.getAbsolutePath(), e);
        }
    }

    // -------------------------------------------------------------------------
    // IRecipeInteropPort — Import (schema-validate + catalogue resolve in one step)
    // -------------------------------------------------------------------------

    @Override
    public Recipe importRecipe(File source, UserId currentAuthor,
                               ICategoryRepository categories,
                               IDifficultyRepository difficulties,
                               IIngredientRepository ingredients,
                               IUnitRepository units) {
        XmlRecipeDto dto = parseAndValidate(source);
        return toDomain(dto, currentAuthor, categories, difficulties, ingredients, units);
    }

    // -------------------------------------------------------------------------
    // Public utility — string-based export/import (not part of the port contract)
    // -------------------------------------------------------------------------

    public String toXml(Recipe recipe) {
        XmlRecipeDto dto = toDto(recipe);
        try {
            Marshaller m = jaxbContext.createMarshaller();
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            StringWriter writer = new StringWriter();
            m.marshal(dto, writer);
            return writer.toString();
        } catch (JAXBException e) {
            throw new XmlInteropException("Error al serializar la receta a XML.", e);
        }
    }

    /** Schema-validates a raw XML string and returns the unmarshalled DTO. */
    public XmlRecipeDto fromXml(String xml) {
        try {
            Unmarshaller u = jaxbContext.createUnmarshaller();
            u.setSchema(schema);
            return (XmlRecipeDto) u.unmarshal(new StringReader(xml));
        } catch (JAXBException e) {
            throw new XmlInteropException("El XML es inválido o no cumple el esquema XSD.", e);
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private XmlRecipeDto parseAndValidate(File file) {
        try {
            Unmarshaller u = jaxbContext.createUnmarshaller();
            u.setSchema(schema);
            return (XmlRecipeDto) u.unmarshal(new StreamSource(file));
        } catch (JAXBException e) {
            throw new XmlInteropException(
                    "El archivo XML es inválido o no cumple el esquema XSD: " + file.getAbsolutePath(), e);
        }
    }

    private XmlRecipeDto toDto(Recipe recipe) {
        XmlRecipeDto dto = new XmlRecipeDto();
        dto.setTitle(recipe.getTitle());
        dto.setDescription(recipe.getDescription());
        dto.setPreparationTimeMinutes(recipe.getPreparationTimeMinutes().value());
        dto.setServings(recipe.getServings().value());
        dto.setCategoryName(recipe.getCategory().getName());
        dto.setDifficultyName(recipe.getDifficulty().getName());

        List<XmlIngredientDto> xmlIngredients = recipe.getIngredients().stream()
                .map(ri -> new XmlIngredientDto(
                        ri.getQuantity(),
                        ri.getUnitAbbreviation() != null ? ri.getUnitAbbreviation() : "",
                        ri.getIngredientName()   != null ? ri.getIngredientName()   : ""))
                .toList();
        dto.setIngredients(xmlIngredients);

        List<XmlStepDto> xmlSteps = recipe.getSteps().stream()
                .map(s -> new XmlStepDto(s.stepOrder(), s.instruction()))
                .toList();
        dto.setSteps(xmlSteps);

        return dto;
    }

    private Recipe toDomain(XmlRecipeDto dto, UserId authorId,
                             ICategoryRepository categoryRepo,
                             IDifficultyRepository difficultyRepo,
                             IIngredientRepository ingredientRepo,
                             IUnitRepository unitRepo) {

        Category category = resolveByName(
                categoryRepo.findAll(), Category::getName, dto.getCategoryName(), "categoría");
        Difficulty difficulty = resolveByName(
                difficultyRepo.findAll(), Difficulty::getName, dto.getDifficultyName(), "dificultad");

        Recipe recipe = new Recipe(
                authorId,
                category,
                difficulty,
                dto.getTitle(),
                dto.getDescription(),
                new PreparationTime(dto.getPreparationTimeMinutes()),
                new Servings(dto.getServings()));

        Map<String, Ingredient> ingredientsByName = ingredientRepo.findAll().stream()
                .collect(Collectors.toMap(i -> i.getName().toLowerCase(), Function.identity()));
        Map<String, Unit> unitsByAbbreviation = unitRepo.findAll().stream()
                .collect(Collectors.toMap(u -> u.getAbbreviation().toLowerCase(), Function.identity()));

        List<RecipeIngredient> domainIngredients = dto.getIngredients().stream()
                .map(xmlIng -> {
                    Ingredient ingredient = ingredientsByName.get(xmlIng.getName().toLowerCase());
                    if (ingredient == null) {
                        throw new XmlInteropException(
                                "Ingrediente no encontrado en el catálogo: '" + xmlIng.getName() + "'.");
                    }
                    Unit unit = unitsByAbbreviation.get(xmlIng.getUnit().toLowerCase());
                    if (unit == null) {
                        throw new XmlInteropException(
                                "Unidad de medida no encontrada por abreviatura: '" + xmlIng.getUnit() + "'.");
                    }
                    return new RecipeIngredient(
                            ingredient.getId(),
                            unit.getId(),
                            xmlIng.getQuantity(),
                            ingredient.getName(),
                            unit.getAbbreviation());
                })
                .toList();
        recipe.syncIngredients(domainIngredients);

        List<RecipeStep> domainSteps = dto.getSteps().stream()
                .map(s -> new RecipeStep(s.getOrder(), s.getInstruction()))
                .toList();
        recipe.syncSteps(domainSteps);

        return recipe;
    }

    private <T> T resolveByName(List<T> all, Function<T, String> nameExtractor,
                                 String name, String entityType) {
        return all.stream()
                .filter(e -> nameExtractor.apply(e).equalsIgnoreCase(name))
                .findFirst()
                .orElseThrow(() -> new XmlInteropException(
                        "No se encontró la " + entityType + " con nombre: '" + name + "'."));
    }

    // -------------------------------------------------------------------------
    // Exception
    // -------------------------------------------------------------------------

    public static class XmlInteropException extends RuntimeException {
        public XmlInteropException(String message) { super(message); }
        public XmlInteropException(String message, Throwable cause) { super(message, cause); }
    }
}
