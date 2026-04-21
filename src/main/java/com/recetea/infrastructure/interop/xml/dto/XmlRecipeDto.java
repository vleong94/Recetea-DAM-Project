package com.recetea.infrastructure.interop.xml.dto;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

import java.util.List;

/**
 * XML-DTO root for a recipe export/import document.
 * Maps to XSD element {@code <recipe>} / type {@code RecipeType} in recipe.xsd.
 *
 * Separation of concerns: this class belongs exclusively to the interop layer.
 * Domain objects (Recipe aggregate, value objects) must never be referenced here.
 */
@XmlRootElement(name = "recipe", namespace = "https://recetea.com/xml/recipe")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "RecipeType",
         namespace = "https://recetea.com/xml/recipe",
         propOrder = {
             "title", "description", "preparationTimeMinutes",
             "servings", "categoryName", "difficultyName",
             "ingredients", "steps"
         })
public class XmlRecipeDto {

    @XmlElement(required = true, namespace = "https://recetea.com/xml/recipe")
    private String title;

    /** Optional — maps to xs:string with minOccurs="0" in the XSD. */
    @XmlElement(namespace = "https://recetea.com/xml/recipe")
    private String description;

    /** Mirrors PreparationTime value object constraint: min value 1. */
    @XmlElement(required = true, namespace = "https://recetea.com/xml/recipe")
    private int preparationTimeMinutes;

    /** Mirrors Servings value object constraint: min value 1. */
    @XmlElement(required = true, namespace = "https://recetea.com/xml/recipe")
    private int servings;

    @XmlElement(required = true, namespace = "https://recetea.com/xml/recipe")
    private String categoryName;

    @XmlElement(required = true, namespace = "https://recetea.com/xml/recipe")
    private String difficultyName;

    /** At least one ingredient required — mirrors Recipe.syncIngredients() invariant. */
    @XmlElementWrapper(name = "ingredients", namespace = "https://recetea.com/xml/recipe")
    @XmlElement(name = "ingredient", namespace = "https://recetea.com/xml/recipe", required = true)
    private List<XmlIngredientDto> ingredients;

    /** At least one step required — mirrors Recipe.syncSteps() invariant. */
    @XmlElementWrapper(name = "steps", namespace = "https://recetea.com/xml/recipe")
    @XmlElement(name = "step", namespace = "https://recetea.com/xml/recipe", required = true)
    private List<XmlStepDto> steps;

    /** Required by JAXB unmarshalling. */
    public XmlRecipeDto() {}

    public String getTitle()                        { return title; }
    public String getDescription()                  { return description; }
    public int getPreparationTimeMinutes()          { return preparationTimeMinutes; }
    public int getServings()                        { return servings; }
    public String getCategoryName()                 { return categoryName; }
    public String getDifficultyName()               { return difficultyName; }
    public List<XmlIngredientDto> getIngredients()  { return ingredients; }
    public List<XmlStepDto> getSteps()              { return steps; }

    public void setTitle(String title)                                      { this.title = title; }
    public void setDescription(String description)                          { this.description = description; }
    public void setPreparationTimeMinutes(int preparationTimeMinutes)       { this.preparationTimeMinutes = preparationTimeMinutes; }
    public void setServings(int servings)                                   { this.servings = servings; }
    public void setCategoryName(String categoryName)                        { this.categoryName = categoryName; }
    public void setDifficultyName(String difficultyName)                    { this.difficultyName = difficultyName; }
    public void setIngredients(List<XmlIngredientDto> ingredients)          { this.ingredients = ingredients; }
    public void setSteps(List<XmlStepDto> steps)                           { this.steps = steps; }
}
