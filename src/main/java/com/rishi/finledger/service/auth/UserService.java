package com.rishi.finledger.service.auth;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.rishi.finledger.Security.JwtService;
import com.rishi.finledger.dto.AuthResponse;
import com.rishi.finledger.dto.LoginRequest;
import com.rishi.finledger.dto.RegisterRequest;
import com.rishi.finledger.dto.UserResponse;
import com.rishi.finledger.entity.RefreshTokenEntity;
import com.rishi.finledger.entity.UserEntity;
import com.rishi.finledger.exception.EmailAlreadyExistsException;
import com.rishi.finledger.exception.InvalidCredentialsException;
import com.rishi.finledger.exception.UserNotFoundException;
import com.rishi.finledger.mapper.RefreshTokenMapper;
import com.rishi.finledger.mapper.UserMapper;
import com.rishi.finledger.repository.RefreshTokenRepository;
import com.rishi.finledger.repository.UserRepository;
import com.rishi.finledger.service.wallet.WalletService;

import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final WalletService walletService;
    private final UserMapper userMapper;
    private final RefreshTokenMapper refreshTokenMapper;
    private final RefreshTokenRepository refreshTokenRepository;

    public UserService(UserRepository userRepository,
            BCryptPasswordEncoder passwordEncoder,
            JwtService jwtService,
            WalletService walletService,
            UserMapper userMapper,
            RefreshTokenMapper refreshTokenMapper,
            RefreshTokenRepository refreshTokenRepository) {
        this.userRepository = userRepository;

        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.walletService = walletService;
        this.userMapper = userMapper;
        this.refreshTokenMapper = refreshTokenMapper;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    public UserResponse getByEmail(String Email) {
        log.info("Fetching user by email: {}", Email);
        UserEntity user = userRepository.findByEmail(Email)

                .orElseThrow(() -> new UserNotFoundException("USER_NOT_FOUND"));

        return userMapper.toUserResponse(user);
    }

    // Registration
    @Transactional
    public UserResponse registerUser(RegisterRequest request) {
        log.info("User registration attempt | email={}", request.getEmail());

        if (userRepository.existsByEmail(request.getEmail())) {

            log.warn("Registration failed | email already exists | email={}", request.getEmail());

            throw new EmailAlreadyExistsException("Email already exists");
        }

        UserEntity user = userMapper.toEntity(request);
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        UserEntity savedUser = userRepository.save(user);

        walletService.createWallet(savedUser);

        log.info("User registered successfully | userId={} | email={}", savedUser.getId(), savedUser.getEmail());

        return userMapper.toUserResponse(savedUser);
    }

    // Login
    public AuthResponse loginUser(LoginRequest request) {
        log.info("Login attempt | email={}", request.getEmail());

        UserEntity userInfo = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    log.warn("Login failed | email not found | email={}", request.getEmail());
                    return new InvalidCredentialsException("Invalid email or password");
                });

        if (!passwordEncoder.matches(request.getPassword(), userInfo.getPassword())) {
            log.warn("Login failed | invalid password | email={}", request.getEmail());

            throw new InvalidCredentialsException("Invalid email or password");
        }

        String accessToken = jwtService.generateAccessToken(userInfo);
        String refreshToken = jwtService.generateRefreshToken(userInfo);

        RefreshTokenEntity entity = refreshTokenMapper.toEntity(refreshToken, userInfo);

        refreshTokenRepository.save(entity);

        log.info("Login success | userId={} | email={}", userInfo.getId(), userInfo.getEmail());

        return new AuthResponse(accessToken, refreshToken);
    }

    public AuthResponse refreshAccessToken(String refreshToken) {

        if (!jwtService.isRefreshToken(refreshToken)) {
            throw new RuntimeException("Invalid refresh token type");
        }

        RefreshTokenEntity stored = refreshTokenRepository
                .findByToken(refreshToken)
                .orElseThrow(() -> new RuntimeException("Invalid refresh token"));

        if (stored.isRevoked()) {
            throw new RuntimeException("Refresh token revoked");
        }

        if (stored.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Refresh token expired");
        }

        UserEntity user = userRepository.findById(stored.getUserId())
                .orElseThrow();

        String newAccessToken = jwtService.generateAccessToken(user);

        return new AuthResponse(newAccessToken, refreshToken);
    }

    public void logout(String refreshToken) {

        RefreshTokenEntity token = refreshTokenRepository
                .findByToken(refreshToken)
                .orElseThrow(() -> new RuntimeException("Invalid refresh token"));

        if (token.isRevoked()) {
            throw new RuntimeException("Token already revoked");
        }

        if (token.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Token already expired");
        }

        token.setRevoked(true);
        refreshTokenRepository.save(token);
    }
}
