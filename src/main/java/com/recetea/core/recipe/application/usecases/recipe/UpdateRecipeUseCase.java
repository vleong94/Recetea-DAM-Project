package com.recetea.core.recipe.application.usecases.recipe;

import com.recetea.core.recipe.application.ports.in.dto.SaveRecipeRequest;
import com.recetea.core.recipe.application.ports.in.recipe.IUpdateRecipeUseCase;
import com.recetea.core.recipe.application.ports.out.category.ICategoryRepository;
import com.recetea.core.recipe.application.ports.out.difficulty.IDifficultyRepository;
import com.recetea.core.recipe.application.ports.out.recipe.IRecipeRepository;
import com.recetea.core.recipe.domain.AuthenticationRequiredException;
import com.recetea.core.recipe.domain.Category;
import com.recetea.core.recipe.domain.Difficulty;
import com.recetea.core.recipe.domain.InvalidRecipeDataException;
import com.recetea.core.recipe.domain.Recipe;
import com.recetea.core.recipe.domain.RecipeIngredient;
import com.recetea.core.recipe.domain.RecipeNotFoundException;
import com.recetea.core.recipe.domain.RecipeStep;
import com.recetea.core.recipe.domain.UnauthorizedRecipeAccessException;
import com.recetea.core.recipe.domain.vo.PreparationTime;
import com.recetea.core.recipe.domain.vo.RecipeId;
import com.recetea.core.recipe.domain.vo.Servings;
import com.recetea.core.shared.application.ports.in.IUserSessionService;
import com.recetea.core.shared.application.ports.out.ITransactionManager;
import com.recetea.core.user.domain.UserId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Edits an existing recipe — load the aggregate, verify the session user
 * owns it, derive the post-edit aggregate by chaining withers + sync
 * calls, and hand it to {@code IRecipeRepository.update} for smart-sync
 * persistence.
 *
 * <p><b>Why the chained withers.</b> {@code Recipe} is a Java 24
 * immutable record; every "mutator" returns a new instance. The chain
 * ({@code .withTitle(...).withDescription(...).syncIngredients(...)})
 * threads the prior result through each step, ending with the aggregate
 * that {@code update} diffs against the persistent state.
 *
 * <p>No {@link com.recetea.core.shared.application.ConcurrencyGuard}
 * here — updates contend less than creates because they touch a single
 * recipe at a time, and the use case is typically driven by the form
 * controller's "Save" button (one click per user, no bulk path).
 *
 * <p><b>ES — </b>Edita una receta existente — carga el agregado,
 * verifica que el usuario de sesión sea su propietario, deriva el
 * agregado post-edición encadenando llamadas de withers + sync, y lo
 * entrega a {@code IRecipeRepository.update} para la persistencia con
 * smart-sync.
 *
 * <p><b>Por qué los withers encadenados.</b> {@code Recipe} es un
 * record inmutable de Java 24; cada "mutador" devuelve una instancia
 * nueva. La cadena
 * ({@code .withTitle(...).withDescription(...).syncIngredients(...)})
 * va pasando el resultado anterior por cada paso, terminando con el
 * agregado que {@code update} comparará con el estado persistente.
 *
 * <p>Aquí no hay {@link com.recetea.core.shared.application.ConcurrencyGuard}
 * — las actualizaciones contienden menos que las creaciones porque
 * tocan una sola receta a la vez, y el caso de uso lo suele
 * disparar el botón "Guardar" del controlador del formulario (un
 * click por usuario, sin ruta masiva).
 */
public class UpdateRecipeUseCase implements IUpdateRecipeUseCase {

    private static final Logger log = LoggerFactory.getLogger(UpdateRecipeUseCase.class);

    private final IRecipeRepository recipeRepository;
    private final ICategoryRepository categoryRepository;
    private final IDifficultyRepository difficultyRepository;
    private final ITransactionManager transactionManager;
    private final IUserSessionService sessionService;

    public UpdateRecipeUseCase(IRecipeRepository recipeRepository,
                               ICategoryRepository categoryRepository,
                               IDifficultyRepository difficultyRepository,
                               ITransactionManager transactionManager,
                               IUserSessionService sessionService) {
        this.recipeRepository = recipeRepository;
        this.categoryRepository = categoryRepository;
        this.difficultyRepository = difficultyRepository;
        this.transactionManager = transactionManager;
        this.sessionService = sessionService;
    }

    @Override
    public void execute(RecipeId recipeId, SaveRecipeRequest request) {
        var validation = request.validate();
        if (!validation.isValid()) {
            log.warn("Validation failed for recipe update (ID: {}): {}", recipeId.value(), validation.errors());
        }
        validation.getOrThrow(InvalidRecipeDataException::new);

        log.info("Updating recipe: {}", recipeId.value());

        transactionManager.execute(() -> {
            Recipe recipe = recipeRepository.findById(recipeId)
                    .orElseThrow(() -> new RecipeNotFoundException(recipeId.value()));

            UserId currentUser = sessionService.getCurrentUserId()
                    .orElseThrow(AuthenticationRequiredException::new);
            if (!recipe.getAuthorId().equals(currentUser)) {
                throw new UnauthorizedRecipeAccessException(
                        "User " + currentUser.value() + " is not authorized to modify recipe " + recipeId.value() + ".");
            }

            Category category = categoryRepository.findById(request.categoryId())
                    .orElseThrow(() -> new IllegalArgumentException("Invalid category ID: " + request.categoryId()));
            Difficulty difficulty = difficultyRepository.findById(request.difficultyId())
                    .orElseThrow(() -> new IllegalArgumentException("Invalid difficulty ID: " + request.difficultyId()));

            // Recipe is an immutable record: every "mutator" returns a new instance.
            // Chaining the withers + sync calls produces the post-edit aggregate, which
            // is what gets persisted.
            Recipe updated = recipe
                    .withTitle(request.title())
                    .withDescription(request.description())
                    .withPreparationTime(new PreparationTime(request.preparationTimeMinutes()))
                    .withServings(new Servings(request.servings()))
                    .withCategory(category)
                    .withDifficulty(difficulty)
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

            recipeRepository.update(updated);
        });

        log.info("Recipe {} updated successfully.", recipeId.value());
    }
}
