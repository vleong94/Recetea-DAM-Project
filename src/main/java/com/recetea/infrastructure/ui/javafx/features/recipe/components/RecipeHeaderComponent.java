package com.recetea.infrastructure.ui.javafx.features.recipe.components;

import com.recetea.core.recipe.domain.Category;
import com.recetea.core.recipe.domain.Difficulty;
import com.recetea.core.recipe.domain.vo.CategoryId;
import com.recetea.core.recipe.domain.vo.DifficultyId;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.util.List;

public class RecipeHeaderComponent extends VBox {

    @FXML private TextField titleField;
    @FXML private TextField prepTimeField;
    @FXML private TextField servingsField;
    @FXML private TextArea descriptionArea;

    @FXML private ComboBox<Category> categoryComboBox;
    @FXML private ComboBox<Difficulty> difficultyComboBox;

    public RecipeHeaderComponent() {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/recetea/infrastructure/ui/javafx/fxml/features/recipe/components/recipe_header.fxml"));
        loader.setRoot(this);
        loader.setController(this);
        try {
            loader.load();
        } catch (IOException e) {
            throw new RuntimeException("Infrastructure Failure: Imposible instanciar el componente visual RecipeHeaderComponent.", e);
        }
    }

    public void initTaxonomy(List<Category> categories, List<Difficulty> difficulties) {
        if (categories != null) categoryComboBox.getItems().setAll(categories);
        if (difficulties != null) difficultyComboBox.getItems().setAll(difficulties);
    }

    public boolean isValid() {
        String title = titleField.getText();
        if (title == null || title.trim().isEmpty()) return false;
        if (categoryComboBox.getValue() == null) return false;
        if (difficultyComboBox.getValue() == null) return false;
        try {
            int prep = Integer.parseInt(prepTimeField.getText().trim());
            int serv = Integer.parseInt(servingsField.getText().trim());
            return prep > 0 && serv > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public String getTitle() { return titleField.getText().trim(); }

    public String getDescription() {
        return descriptionArea.getText() != null ? descriptionArea.getText().trim() : "";
    }

    public int getPrepTime() { return Integer.parseInt(prepTimeField.getText().trim()); }

    public int getServings() { return Integer.parseInt(servingsField.getText().trim()); }

    public CategoryId getSelectedCategoryId() {
        return categoryComboBox.getValue().getId();
    }

    public DifficultyId getSelectedDifficultyId() {
        return difficultyComboBox.getValue().getId();
    }

    public void setData(String title, String description, int prepTime, int servings,
                        CategoryId categoryId, DifficultyId difficultyId) {
        titleField.setText(title);
        descriptionArea.setText(description);
        prepTimeField.setText(String.valueOf(prepTime));
        servingsField.setText(String.valueOf(servings));

        categoryComboBox.getItems().stream()
                .filter(c -> c.getId().equals(categoryId))
                .findFirst()
                .ifPresent(categoryComboBox::setValue);

        difficultyComboBox.getItems().stream()
                .filter(d -> d.getId().equals(difficultyId))
                .findFirst()
                .ifPresent(difficultyComboBox::setValue);
    }
}
