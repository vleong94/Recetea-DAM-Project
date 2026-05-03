package com.recetea.infrastructure.interop.xml.dto;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

import java.math.BigDecimal;

/**
 * JAXB-bound ingredient line inside an exported / imported recipe.
 * {@code unit} carries the unit's abbreviation (e.g. "g") rather than the
 * full name — abbreviations are more compact and language-agnostic in
 * the XML payload. {@code XmlInteropAdapter} resolves both
 * {@code name → Ingredient} and {@code unit → Unit} via case-insensitive
 * catalogue lookup.
 *
 * <p><b>ES — </b>Línea de ingrediente enlazada a JAXB dentro de una
 * receta exportada / importada. {@code unit} lleva la abreviatura de
 * la unidad (p. ej. "g") en lugar del nombre completo — las
 * abreviaturas son más compactas y agnósticas al idioma en el
 * payload XML. {@code XmlInteropAdapter} resuelve tanto
 * {@code name → Ingredient} como {@code unit → Unit} vía búsqueda
 * de catálogo insensible a mayúsculas.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "IngredientType",
         namespace = "https://recetea.com/xml/recipe",
         propOrder = {"quantity", "unit", "name"})
public class XmlIngredientDto {

    @XmlElement(required = true, namespace = "https://recetea.com/xml/recipe")
    private BigDecimal quantity;

    @XmlElement(required = true, namespace = "https://recetea.com/xml/recipe")
    private String unit;

    @XmlElement(required = true, namespace = "https://recetea.com/xml/recipe")
    private String name;

    public XmlIngredientDto() {}

    public XmlIngredientDto(BigDecimal quantity, String unit, String name) {
        this.quantity = quantity;
        this.unit = unit;
        this.name = name;
    }

    public BigDecimal getQuantity() { return quantity; }
    public String getUnit()         { return unit; }
    public String getName()         { return name; }

    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    public void setUnit(String unit)             { this.unit = unit; }
    public void setName(String name)             { this.name = name; }
}
