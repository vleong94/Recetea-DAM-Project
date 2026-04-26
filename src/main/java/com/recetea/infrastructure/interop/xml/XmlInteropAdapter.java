package com.recetea.infrastructure.interop.xml;

import com.recetea.core.recipe.application.ports.out.interop.IRecipeInteropPort;
import com.recetea.core.recipe.application.ports.out.interop.dto.XmlIngredientDto;
import com.recetea.core.recipe.application.ports.out.interop.dto.XmlRecipeDto;
import com.recetea.core.recipe.application.ports.out.interop.dto.XmlStepDto;
import com.recetea.core.recipe.domain.Recipe;

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

    @Override
    public XmlRecipeDto readFromSource(File source) {
        try {
            Unmarshaller u = jaxbContext.createUnmarshaller();
            u.setSchema(schema);
            return (XmlRecipeDto) u.unmarshal(new StreamSource(source));
        } catch (JAXBException e) {
            throw new XmlInteropException(
                    "El archivo XML es inválido o no cumple el esquema XSD: " + source.getAbsolutePath(), e);
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

    public static class XmlInteropException extends RuntimeException {
        public XmlInteropException(String message) { super(message); }
        public XmlInteropException(String message, Throwable cause) { super(message, cause); }
    }
}
