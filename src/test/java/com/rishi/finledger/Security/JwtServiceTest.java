package com.rishi.finledger.Security;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.rishi.finledger.entity.Role;
import com.rishi.finledger.entity.UserEntity;

import io.jsonwebtoken.Claims;

public class JwtServiceTest {

    private JwtService jwtService;
    private UserEntity user;

    @BeforeEach
    void setup() {

        jwtService = new JwtService();

        ReflectionTestUtils.setField(jwtService, "secret", "mysecretkeymysecretkeymysecretkey12345");

        ReflectionTestUtils.setField( jwtService, "expiration", 1000L * 60 * 60);

        user = new UserEntity();
        user.setId(1L);
        user.setName("Rishi");
        user.setEmail("rishi@gmail.com");
        user.setRole(Role.USER);
    }

    @Test
    void generateAccessToken_ShouldReturnValidToken() {

        String token = jwtService.generateAccessToken(user);

        assertNotNull(token);
        assertTrue(jwtService.validateToken(token));
    }

    @Test
    void extractAllClaims_ShouldReturnCorrectClaims() {

        String token = jwtService.generateAccessToken(user);

        Claims claims = jwtService.extractAllClaims(token);

        assertEquals("rishi@gmail.com", claims.getSubject());
        assertEquals(1, claims.get("userId", Integer.class));
        assertEquals("USER", claims.get("role", String.class));
    }

    @Test
    void validateToken_ShouldReturnTrue_ForValidToken() {

        String token = jwtService.generateAccessToken(user);

        assertTrue(jwtService.validateToken(token));
    }

    @Test
    void validateToken_ShouldReturnFalse_ForInvalidToken() {

        assertFalse(jwtService.validateToken("invalid-token"));
    }

    @Test
    void generateRefreshToken_ShouldCreateRefreshToken() {

        String refreshToken = jwtService.generateRefreshToken(user);

        assertNotNull(refreshToken);
        assertTrue(jwtService.validateToken(refreshToken));
        assertTrue(jwtService.isRefreshToken(refreshToken));
    }

    @Test
    void isRefreshToken_ShouldReturnFalse_ForAccessToken() {

        String accessToken = jwtService.generateAccessToken(user);

        assertFalse(jwtService.isRefreshToken(accessToken));
    }

    @Test
    void generateExpiredToken_ShouldBeInvalid() {

        String expiredToken = jwtService.generateExpiredToken(user);

        assertFalse(jwtService.validateToken(expiredToken));
    }

    @Test
    void extractAllClaims_ShouldThrowException_ForExpiredToken() {

        String expiredToken = jwtService.generateExpiredToken(user);

        assertThrows(Exception.class, () -> jwtService.extractAllClaims(expiredToken));
    }

    @Test
    void accessToken_ShouldContainUserInformation() {

        String token = jwtService.generateAccessToken(user);

        Claims claims = jwtService.extractAllClaims(token);

        assertEquals("rishi@gmail.com", claims.getSubject());
        assertEquals("USER", claims.get("role"));
    }
}