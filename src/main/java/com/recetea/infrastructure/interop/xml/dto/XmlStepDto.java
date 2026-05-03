package com.recetea.infrastructure.interop.xml.dto;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * JAXB-bound step inside an exported / imported recipe. {@code order}
 * matches {@code RecipeStep.stepOrder} (1-based, unique within a recipe);
 * {@code instruction} is the step text. Mapping is direct — no
 * catalogue lookup, no transformation.
 *
 * <p><b>ES — </b>Paso enlazado a JAXB dentro de una receta exportada
 * / importada. {@code order} se corresponde con
 * {@code RecipeStep.stepOrder} (1-based, único dentro de una receta);
 * {@code instruction} es el texto del paso. El mapeo es directo —
 * sin búsqueda de catálogo, sin transformación.
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

    public XmlStepDto() {}

    public XmlStepDto(int order, String instruction) {
        this.order = order;
        this.instruction = instruction;
    }

    public int getOrder()           { return order; }
    public String getInstruction()  { return instruction; }

    public void setOrder(int order)                { this.order = order; }
    public void setInstruction(String instruction) { this.instruction = instruction; }
}
