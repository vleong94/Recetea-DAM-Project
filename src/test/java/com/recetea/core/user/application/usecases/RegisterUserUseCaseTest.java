package com.recetea.core.user.application.usecases;

import com.recetea.core.shared.application.ports.out.ITransactionManager;
import com.recetea.core.user.application.ports.in.dto.RegisterUserRequest;
import com.recetea.core.user.application.ports.in.dto.UserResponse;
import com.recetea.core.user.application.ports.out.IPasswordEncoder;
import com.recetea.core.user.application.ports.out.IUserRepository;
import com.recetea.core.user.domain.DuplicateIdentityException;
import com.recetea.core.user.domain.InvalidUserDataException;
import com.recetea.core.user.domain.User;
import com.recetea.core.user.domain.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RegisterUserUseCase — Account registration and identity integrity")
class RegisterUserUseCaseTest {

    @Mock private IUserRepository userRepository;
    @Mock private IPasswordEncoder passwordEncoder;
    @Mock private ITransactionManager transactionManager;

    private RegisterUserUseCase useCase;

    private static final String VALID_HASH = "$2a$12$aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";

    @BeforeEach
    void setUp() {
        useCase = new RegisterUserUseCase(userRepository, passwordEncoder, transactionManager);
    }

    // ── Happy path ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("execute: happy path — encodes the password and persists the user")
    void execute_ShouldEncodePasswordAndSaveUser() {
        when(transactionManager.execute(any(Supplier.class)))
                .thenAnswer(inv -> inv.getArgument(0, Supplier.class).get());
        when(userRepository.findByUsername("victor")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("victor@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("secret12")).thenReturn(VALID_HASH);
        when(userRepository.save(any(User.class))).thenReturn(new UserId(1));

        RegisterUserRequest request = new RegisterUserRequest("victor", "victor@example.com", "secret12");
        UserResponse response = useCase.execute(request);

        verify(passwordEncoder, times(1)).encode("secret12");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertEquals(VALID_HASH, captor.getValue().passwordHash().value());

        assertEquals(1, response.id().value());
        assertEquals("victor", response.username());
        assertEquals("victor@example.com", response.email());
    }

    @Test
    @DisplayName("execute: the transaction must wrap the entire operation")
    void execute_ShouldRunInsideTransaction() {
        when(transactionManager.execute(any(Supplier.class)))
                .thenAnswer(inv -> inv.getArgument(0, Supplier.class).get());
        when(userRepository.findByUsername(any())).thenReturn(Optional.empty());
        when(userRepository.findByEmail(any())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(any())).thenReturn(VALID_HASH);
        when(userRepository.save(any(User.class))).thenReturn(new UserId(2));

        useCase.execute(new RegisterUserRequest("ana", "ana@example.com", "password"));

        verify(transactionManager, times(1)).execute(any(Supplier.class));
    }

    // ── Duplicate identity ────────────────────────────────────────────────────

    @Test
    @DisplayName("execute: throws DuplicateIdentityException when the username is already taken")
    void execute_ShouldThrow_WhenUsernameIsTaken() {
        when(transactionManager.execute(any(Supplier.class)))
                .thenAnswer(inv -> inv.getArgument(0, Supplier.class).get());
        when(userRepository.findByUsername("victor")).thenReturn(Optional.of(mock(User.class)));

        RegisterUserRequest request = new RegisterUserRequest("victor", "new@example.com", "password");

        assertThrows(DuplicateIdentityException.class, () -> useCase.execute(request));
        verify(userRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    @DisplayName("execute: throws DuplicateIdentityException when the email is already taken")
    void execute_ShouldThrow_WhenEmailIsTaken() {
        when(transactionManager.execute(any(Supplier.class)))
                .thenAnswer(inv -> inv.getArgument(0, Supplier.class).get());
        when(userRepository.findByUsername("newuser")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("taken@example.com")).thenReturn(Optional.of(mock(User.class)));

        RegisterUserRequest request = new RegisterUserRequest("newuser", "taken@example.com", "password");

        assertThrows(DuplicateIdentityException.class, () -> useCase.execute(request));
        verify(userRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(any());
    }

    // ── Input validation ──────────────────────────────────────────────────────

    @Test
    @DisplayName("execute: throws InvalidUserDataException when the username is too short")
    void execute_ShouldThrow_WhenUsernameIsTooShort() {
        RegisterUserRequest request = new RegisterUserRequest("ab", "valid@example.com", "password");

        InvalidUserDataException ex = assertThrows(InvalidUserDataException.class,
                () -> useCase.execute(request));

        assertEquals(1, ex.getErrors().size());
        assertTrue(ex.getErrors().get(0).toLowerCase().contains("username"));
        verifyNoInteractions(transactionManager, userRepository, passwordEncoder);
    }

    @Test
    @DisplayName("execute: throws InvalidUserDataException when the email format is invalid")
    void execute_ShouldThrow_WhenEmailFormatIsInvalid() {
        RegisterUserRequest request = new RegisterUserRequest("victor", "not-an-email", "password");

        InvalidUserDataException ex = assertThrows(InvalidUserDataException.class,
                () -> useCase.execute(request));

        assertEquals(1, ex.getErrors().size());
        assertTrue(ex.getErrors().get(0).toLowerCase().contains("email"));
        verifyNoInteractions(transactionManager, userRepository, passwordEncoder);
    }

    @Test
    @DisplayName("execute: accumulates all errors when multiple fields are invalid")
    void execute_ShouldAccumulateErrors_WhenMultipleFieldsInvalid() {
        RegisterUserRequest request = new RegisterUserRequest("ab", "not-an-email", "password");

        InvalidUserDataException ex = assertThrows(InvalidUserDataException.class,
                () -> useCase.execute(request));

        assertEquals(2, ex.getErrors().size());
        verifyNoInteractions(transactionManager, userRepository, passwordEncoder);
    }

    @Test
    @DisplayName("execute: throws InvalidUserDataException when the password is blank")
    void execute_ShouldThrow_WhenPasswordIsBlank() {
        RegisterUserRequest request = new RegisterUserRequest("victor", "victor@example.com", "");

        InvalidUserDataException ex = assertThrows(InvalidUserDataException.class,
                () -> useCase.execute(request));

        // Blank → required AND minLength both fire (blank fails both predicates).
        assertTrue(ex.getErrors().stream().allMatch(k -> k.toLowerCase().contains("password")),
                "Every error must mention the password field");
        assertTrue(ex.getErrors().contains("register.error.password.required"),
                "Required-field key must be emitted on blank input");
        verifyNoInteractions(transactionManager, userRepository, passwordEncoder);
    }

    @Test
    @DisplayName("execute: throws InvalidUserDataException when the password is shorter than the minimum length")
    void execute_ShouldThrow_WhenPasswordIsTooShort() {
        RegisterUserRequest request = new RegisterUserRequest("victor", "victor@example.com", "1234567");

        InvalidUserDataException ex = assertThrows(InvalidUserDataException.class,
                () -> useCase.execute(request));

        assertEquals(1, ex.getErrors().size());
        assertEquals("register.error.password.minLength", ex.getErrors().get(0));
        verifyNoInteractions(transactionManager, userRepository, passwordEncoder);
    }
}
