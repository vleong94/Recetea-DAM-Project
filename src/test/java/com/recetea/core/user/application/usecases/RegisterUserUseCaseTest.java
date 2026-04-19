package com.recetea.core.user.application.usecases;

import com.recetea.core.shared.application.ports.out.ITransactionManager;
import com.recetea.core.user.application.ports.in.dto.RegisterUserRequest;
import com.recetea.core.user.application.ports.in.dto.UserResponse;
import com.recetea.core.user.application.ports.out.IPasswordEncoder;
import com.recetea.core.user.application.ports.out.IUserRepository;
import com.recetea.core.user.domain.DuplicateIdentityException;
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
@DisplayName("RegisterUserUseCase — Registro de cuentas e integridad de identidad")
class RegisterUserUseCaseTest {

    @Mock private IUserRepository userRepository;
    @Mock private IPasswordEncoder passwordEncoder;
    @Mock private ITransactionManager transactionManager;

    private RegisterUserUseCase useCase;

    private static final String VALID_HASH = "$2a$12$aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";

    @BeforeEach
    void setUp() {
        useCase = new RegisterUserUseCase(userRepository, passwordEncoder, transactionManager);

        when(transactionManager.execute(any(Supplier.class)))
                .thenAnswer(inv -> inv.getArgument(0, Supplier.class).get());
    }

    @Test
    @DisplayName("execute: camino feliz — codifica la contraseña y persiste el usuario")
    void execute_ShouldEncodePasswordAndSaveUser() {
        when(userRepository.findByUsername("victor")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("victor@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("secret")).thenReturn(VALID_HASH);
        doAnswer(inv -> { ((User) inv.getArgument(0)).setId(new UserId(1)); return null; })
                .when(userRepository).save(any(User.class));

        RegisterUserRequest request = new RegisterUserRequest("victor", "victor@example.com", "secret");
        UserResponse response = useCase.execute(request);

        // Encoder must have been called with the plain password
        verify(passwordEncoder, times(1)).encode("secret");

        // Repository must receive a user whose hash is the encoded value
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertEquals(VALID_HASH, captor.getValue().getPasswordHash());

        // Response must not expose the hash
        assertEquals(1, response.id().value());
        assertEquals("victor", response.username());
        assertEquals("victor@example.com", response.email());
    }

    @Test
    @DisplayName("execute: lanza DuplicateIdentityException si el username ya existe")
    void execute_ShouldThrow_WhenUsernameIsTaken() {
        when(userRepository.findByUsername("victor")).thenReturn(Optional.of(mock(User.class)));

        RegisterUserRequest request = new RegisterUserRequest("victor", "new@example.com", "secret");

        assertThrows(DuplicateIdentityException.class, () -> useCase.execute(request));
        verify(userRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    @DisplayName("execute: lanza DuplicateIdentityException si el email ya existe")
    void execute_ShouldThrow_WhenEmailIsTaken() {
        when(userRepository.findByUsername("newuser")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("taken@example.com")).thenReturn(Optional.of(mock(User.class)));

        RegisterUserRequest request = new RegisterUserRequest("newuser", "taken@example.com", "secret");

        assertThrows(DuplicateIdentityException.class, () -> useCase.execute(request));
        verify(userRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    @DisplayName("execute: la transacción debe enmarcar toda la operación")
    void execute_ShouldRunInsideTransaction() {
        when(userRepository.findByUsername(any())).thenReturn(Optional.empty());
        when(userRepository.findByEmail(any())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(any())).thenReturn(VALID_HASH);
        doAnswer(inv -> { ((User) inv.getArgument(0)).setId(new UserId(2)); return null; })
                .when(userRepository).save(any(User.class));

        useCase.execute(new RegisterUserRequest("ana", "ana@example.com", "pass"));

        verify(transactionManager, times(1)).execute(any(Supplier.class));
    }
}
