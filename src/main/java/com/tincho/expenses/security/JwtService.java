package com.tincho.expenses.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private static final String TYPE_CLAIM = "type";
    private static final String REFRESH_TYPE = "refresh";

    private final SecretKey secretKey;
    private final long expirationMs;
    private final long refreshExpirationMs;

    public JwtService(JwtProperties properties) {
        this.secretKey = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
        this.expirationMs = properties.expirationMs();
        this.refreshExpirationMs = properties.refreshExpirationMs();
    }

    public String generateToken(TenantUserDetails userDetails) {
        return Jwts.builder()
                .subject(userDetails.getUsername())
                .claim("id", userDetails.getId())
                .claim("role", userDetails.getRole())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(secretKey)
                .compact();
    }

    public String generateRefreshToken(TenantUserDetails userDetails) {
        return Jwts.builder()
                .subject(userDetails.getUsername())
                .claim(TYPE_CLAIM, REFRESH_TYPE)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + refreshExpirationMs))
                .signWith(secretKey)
                .compact();
    }

    /**
     * Validates a refresh token (signature, expiry, type) and returns its subject (email).
     * Throws {@link JwtException} if the token is invalid, expired, or not a refresh token.
     */
    public String extractRefreshSubject(String token) {
        Claims claims = extractAllClaims(token);
        if (!REFRESH_TYPE.equals(claims.get(TYPE_CLAIM, String.class))) {
            throw new JwtException("Not a refresh token");
        }
        return claims.getSubject();
    }

    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isTokenValid(String token) {
        try {
            extractAllClaims(token);
            return true;
        } catch (JwtException e) {
            return false;
        }
    }
}
