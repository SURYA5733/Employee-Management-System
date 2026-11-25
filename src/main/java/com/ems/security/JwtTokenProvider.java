package com.ems.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.stream.Collectors;

@Component
public class JwtTokenProvider {

    // 256-bit secret key for HMAC-SHA256
    @Value("${app.jwt.secret:404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970}")
    private String jwtSecret;

    @Value("${app.jwt.expiration-ms:86400000}") // 24 hours
    private long jwtExpirationMs;

    private Key getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(Authentication authentication) {
        CustomUserDetails userPrincipal = (CustomUserDetails) authentication.getPrincipal();
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);

        String roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        return Jwts.builder()
                .setSubject(userPrincipal.getUsername())
                .claim("userId", userPrincipal.getId())
                .claim("email", userPrincipal.getEmail())
                .claim("roles", roles)
                .claim("employeeId", userPrincipal.getEmployeeId())
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String getUsernameFromJwt(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.getSubject();
    }

    public boolean validateToken(String authToken) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(authToken);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            // Invalid or expired JWT
            return false;
        }
    }
}
