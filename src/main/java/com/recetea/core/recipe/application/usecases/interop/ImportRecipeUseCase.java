package com.recetea.core.recipe.application.usecases.interop;

import com.recetea.core.recipe.application.ports.in.interop.IImportRecipeUseCase;
import com.recetea.core.recipe.application.ports.out.category.ICategoryRepository;
import com.recetea.core.recipe.application.ports.out.difficulty.IDifficultyRepository;
import com.recetea.core.recipe.application.ports.out.ingredient.IIngredientRepository;
import com.recetea.core.recipe.application.ports.out.interop.IRecipeInteropPort;
import com.recetea.core.recipe.application.ports.out.interop.dto.XmlIngredientDto;
import com.recetea.core.recipe.application.ports.out.interop.dto.XmlRecipeDto;
import com.recetea.core.recipe.application.ports.out.recipe.IRecipeRepository;
import com.recetea.core.recipe.application.ports.out.unit.IUnitRepository;
import com.recetea.core.recipe.domain.AuthenticationRequiredException;
import com.recetea.core.recipe.domain.Category;
import com.recetea.core.recipe.domain.Difficulty;
import com.recetea.core.recipe.domain.Ingredient;
import com.recetea.core.recipe.domain.InvalidIngredientException;
import com.recetea.core.recipe.domain.Recipe;
import com.recetea.core.recipe.domain.RecipeIngredient;
import com.recetea.core.recipe.domain.RecipeStep;
import com.recetea.core.recipe.domain.Unit;
import com.recetea.core.recipe.domain.vo.PreparationTime;
import com.recetea.core.recipe.domain.vo.RecipeId;
import com.recetea.core.recipe.domain.vo.Servings;
import com.recetea.core.shared.application.ports.in.IUserSessionService;
import com.recetea.core.shared.application.ports.out.ITransactionManager;
import com.recetea.core.user.domain.UserId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ImportRecipeUseCase implements IImportRecipeUseCase {

    private static final Logger log = LoggerFactory.getLogger(ImportRecipeUseCase.class);

    private final IRecipeRepository recipeRepository;
    private final ICategoryRepository categoryRepository;
    private final IDifficultyRepository difficultyRepository;
    private final IIngredientRepository ingredientRepository;
    private final IUnitRepository unitRepository;
    private final ITransactionManager transactionManager;
    private final IUserSessionService sessionService;
    private final IRecipeInteropPort interopPort;

    public ImportRecipeUseCase(IRecipeRepository recipeRepository,
                               ICategoryRepository categoryRepository,
                               IDifficultyRepository difficultyRepository,
                               IIngredientRepository ingredientRepository,
                               IUnitRepository unitRepository,
                               ITransactionManager transactionManager,
                               IUserSessionService sessionService,
                               IRecipeInteropPort interopPort) {
        this.recipeRepository    = recipeRepository;
        this.categoryRepository  = categoryRepository;
        this.difficultyRepository = difficultyRepository;
        this.ingredientRepository = ingredientRepository;
        this.unitRepository      = unitRepository;
        this.transactionManager  = transactionManager;
        this.sessionService      = sessionService;
        this.interopPort         = interopPort;
    }

    @Override
    public RecipeId execute(File source) {
        UserId currentUser = sessionService.getCurrentUserId()
                .orElseThrow(AuthenticationRequiredException::new);

        log.info("Importing recipe from '{}' for user: {}", source.getName(), currentUser.value());

        XmlRecipeDto dto = interopPort.readFromSource(source);
        Recipe recipe = toDomain(dto, currentUser);

        RecipeId newId = transactionManager.execute(() -> {
            recipeRepository.save(recipe);
            return recipe.getId();
        });

        log.info("Recipe imported successfully. ID: {}", newId.value());
        return newId;
    }

    private Recipe toDomain(XmlRecipeDto dto, UserId authorId) {
        Category category = categoryRepository.findAll().stream()
                .filter(c -> c.getName().equalsIgnoreCase(dto.getCategoryName()))
                .findFirst()
                .orElseThrow(() -> new InvalidIngredientException(
                        "Category not found in catalogue: '" + dto.getCategoryName() + "'."));

        Difficulty difficulty = difficultyRepository.findAll().stream()
                .filter(d -> d.getName().equalsIgnoreCase(dto.getDifficultyName()))
                .findFirst()
                .orElseThrow(() -> new InvalidIngredientException(
                        "Difficulty not found in catalogue: '" + dto.getDifficultyName() + "'."));

        Recipe recipe = new Recipe(
                authorId, category, difficulty,
                dto.getTitle(), dto.getDescription(),
                new PreparationTime(dto.getPreparationTimeMinutes()),
                new Servings(dto.getServings()));

        Map<String, Ingredient> ingredientsByName = ingredientRepository.findAll().stream()
                .collect(Collectors.toMap(i -> i.getName().toLowerCase(), Function.identity()));
        Map<String, Unit> unitsByAbbreviation = unitRepository.findAll().stream()
                .collect(Collectors.toMap(u -> u.getAbbreviation().toLowerCase(), Function.identity()));

        List<RecipeIngredient> domainIngredients = dto.getIngredients().stream()
                .map(xmlIng -> resolveIngredient(xmlIng, ingredientsByName, unitsByAbbreviation))
                .toList();
        recipe.syncIngredients(domainIngredients);

        List<RecipeStep> domainSteps = dto.getSteps().stream()
                .map(s -> new RecipeStep(s.getOrder(), s.getInstruction()))
                .toList();
        recipe.syncSteps(domainSteps);

        return recipe;
    }

    private RecipeIngredient resolveIngredient(XmlIngredientDto xmlIng,
                                               Map<String, Ingredient> ingredientsByName,
                                               Map<String, Unit> unitsByAbbreviation) {
        Ingredient ingredient = ingredientsByName.get(xmlIng.getName().toLowerCase());
        if (ingredient == null) {
            throw new InvalidIngredientException(
                    "Ingredient not found in catalogue: '" + xmlIng.getName() + "'.");
        }
        Unit unit = unitsByAbbreviation.get(xmlIng.getUnit().toLowerCase());
        if (unit == null) {
            throw new InvalidIngredientException(
                    "Unit of measure not found by abbreviation: '" + xmlIng.getUnit() + "'.");
        }
        return new RecipeIngredient(
                ingredient.getId(), unit.getId(), xmlIng.getQuantity(),
                ingredient.getName(), unit.getAbbreviation());
    }
}
