package com.recetea.core.recipe.application.usecases.interop;

import com.recetea.core.recipe.application.ports.in.interop.IImportRecipeUseCase;
import com.recetea.core.recipe.application.ports.out.category.ICategoryRepository;
import com.recetea.core.recipe.application.ports.out.difficulty.IDifficultyRepository;
import com.recetea.core.recipe.application.ports.out.ingredient.IIngredientRepository;
import com.recetea.core.recipe.application.ports.out.recipe.IRecipeRepository;
import com.recetea.core.recipe.application.ports.out.unit.IUnitRepository;
import com.recetea.core.recipe.domain.AuthenticationRequiredException;
import com.recetea.core.recipe.domain.Recipe;
import com.recetea.core.recipe.domain.vo.RecipeId;
import com.recetea.core.shared.application.ports.in.IUserSessionService;
import com.recetea.core.shared.application.ports.out.ITransactionManager;
import com.recetea.core.user.domain.UserId;
import com.recetea.infrastructure.interop.xml.XmlInteropAdapter;
import com.recetea.infrastructure.interop.xml.dto.XmlRecipeDto;

import java.io.File;

public class ImportRecipeUseCase implements IImportRecipeUseCase {

    private final IRecipeRepository recipeRepository;
    private final ICategoryRepository categoryRepository;
    private final IDifficultyRepository difficultyRepository;
    private final IIngredientRepository ingredientRepository;
    private final IUnitRepository unitRepository;
    private final ITransactionManager transactionManager;
    private final IUserSessionService sessionService;
    private final XmlInteropAdapter xmlAdapter;

    public ImportRecipeUseCase(IRecipeRepository recipeRepository,
                               ICategoryRepository categoryRepository,
                               IDifficultyRepository difficultyRepository,
                               IIngredientRepository ingredientRepository,
                               IUnitRepository unitRepository,
                               ITransactionManager transactionManager,
                               IUserSessionService sessionService,
                               XmlInteropAdapter xmlAdapter) {
        this.recipeRepository = recipeRepository;
        this.categoryRepository = categoryRepository;
        this.difficultyRepository = difficultyRepository;
        this.ingredientRepository = ingredientRepository;
        this.unitRepository = unitRepository;
        this.transactionManager = transactionManager;
        this.sessionService = sessionService;
        this.xmlAdapter = xmlAdapter;
    }

    @Override
    public RecipeId execute(File source) {
        // Parse and schema-validate outside the transaction — no DB involved yet.
        XmlRecipeDto dto = xmlAdapter.fromFile(source);

        UserId currentUser = sessionService.getCurrentUserId()
                .orElseThrow(AuthenticationRequiredException::new);

        return transactionManager.execute(() -> {
            Recipe recipe = xmlAdapter.toDomain(
                    dto,
                    currentUser,
                    categoryRepository,
                    difficultyRepository,
                    ingredientRepository,
                    unitRepository);

            recipeRepository.save(recipe);
            return recipe.getId();
        });
    }
}
