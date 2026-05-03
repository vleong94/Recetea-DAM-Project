package com.recetea.infrastructure.interop.xml.dto;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

import java.util.List;

/**
 * JAXB-bound representation of a recipe document. Backs the root element
 * {@code <recipe>} in namespace {@code https://recetea.com/xml/recipe}
 * defined by {@code recipe.xsd}. Field-level access ({@code XmlAccessType
 * .FIELD}) keeps the bean shape minimal — getters and setters are present
 * only because some JAXB providers prefer them, not as part of any contract.
 *
 * <p>The {@code propOrder} mirrors the XSD's {@code <sequence>} so the
 * marshalled output matches the schema validator's expectation. Adding a
 * new element requires updates in three places: the XSD, this property
 * order list, and the corresponding domain mapping in
 * {@code XmlInteropAdapter.toDto / toDomain}.
 *
 * <p>Regular class (not a record) because JAXB needs a no-arg constructor
 * + mutable fields. The application layer never imports this type — it
 * stays inside the adapter.
 *
 * <p><b>ES — </b>Representación enlazada a JAXB de un documento de
 * receta. Respalda el elemento raíz {@code <recipe>} en el espacio de
 * nombres {@code https://recetea.com/xml/recipe} definido por
 * {@code recipe.xsd}. El acceso a nivel de campo
 * ({@code XmlAccessType.FIELD}) mantiene la forma del bean mínima —
 * los getters y setters están sólo porque algunos proveedores JAXB
 * los prefieren, no como parte de ningún contrato.
 *
 * <p>El {@code propOrder} refleja el {@code <sequence>} del XSD para
 * que la salida del marshalling cumpla la expectativa del validador
 * de esquema. Añadir un elemento nuevo requiere actualizaciones en
 * tres sitios: el XSD, esta lista de orden de propiedades, y el
 * mapeo de dominio correspondiente en
 * {@code XmlInteropAdapter.toDto / toDomain}.
 *
 * <p>Clase normal (no record) porque JAXB necesita un constructor
 * sin argumentos + campos mutables. La capa de aplicación nunca
 * importa este tipo — queda dentro del adaptador.
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

    @XmlElement(namespace = "https://recetea.com/xml/recipe")
    private String description;

    @XmlElement(required = true, namespace = "https://recetea.com/xml/recipe")
    private int preparationTimeMinutes;

    @XmlElement(required = true, namespace = "https://recetea.com/xml/recipe")
    private int servings;

    @XmlElement(required = true, namespace = "https://recetea.com/xml/recipe")
    private String categoryName;

    @XmlElement(required = true, namespace = "https://recetea.com/xml/recipe")
    private String difficultyName;

    @XmlElementWrapper(name = "ingredients", namespace = "https://recetea.com/xml/recipe")
    @XmlElement(name = "ingredient", namespace = "https://recetea.com/xml/recipe", required = true)
    private List<XmlIngredientDto> ingredients;

    @XmlElementWrapper(name = "steps", namespace = "https://recetea.com/xml/recipe")
    @XmlElement(name = "step", namespace = "https://recetea.com/xml/recipe", required = true)
    private List<XmlStepDto> steps;

    public XmlRecipeDto() {}

    public String getTitle()                        { return title; }
    public String getDescription()                  { return description; }
    public int getPreparationTimeMinutes()          { return preparationTimeMinutes; }
    public int getServings()                        { return servings; }
    public String getCategoryName()                 { return categoryName; }
    public String getDifficultyName()               { return difficultyName; }
    public List<XmlIngredientDto> getIngredients()  { return ingredients; }
    public List<XmlStepDto> getSteps()              { return steps; }

    public void setTitle(String title)                               { this.title = title; }
    public void setDescription(String description)                   { this.description = description; }
    public void setPreparationTimeMinutes(int preparationTimeMinutes){ this.preparationTimeMinutes = preparationTimeMinutes; }
    public void setServings(int servings)                            { this.servings = servings; }
    public void setCategoryName(String categoryName)                 { this.categoryName = categoryName; }
    public void setDifficultyName(String difficultyName)             { this.difficultyName = difficultyName; }
    public void setIngredients(List<XmlIngredientDto> ingredients)   { this.ingredients = ingredients; }
    public void setSteps(List<XmlStepDto> steps)                     { this.steps = steps; }
}
