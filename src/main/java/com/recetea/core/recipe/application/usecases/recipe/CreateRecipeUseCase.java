package com.recetea.core.recipe.application.usecases.recipe;

import com.recetea.core.recipe.application.ports.in.dto.SaveRecipeRequest;
import com.recetea.core.recipe.application.ports.in.recipe.ICreateRecipeUseCase;
import com.recetea.core.recipe.application.ports.out.category.ICategoryRepository;
import com.recetea.core.recipe.application.ports.out.difficulty.IDifficultyRepository;
import com.recetea.core.recipe.application.ports.out.recipe.IRecipeRepository;
import com.recetea.core.recipe.domain.AuthenticationRequiredException;
import com.recetea.core.recipe.domain.Category;
import com.recetea.core.recipe.domain.Difficulty;
import com.recetea.core.recipe.domain.InvalidRecipeDataException;
import com.recetea.core.recipe.domain.Recipe;
import com.recetea.core.recipe.domain.RecipeIngredient;
import com.recetea.core.recipe.domain.RecipeStep;
import com.recetea.core.recipe.domain.vo.PreparationTime;
import com.recetea.core.recipe.domain.vo.RecipeId;
import com.recetea.core.recipe.domain.vo.Servings;
import com.recetea.core.shared.application.ConcurrencyGuard;
import com.recetea.core.shared.application.ports.in.IUserSessionService;
import com.recetea.core.shared.application.ports.out.ITransactionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Persists a fresh recipe — DTO validation, catalogue resolution, aggregate
 * construction, and INSERT all happen inside one transaction.
 *
 * <p>Authorship is sourced from {@code IUserSessionService} (not the DTO)
 * so a crafted payload can't impersonate another user. The catalogue
 * lookups for category and difficulty execute inside the transaction;
 * they're cheap (the JDBC adapter caches the catalogue) but pulling them
 * inside keeps the transaction self-contained.
 *
 * <p>Wrapped in {@code ConcurrencyGuard.run(...)} because aggregate
 * construction touches several rows (the parent + every ingredient + every
 * step + media). The guard caps concurrent heavy writes at 80% of the
 * Hikari pool size so a burst of submissions can't starve other use cases
 * by holding every connection simultaneously.
 *
 * <p><b>ES — </b>Persiste una receta nueva — validación del DTO,
 * resolución de catálogo, construcción del agregado e INSERT, todo
 * dentro de una sola transacción.
 *
 * <p>La autoría se obtiene de {@code IUserSessionService} (no del
 * DTO) para que un payload manipulado no pueda suplantar a otro
 * usuario. Las búsquedas de catálogo para categoría y dificultad se
 * ejecutan dentro de la transacción; son baratas (el adaptador JDBC
 * cachea el catálogo) pero meterlas dentro mantiene la transacción
 * autocontenida.
 *
 * <p>Envuelto en {@code ConcurrencyGuard.run(...)} porque la
 * construcción del agregado toca varias filas (el padre + cada
 * ingrediente + cada paso + multimedia). El guard acota las
 * escrituras pesadas concurrentes al 80% del tamaño del pool de
 * Hikari, de modo que una ráfaga de envíos no pueda matar de
 * hambre a otros casos de uso reteniendo todas las conexiones a la
 * vez.
 */
public class CreateRecipeUseCase implements ICreateRecipeUseCase {

    private static final Logger log = LoggerFactory.getLogger(CreateRecipeUseCase.class);

    private final IRecipeRepository recipeRepository;
    private final ICategoryRepository categoryRepository;
    private final IDifficultyRepository difficultyRepository;
    private final ITransactionManager transactionManager;
    private final IUserSessionService sessionService;
    private final ConcurrencyGuard concurrencyGuard;

    public CreateRecipeUseCase(IRecipeRepository recipeRepository,
                               ICategoryRepository categoryRepository,
                               IDifficultyRepository difficultyRepository,
                               ITransactionManager transactionManager,
                               IUserSessionService sessionService,
                               ConcurrencyGuard concurrencyGuard) {
        this.recipeRepository = recipeRepository;
        this.categoryRepository = categoryRepository;
        this.difficultyRepository = difficultyRepository;
        this.transactionManager = transactionManager;
        this.sessionService = sessionService;
        this.concurrencyGuard = concurrencyGuard;
    }

    @Override
    public RecipeId execute(SaveRecipeRequest request) {
        var validation = request.validate();
        if (!validation.isValid()) {
            log.warn("Validation failed for recipe creation: {}", validation.errors());
        }
        validation.getOrThrow(InvalidRecipeDataException::new);

        log.info("Creating recipe: '{}'", request.title());

        RecipeId newId = concurrencyGuard.run(() -> transactionManager.execute(() -> {
            Category category = categoryRepository.findById(request.categoryId())
                    .orElseThrow(() -> new IllegalArgumentException("Invalid category ID: " + request.categoryId()));
            Difficulty difficulty = difficultyRepository.findById(request.difficultyId())
                    .orElseThrow(() -> new IllegalArgumentException("Invalid difficulty ID: " + request.difficultyId()));

            Recipe recipe = new Recipe(
                    sessionService.getCurrentUserId()
                            .orElseThrow(AuthenticationRequiredException::new),
                    category,
                    difficulty,
                    request.title(),
                    request.description(),
                    new PreparationTime(request.preparationTimeMinutes()),
                    new Servings(request.servings())
            )
                    .syncIngredients(request.ingredients().stream()
                            .map(ir -> new RecipeIngredient(
                                    ir.ingredientId(),
                                    ir.unitId(),
                                    ir.quantity(),
                                    ir.ingredientName(),
                                    ir.unitName()))
                            .toList())
                    .syncSteps(request.steps().stream()
                            .map(sr -> new RecipeStep(sr.stepOrder(), sr.instruction()))
                            .toList());

            return recipeRepository.save(recipe);
        }));

        log.info("Recipe created successfully. ID: {}", newId.value());
        return newId;
    }
}
