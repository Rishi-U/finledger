package com.rishi.finledger.service.auth;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.rishi.finledger.Security.JwtService;
import com.rishi.finledger.dto.AuthResponse;
import com.rishi.finledger.dto.LoginRequest;
import com.rishi.finledger.dto.RegisterRequest;
import com.rishi.finledger.dto.UserResponse;
import com.rishi.finledger.entity.RefreshTokenEntity;
import com.rishi.finledger.entity.UserEntity;
import com.rishi.finledger.exception.EmailAlreadyExistsException;
import com.rishi.finledger.exception.InvalidCredentialsException;
import com.rishi.finledger.mapper.RefreshTokenMapper;
import com.rishi.finledger.mapper.UserMapper;
import com.rishi.finledger.repository.RefreshTokenRepository;
import com.rishi.finledger.repository.UserRepository;
import com.rishi.finledger.service.wallet.WalletService;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private WalletService walletService;

    @Mock
    private UserMapper userMapper;

    @Mock
    private RefreshTokenMapper refreshTokenMapper;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private UserService userService;

    private UserEntity user;
    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {

        user = new UserEntity();
        user.setId(1L);
        user.setName("Rishi");
        user.setEmail("rishi@test.com");
        user.setPassword("encodedPassword");

        registerRequest = new RegisterRequest();
        registerRequest.setName("Rishi");
        registerRequest.setEmail("rishi@test.com");
        registerRequest.setPassword("password");

        loginRequest = new LoginRequest();
        loginRequest.setEmail("rishi@test.com");
        loginRequest.setPassword("password");
    }

    @Test
    void registerUser_ShouldRegisterSuccessfully() {

        UserResponse response =
                new UserResponse(1L, "Rishi", "rishi@test.com");

        when(userRepository.existsByEmail(registerRequest.getEmail()))
                .thenReturn(false);

        when(userMapper.toEntity(registerRequest))
                .thenReturn(user);

        when(passwordEncoder.encode(registerRequest.getPassword()))
                .thenReturn("encodedPassword");

        when(userRepository.save(user))
                .thenReturn(user);

        when(userMapper.toUserResponse(user))
                .thenReturn(response);

        UserResponse result = userService.registerUser(registerRequest);

        assertNotNull(result);
        assertEquals("Rishi", result.getName());

        verify(walletService).createWallet(user);
    }

    @Test
    void registerUser_ShouldThrow_WhenEmailExists() {

        when(userRepository.existsByEmail(registerRequest.getEmail()))
                .thenReturn(true);

        assertThrows(
                EmailAlreadyExistsException.class,
                () -> userService.registerUser(registerRequest)
        );

        verify(userRepository, never()).save(any());
    }

    @Test
    void loginUser_ShouldLoginSuccessfully() {

        when(userRepository.findByEmail(loginRequest.getEmail()))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(
                loginRequest.getPassword(),
                user.getPassword()))
                .thenReturn(true);

        when(jwtService.generateAccessToken(user))
                .thenReturn("access-token");

        when(jwtService.generateRefreshToken(user))
                .thenReturn("refresh-token");

        RefreshTokenEntity refreshEntity = new RefreshTokenEntity();

        when(refreshTokenMapper.toEntity("refresh-token", user))
                .thenReturn(refreshEntity);

        AuthResponse response = userService.loginUser(loginRequest);

        assertNotNull(response);
        assertEquals("access-token", response.getAccessToken());
        assertEquals("refresh-token", response.getRefreshToken());

        verify(refreshTokenRepository).save(refreshEntity);
    }

    @Test
    void loginUser_ShouldThrow_WhenEmailNotFound() {

        when(userRepository.findByEmail(loginRequest.getEmail()))
                .thenReturn(Optional.empty());

        assertThrows(
                InvalidCredentialsException.class,
                () -> userService.loginUser(loginRequest)
        );
    }

    @Test
    void loginUser_ShouldThrow_WhenPasswordInvalid() {

        when(userRepository.findByEmail(loginRequest.getEmail()))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(any(), any()))
                .thenReturn(false);

        assertThrows(
                InvalidCredentialsException.class,
                () -> userService.loginUser(loginRequest)
        );
    }

    @Test
    void refreshAccessToken_ShouldReturnNewAccessToken() {

        RefreshTokenEntity token = new RefreshTokenEntity();

        token.setToken("refresh-token");
        token.setRevoked(false);
        token.setExpiryDate(LocalDateTime.now().plusDays(1));
        token.setUserId(1L);

        when(jwtService.isRefreshToken("refresh-token"))
                .thenReturn(true);

        when(refreshTokenRepository.findByToken("refresh-token"))
                .thenReturn(Optional.of(token));

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        when(jwtService.generateAccessToken(user))
                .thenReturn("new-access-token");

        AuthResponse response =
                userService.refreshAccessToken("refresh-token");

        assertEquals(
                "new-access-token",
                response.getAccessToken()
        );
    }

    @Test
    void refreshAccessToken_ShouldThrow_WhenTokenRevoked() {

        RefreshTokenEntity token = new RefreshTokenEntity();

        token.setRevoked(true);

        when(jwtService.isRefreshToken("refresh-token"))
                .thenReturn(true);

        when(refreshTokenRepository.findByToken("refresh-token"))
                .thenReturn(Optional.of(token));

        assertThrows(
                RuntimeException.class,
                () -> userService.refreshAccessToken("refresh-token")
        );
    }

    @Test
    void logout_ShouldRevokeToken() {

        RefreshTokenEntity token = new RefreshTokenEntity();

        token.setRevoked(false);
        token.setExpiryDate(LocalDateTime.now().plusDays(1));

        when(refreshTokenRepository.findByToken("refresh-token"))
                .thenReturn(Optional.of(token));

        userService.logout("refresh-token");

        assertTrue(token.isRevoked());

        verify(refreshTokenRepository).save(token);
    }

    @Test
    void logout_ShouldThrow_WhenAlreadyRevoked() {

        RefreshTokenEntity token = new RefreshTokenEntity();

        token.setRevoked(true);
        token.setExpiryDate(LocalDateTime.now().plusDays(1));

        when(refreshTokenRepository.findByToken("refresh-token"))
                .thenReturn(Optional.of(token));

        assertThrows(
                RuntimeException.class,
                () -> userService.logout("refresh-token")
        );
    }
}