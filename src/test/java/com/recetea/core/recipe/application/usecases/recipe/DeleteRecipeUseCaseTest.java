package com.recetea.core.recipe.application.usecases.recipe;

import com.recetea.core.recipe.application.ports.out.recipe.IRecipeRepository;
import com.recetea.core.recipe.domain.AuthenticationRequiredException;
import com.recetea.core.recipe.domain.Category;
import com.recetea.core.recipe.domain.Difficulty;
import com.recetea.core.recipe.domain.Recipe;
import com.recetea.core.recipe.domain.RecipeNotFoundException;
import com.recetea.core.recipe.domain.UnauthorizedRecipeAccessException;
import com.recetea.core.recipe.domain.vo.CategoryId;
import com.recetea.core.recipe.domain.vo.DifficultyId;
import com.recetea.core.recipe.domain.vo.PreparationTime;
import com.recetea.core.recipe.domain.vo.RecipeId;
import com.recetea.core.recipe.domain.vo.Servings;
import com.recetea.core.shared.application.ports.in.IUserSessionService;
import com.recetea.core.shared.application.ports.out.ITransactionManager;
import com.recetea.core.social.application.ports.out.IFavoriteRepository;
import com.recetea.core.user.domain.UserId;
import com.recetea.infrastructure.persistence.recipe.jdbc.JdbcTransactionManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.sql.Connection;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Cross-module atomicity + ScopedValue propagation contract for
 * {@link DeleteRecipeUseCase}. Complements {@link OwnershipSecurityTest}'s authorisation
 * coverage with focused assertions on:
 *
 * <ul>
 *   <li>Both repository calls happen <em>inside</em> the lambda passed to
 *       {@link ITransactionManager#execute(Runnable)} — transaction boundary participation.</li>
 *   <li>Both repositories observe the <em>same</em> {@link JdbcTransactionManager#CONNECTION}
 *       binding while inside the transaction — proves the ScopedValue is propagated across
 *       the recipe + social bounded contexts under one transaction.</li>
 *   <li>A failure during favourite cleanup short-circuits the recipe delete (atomic rollback).</li>
 *   <li>Authorisation failures (no session, recipe missing, non-owner) skip both deletes.</li>
 * </ul>
 *
 * <p>The transaction manager is mocked so the test can assert the use case's contract
 * with the boundary, not the internals of the JDBC layer (those are covered by
 * {@code VirtualThreadPinningTest} and the integration suites).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("DeleteRecipeUseCase — atomic cross-module deletion + ScopedValue propagation")
class DeleteRecipeUseCaseTest {

    @Mock private IRecipeRepository    recipeRepository;
    @Mock private IFavoriteRepository  favoriteRepository;
    @Mock private ITransactionManager  transactionManager;
    @Mock private IUserSessionService  sessionService;

    private DeleteRecipeUseCase useCase;
    private Recipe ownerRecipe;

    private static final UserId   OWNER     = new UserId(1);
    private static final UserId   INTRUDER  = new UserId(2);
    private static final RecipeId RECIPE_ID = new RecipeId(10);

    @BeforeEach
    void setUp() {
        useCase = new DeleteRecipeUseCase(recipeRepository, favoriteRepository, transactionManager, sessionService);
        ownerRecipe = new Recipe(
                RECIPE_ID, OWNER,
                new Category(new CategoryId(1), "Appetizers"),
                new Difficulty(new DifficultyId(1), "Easy"),
                "Owner's Recipe", "Description",
                new PreparationTime(20), new Servings(2),
                BigDecimal.ZERO, 0);
    }

    /** Runs the lambda inline so verify() can observe the calls made within the boundary. */
    private void stubInlineTransaction() {
        doAnswer(inv -> { ((Runnable) inv.getArgument(0)).run(); return null; })
                .when(transactionManager).execute(any(Runnable.class));
    }

    // ── Happy path ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("execute: enters one transaction and deletes favorites BEFORE the recipe row")
    void execute_runsBothDeletesInOrderWithinSingleTransaction() {
        stubInlineTransaction();
        when(recipeRepository.findById(RECIPE_ID)).thenReturn(Optional.of(ownerRecipe));
        when(sessionService.getCurrentUserId()).thenReturn(Optional.of(OWNER));

        useCase.execute(RECIPE_ID);

        // Single transaction boundary entered.
        verify(transactionManager, times(1)).execute(any(Runnable.class));

        // Strict ordering: favourites first, recipe after.
        InOrder order = inOrder(favoriteRepository, recipeRepository);
        order.verify(favoriteRepository).deleteAllByRecipeId(RECIPE_ID);
        order.verify(recipeRepository).delete(RECIPE_ID);
    }

    // ── Transaction participation: both calls happen inside the lambda ──────

    @Test
    @DisplayName("execute: both repository calls fire while the transaction lambda is on the stack")
    void execute_bothRepositoryCallsHappenInsideTransactionLambda() {
        AtomicBoolean inTransaction = new AtomicBoolean(false);
        AtomicBoolean favoritesSawTx = new AtomicBoolean(false);
        AtomicBoolean recipeSawTx    = new AtomicBoolean(false);

        // The mock TM flips the flag while running the lambda — repository mocks read it
        // at invocation time, so we observe whether each call landed inside the boundary.
        doAnswer(inv -> {
            inTransaction.set(true);
            try {
                ((Runnable) inv.getArgument(0)).run();
            } finally {
                inTransaction.set(false);
            }
            return null;
        }).when(transactionManager).execute(any(Runnable.class));

        doAnswer(inv -> { favoritesSawTx.set(inTransaction.get()); return null; })
                .when(favoriteRepository).deleteAllByRecipeId(RECIPE_ID);
        doAnswer(inv -> { recipeSawTx.set(inTransaction.get()); return null; })
                .when(recipeRepository).delete(RECIPE_ID);

        when(recipeRepository.findById(RECIPE_ID)).thenReturn(Optional.of(ownerRecipe));
        when(sessionService.getCurrentUserId()).thenReturn(Optional.of(OWNER));

        useCase.execute(RECIPE_ID);

        assertTrue(favoritesSawTx.get(), "favoriteRepository.deleteAllByRecipeId must run inside the transaction lambda");
        assertTrue(recipeSawTx.get(),    "recipeRepository.delete must run inside the transaction lambda");
    }

    // ── ScopedValue propagation: both repos see the same CONNECTION binding ─

    @Test
    @DisplayName("execute: favoriteRepository and recipeRepository observe the same ScopedValue connection")
    void execute_bothRepositoriesObserveSameScopedValueBinding() throws Exception {
        Connection sharedConnection = mock(Connection.class);
        AtomicReference<Connection> favoritesObserved = new AtomicReference<>();
        AtomicReference<Connection> recipeObserved    = new AtomicReference<>();

        // Simulate what JdbcTransactionManager.doExecute does: bind CONNECTION before
        // running the action, unbind on every exit path. Repository mocks then read
        // the live binding to confirm propagation.
        doAnswer(inv -> {
            Runnable action = inv.getArgument(0);
            ScopedValue.where(JdbcTransactionManager.CONNECTION, sharedConnection).run(action::run);
            return null;
        }).when(transactionManager).execute(any(Runnable.class));

        doAnswer(inv -> {
            assertTrue(JdbcTransactionManager.CONNECTION.isBound(),
                    "ScopedValue CONNECTION must be bound when favoriteRepository fires");
            favoritesObserved.set(JdbcTransactionManager.CONNECTION.get());
            return null;
        }).when(favoriteRepository).deleteAllByRecipeId(RECIPE_ID);
        doAnswer(inv -> {
            assertTrue(JdbcTransactionManager.CONNECTION.isBound(),
                    "ScopedValue CONNECTION must be bound when recipeRepository fires");
            recipeObserved.set(JdbcTransactionManager.CONNECTION.get());
            return null;
        }).when(recipeRepository).delete(RECIPE_ID);

        when(recipeRepository.findById(RECIPE_ID)).thenReturn(Optional.of(ownerRecipe));
        when(sessionService.getCurrentUserId()).thenReturn(Optional.of(OWNER));

        useCase.execute(RECIPE_ID);

        assertSame(sharedConnection, favoritesObserved.get(),
                "favoriteRepository should observe the transaction's bound Connection");
        assertSame(sharedConnection, recipeObserved.get(),
                "recipeRepository should observe the SAME bound Connection — proving ScopedValue propagation across modules");

        // After the transaction completes, the binding must be unbound again.
        assertFalse(JdbcTransactionManager.CONNECTION.isBound(),
                "ScopedValue must auto-unbind once the transaction lambda returns");
    }

    // ── Atomic rollback: failure mid-transaction skips downstream work ──────

    @Test
    @DisplayName("execute: a failure during favorites cleanup propagates and skips the recipe delete")
    void execute_favoriteCleanupFailure_skipsRecipeDelete() {
        stubInlineTransaction();
        when(recipeRepository.findById(RECIPE_ID)).thenReturn(Optional.of(ownerRecipe));
        when(sessionService.getCurrentUserId()).thenReturn(Optional.of(OWNER));
        doThrow(new RuntimeException("favorites cleanup failed"))
                .when(favoriteRepository).deleteAllByRecipeId(RECIPE_ID);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> useCase.execute(RECIPE_ID));
        assertEquals("favorites cleanup failed", ex.getMessage());

        // The recipe row must not have been touched — the transaction-manager rollback
        // (in production) would undo the favourites delete too. We assert the use case's
        // half of that contract: it never reaches the recipeRepository.delete call.
        verify(recipeRepository, never()).delete(any(RecipeId.class));
    }

    @Test
    @DisplayName("execute: ScopedValue still unbinds when a failure occurs mid-transaction")
    void execute_scopedValueUnbindsEvenOnFailure() {
        Connection sharedConnection = mock(Connection.class);

        doAnswer(inv -> {
            Runnable action = inv.getArgument(0);
            try {
                ScopedValue.where(JdbcTransactionManager.CONNECTION, sharedConnection).run(action::run);
            } catch (RuntimeException re) {
                // Real TM would rollback; we just rethrow so the use case sees the failure.
                throw re;
            }
            return null;
        }).when(transactionManager).execute(any(Runnable.class));

        when(recipeRepository.findById(RECIPE_ID)).thenReturn(Optional.of(ownerRecipe));
        when(sessionService.getCurrentUserId()).thenReturn(Optional.of(OWNER));
        doThrow(new RuntimeException("favorites cleanup failed"))
                .when(favoriteRepository).deleteAllByRecipeId(RECIPE_ID);

        assertThrows(RuntimeException.class, () -> useCase.execute(RECIPE_ID));

        // Even though the transaction lambda threw, ScopedValue.where(...).run(...)
        // must release the binding on every exit path — no leak.
        assertFalse(JdbcTransactionManager.CONNECTION.isBound(),
                "ScopedValue must unbind on the failure exit path, not just the happy path");
    }

    // ── Authorisation guards: every short-circuit skips both deletes ────────

    @Test
    @DisplayName("execute: throws RecipeNotFoundException and skips both deletes when recipe is missing")
    void execute_recipeMissing_skipsBothDeletes() {
        stubInlineTransaction();
        when(recipeRepository.findById(RECIPE_ID)).thenReturn(Optional.empty());

        assertThrows(RecipeNotFoundException.class, () -> useCase.execute(RECIPE_ID));

        verify(favoriteRepository, never()).deleteAllByRecipeId(any());
        verify(recipeRepository,   never()).delete(any(RecipeId.class));
    }

    @Test
    @DisplayName("execute: throws AuthenticationRequiredException and skips both deletes when no session")
    void execute_noSession_skipsBothDeletes() {
        stubInlineTransaction();
        when(recipeRepository.findById(RECIPE_ID)).thenReturn(Optional.of(ownerRecipe));
        when(sessionService.getCurrentUserId()).thenReturn(Optional.empty());

        assertThrows(AuthenticationRequiredException.class, () -> useCase.execute(RECIPE_ID));

        verify(favoriteRepository, never()).deleteAllByRecipeId(any());
        verify(recipeRepository,   never()).delete(any(RecipeId.class));
    }

    @Test
    @DisplayName("execute: throws UnauthorizedRecipeAccessException and skips both deletes for a non-owner")
    void execute_nonOwner_skipsBothDeletes() {
        stubInlineTransaction();
        when(recipeRepository.findById(RECIPE_ID)).thenReturn(Optional.of(ownerRecipe));
        when(sessionService.getCurrentUserId()).thenReturn(Optional.of(INTRUDER));

        assertThrows(UnauthorizedRecipeAccessException.class, () -> useCase.execute(RECIPE_ID));

        verify(favoriteRepository, never()).deleteAllByRecipeId(any());
        verify(recipeRepository,   never()).delete(any(RecipeId.class));
    }
}
