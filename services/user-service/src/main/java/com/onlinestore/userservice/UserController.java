package com.onlinestore.userservice;

import com.onlinestore.contracts.Dtos;
import com.onlinestore.contracts.Dtos.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;

@RestController
public class UserController {

    private final UserAccountService accounts;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final AuthProperties auth;

    public UserController(
            UserAccountService accounts,
            PasswordEncoder passwordEncoder,
            JwtTokenService jwtTokenService,
            AuthProperties auth) {
        this.accounts = accounts;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
        this.auth = auth;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
                "service", "user-service",
                "status", "ok",
                "persistence", accounts.isPostgres() ? "postgres" : "memory");
    }

    @PostMapping("/users/register")
    public ResponseEntity<UserProfileDto> register(@RequestBody UserRegistrationRequest request) {
        if (request.email() == null
                || request.email().isBlank()
                || request.password() == null
                || request.password().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email and password are required.");
        }
        try {
            var profile = accounts.register(request.email(), request.password(), request.fullName());
            return ResponseEntity.created(java.net.URI.create("/users/" + profile.userId())).body(profile);
        } catch (IllegalStateException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists.");
        }
    }

    @PostMapping("/users/login")
    public LoginResponse login(@RequestBody UserLoginRequest request) {
        if (request.email() == null
                || request.email().isBlank()
                || request.password() == null
                || request.password().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        var account = accounts.findByEmail(request.email()).orElse(null);
        if (account == null || !passwordEncoder.matches(request.password(), account.passwordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        return issueTokens(account);
    }

    @PostMapping("/users/refresh")
    public LoginResponse refresh(@RequestBody RefreshTokenRequest request) {
        if (request.refreshToken() == null || request.refreshToken().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        var hash = TokenHasher.hash(request.refreshToken().trim());
        var stored = accounts.findRefreshToken(hash).orElse(null);
        if (stored == null
                || stored.revokedAt() != null
                || stored.expiresAt().isBefore(OffsetDateTime.now(ZoneOffset.UTC))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        var account = accounts.findById(stored.userId()).orElse(null);
        if (account == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        accounts.revokeRefreshToken(hash, null);
        return issueTokens(account);
    }

    @PostMapping("/users/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@RequestBody(required = false) RefreshTokenRequest request) {
        if (request != null && request.refreshToken() != null && !request.refreshToken().isBlank()) {
            accounts.revokeRefreshToken(TokenHasher.hash(request.refreshToken().trim()), null);
        }
    }

    @GetMapping("/users/{userId}")
    public UserProfileDto getUser(@PathVariable UUID userId) {
        return accounts.findById(userId)
                .map(UserStore.UserAccount::profile)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private LoginResponse issueTokens(UserStore.UserAccount account) {
        var pair = jwtTokenService.createTokenPair(account.profile());
        var refreshHash = TokenHasher.hash(pair.refreshToken());
        accounts.saveRefreshToken(
                account.profile().userId(),
                refreshHash,
                OffsetDateTime.now(ZoneOffset.UTC).plusDays(auth.refreshTokenDays()));
        return new LoginResponse(
                pair.accessToken(), pair.refreshToken(), pair.accessTokenExpiresAt(), account.profile());
    }
}
