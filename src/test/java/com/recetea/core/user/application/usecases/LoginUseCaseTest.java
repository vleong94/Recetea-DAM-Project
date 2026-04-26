package com.recetea.core.user.application.usecases;

import com.recetea.core.user.application.ports.in.dto.LoginRequest;
import com.recetea.core.user.application.ports.in.dto.UserResponse;
import com.recetea.core.user.application.ports.out.IPasswordEncoder;
import com.recetea.core.user.application.ports.out.IUserRepository;
import com.recetea.core.user.domain.User;
import com.recetea.core.user.domain.UserId;
import com.recetea.core.user.domain.vo.Email;
import com.recetea.core.user.domain.vo.PasswordHash;
import com.recetea.core.user.domain.vo.Username;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LoginUseCase — Autenticación y prevención de account harvesting")
class LoginUseCaseTest {

    @Mock private IUserRepository userRepository;
    @Mock private IPasswordEncoder passwordEncoder;

    private LoginUseCase useCase;

    private static final String VALID_HASH = "$2a$12$aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";

    @BeforeEach
    void setUp() {
        useCase = new LoginUseCase(userRepository, passwordEncoder);
    }

    private User buildUser() {
        User user = new User(
                new Username("victor"),
                new Email("victor@example.com"),
                new PasswordHash(VALID_HASH)
        );
        user.setId(new UserId(1));
        return user;
    }

    @Test
    @DisplayName("execute: login exitoso por username")
    void execute_ShouldReturnUserResponse_WhenUsernameAndPasswordMatch() {
        User user = buildUser();
        when(userRepository.findByUsername("victor")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret", VALID_HASH)).thenReturn(true);

        Optional<UserResponse> result = useCase.execute(new LoginRequest("victor", "secret"));

        assertTrue(result.isPresent());
        assertEquals(1, result.get().id().value());
        assertEquals("victor", result.get().username());
        assertEquals("victor@example.com", result.get().email());
        verify(userRepository, never()).findByEmail(any());
    }

    @Test
    @DisplayName("execute: login exitoso por email cuando el username no coincide")
    void execute_ShouldFallbackToEmail_WhenUsernameNotFound() {
        User user = buildUser();
        when(userRepository.findByUsername("victor@example.com")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("victor@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret", VALID_HASH)).thenReturn(true);

        Optional<UserResponse> result = useCase.execute(new LoginRequest("victor@example.com", "secret"));

        assertTrue(result.isPresent());
        assertEquals("victor", result.get().username());
    }

    @Test
    @DisplayName("execute: retorna Optional.empty si la contraseña es incorrecta")
    void execute_ShouldReturnEmpty_WhenPasswordDoesNotMatch() {
        User user = buildUser();
        when(userRepository.findByUsername("victor")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", VALID_HASH)).thenReturn(false);

        Optional<UserResponse> result = useCase.execute(new LoginRequest("victor", "wrong"));

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("execute: retorna Optional.empty si el usuario no existe (sin distinguir de contraseña incorrecta)")
    void execute_ShouldReturnEmpty_WhenUserDoesNotExist() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("ghost")).thenReturn(Optional.empty());

        Optional<UserResponse> result = useCase.execute(new LoginRequest("ghost", "any"));

        assertTrue(result.isEmpty());
        verify(passwordEncoder, never()).matches(any(), any());
    }

    @Test
    @DisplayName("execute: no invoca al encoder si el usuario no existe (evita timing attacks superfluos)")
    void execute_ShouldNotCallEncoder_WhenUserNotFound() {
        when(userRepository.findByUsername(any())).thenReturn(Optional.empty());
        when(userRepository.findByEmail(any())).thenReturn(Optional.empty());

        useCase.execute(new LoginRequest("nobody", "password"));

        verify(passwordEncoder, never()).matches(any(), any());
    }
}
