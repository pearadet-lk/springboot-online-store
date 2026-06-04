package com.onlinestore.userservice;

import com.onlinestore.contracts.Dtos.UserProfileDto;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.UUID;

public class JwtTokenService {

    private final AuthProperties auth;
    private final SecretKey key;

    public JwtTokenService(AuthProperties auth) {
        this.auth = auth;
        this.key = Keys.hmacShaKeyFor(auth.jwtSigningKey().getBytes(StandardCharsets.UTF_8));
    }

    public TokenPair createTokenPair(UserProfileDto user) {
        var expiresAt = OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(auth.accessTokenMinutes());
        var accessToken = Jwts.builder()
                .issuer(auth.jwtIssuer())
                .audience().add(auth.jwtAudience()).and()
                .subject(user.userId().toString())
                .claim("email", user.email())
                .claim("name", user.fullName())
                .issuedAt(new Date())
                .expiration(Date.from(expiresAt.toInstant()))
                .signWith(key)
                .compact();
        var refreshToken = UUID.randomUUID() + "." + UUID.randomUUID();
        return new TokenPair(accessToken, refreshToken, expiresAt);
    }

    public record TokenPair(String accessToken, String refreshToken, OffsetDateTime accessTokenExpiresAt) {}
}
