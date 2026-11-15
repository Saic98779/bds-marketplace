package com.sidbi.bds.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Component
public class JwtService {

    private final SecurityProperties properties;
    private final SecretKey key;

    public JwtService(SecurityProperties properties) {
        this.properties = properties;
        if (properties.getJwtSecret() == null || properties.getJwtSecret().length() < 32) {
            throw new IllegalStateException("bds.security.jwt-secret must be at least 32 characters");
        }
        this.key = Keys.hmacShaKeyFor(properties.getJwtSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String createAccessToken(AuthUser user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .issuer(properties.getIssuer())
                .subject(String.valueOf(user.userId()))
                .claim("name", user.name())
                .claim("role", user.role().name())
                .claim("status", user.status())
                .claim("ref", user.referenceNo())
                .claim("perms", List.copyOf(user.permissions()))
                .claim("typ", "access")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(properties.getAccessTokenMinutes(), ChronoUnit.MINUTES)))
                .signWith(key)
                .compact();
    }

    public long accessTokenSeconds() {
        return properties.getAccessTokenMinutes() * 60;
    }

    public Optional<AuthUser> parse(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .requireIssuer(properties.getIssuer())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            if (!"access".equals(claims.get("typ", String.class))) {
                return Optional.empty();
            }
            Set<String> perms = new HashSet<>();
            Object raw = claims.get("perms");
            if (raw instanceof Collection<?> collection) {
                collection.forEach(p -> perms.add(String.valueOf(p)));
            }
            return Optional.of(new AuthUser(
                    Long.valueOf(claims.getSubject()),
                    claims.get("name", String.class),
                    Role.valueOf(claims.get("role", String.class)),
                    claims.get("status", String.class),
                    claims.get("ref", String.class),
                    perms
            ));
        } catch (JwtException | IllegalArgumentException ex) {
            return Optional.empty();
        }
    }
}
