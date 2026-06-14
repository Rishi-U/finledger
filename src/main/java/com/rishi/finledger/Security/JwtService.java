package com.rishi.finledger.Security;

import java.security.Key;
import java.util.Date;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.rishi.finledger.entity.UserEntity;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    private static final String TYPE_REFRESH = "REFRESH";

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    // ---------------- ACCESS TOKEN ----------------
    public String generateAccessToken(UserEntity user) {

        long now = System.currentTimeMillis();

        return Jwts.builder()
                .setSubject(user.getEmail())
                .claim("userId", user.getId())
                .claim("role", user.getRole().name())
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + expiration))
                .signWith(getSigningKey())
                .compact();
    }

    // ---------------- REFRESH TOKEN ----------------
    public String generateRefreshToken(UserEntity user) {

        long now = System.currentTimeMillis();
        long refreshValidity = 1000L * 60 * 60 * 24 * 7;

        return Jwts.builder()
                .setSubject(user.getEmail())
                .claim("userId", user.getId())
                .claim("type", TYPE_REFRESH)
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + refreshValidity))
                .signWith(getSigningKey())
                .compact();
    }

    // ---------------- SINGLE PARSE ----------------
    public Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public boolean validateToken(String token) {
        try {
            extractAllClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isRefreshToken(String token) {
        return TYPE_REFRESH.equals(
                extractAllClaims(token).get("type", String.class));
    }

    // ---------------- EXPIRED TOKEN ----------------
    public String generateExpiredToken(UserEntity user) {

        long now = System.currentTimeMillis();

        return Jwts.builder()
                .setSubject(user.getEmail())
                .claim("userId", user.getId())
                .claim("role", user.getRole().name())
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now - 1000))
                .signWith(getSigningKey())
                .compact();
    }
}