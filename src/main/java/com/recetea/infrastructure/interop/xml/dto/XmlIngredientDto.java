package com.recetea.infrastructure.interop.xml.dto;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

import java.math.BigDecimal;

/**
 * XML-DTO for a single ingredient line in a recipe export/import.
 * Maps to XSD type {@code IngredientType} in recipe.xsd.
 * Not a domain object — lives exclusively in the interop layer.
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

    /** Required by JAXB unmarshalling. */
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
