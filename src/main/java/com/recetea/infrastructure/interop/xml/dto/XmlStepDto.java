package com.recetea.infrastructure.interop.xml.dto;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * XML-DTO for a single preparation step in a recipe export/import.
 * Maps to XSD type {@code StepType} in recipe.xsd.
 * Not a domain object — lives exclusively in the interop layer.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "StepType",
         namespace = "https://recetea.com/xml/recipe",
         propOrder = {"order", "instruction"})
public class XmlStepDto {

    @XmlElement(required = true, namespace = "https://recetea.com/xml/recipe")
    private int order;

    @XmlElement(required = true, namespace = "https://recetea.com/xml/recipe")
    private String instruction;

    /** Required by JAXB unmarshalling. */
    public XmlStepDto() {}

    public XmlStepDto(int order, String instruction) {
        this.order = order;
        this.instruction = instruction;
    }

    public int getOrder()           { return order; }
    public String getInstruction()  { return instruction; }

    public void setOrder(int order)                  { this.order = order; }
    public void setInstruction(String instruction)   { this.instruction = instruction; }
}
