package com.alevo.usermodule.security;

import com.alevo.usermodule.entity.Device;
import com.alevo.usermodule.entity.UserAccount;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
@Slf4j
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.access-token-expiry-ms}")
    private long accessTokenExpiryMs;

    @Value("${jwt.refresh-token-expiry-ms}")
    private long refreshTokenExpiryMs;

    private Key getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(java.util.Base64.getEncoder().encodeToString(jwtSecret.getBytes()));
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Generate a short-lived access token (15 minutes).
     * Claims include userId, deviceId, and phone number.
     */
    public String generateAccessToken(UserAccount user, Device device) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("deviceId", device.getId());
        claims.put("phone", user.getPhoneNumber());
        claims.put("type", "access");

        return Jwts.builder().setClaims(claims).setSubject(user.getId()).setIssuedAt(new Date()).setExpiration(new Date(System.currentTimeMillis() + accessTokenExpiryMs)).setId(UUID.randomUUID().toString()).signWith(getSigningKey(), SignatureAlgorithm.HS256).compact();
    }

    /**
     * Generate a long-lived opaque refresh token (30 days).
     * Stored in DB and validated there — not decoded from JWT.
     */
    public String generateRefreshToken() {
        return UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * Generate a device link token (used for QR code / pairing flow).
     */
    public String generateDeviceLinkToken() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }

    /**
     * Parse and validate a JWT access token.
     */
    public Claims parseToken(String token) {
        return Jwts.parserBuilder().setSigningKey(getSigningKey()).build().parseClaimsJws(token).getBody();
    }

    /**
     * Validate token and return true if valid.
     */
    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (SecurityException e) {
            log.warn("Invalid JWT signature: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.warn("Malformed JWT token: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.warn("Expired JWT token: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.warn("Unsupported JWT token: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("JWT claims string is empty: {}", e.getMessage());
        }
        return false;
    }

    public String extractUserId(String token) {
        return parseToken(token).get("userId", String.class);
    }

    public String extractDeviceId(String token) {
        return parseToken(token).get("deviceId", String.class);
    }

    public String extractPhone(String token) {
        return parseToken(token).get("phone", String.class);
    }

    public long getRefreshTokenExpiryMs() {
        return refreshTokenExpiryMs;
    }
}
