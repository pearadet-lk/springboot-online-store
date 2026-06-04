package com.onlinestore.userservice;

import com.onlinestore.contracts.DemoConstants;
import com.onlinestore.contracts.Dtos.UserProfileDto;
import com.onlinestore.userservice.persistence.RefreshTokenEntity;
import com.onlinestore.userservice.persistence.RefreshTokenJpaRepository;
import com.onlinestore.userservice.persistence.UserEntity;
import com.onlinestore.userservice.persistence.UserJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserAccountService {

    private static final Logger log = LoggerFactory.getLogger(UserAccountService.class);

    private final UserPersistenceMode mode;
    private final UserStore memoryStore;
    private final UserJpaRepository userRepo; // null when in-memory mode
    private final RefreshTokenJpaRepository refreshRepo;
    private final PasswordEncoder passwordEncoder;

    public UserAccountService(
            UserPersistenceMode mode,
            UserStore memoryStore,
            UserJpaRepository userRepo,
            RefreshTokenJpaRepository refreshRepo,
            PasswordEncoder passwordEncoder) {
        this.mode = mode;
        this.memoryStore = memoryStore;
        this.userRepo = userRepo;
        this.refreshRepo = refreshRepo;
        this.passwordEncoder = passwordEncoder;
        if (mode == UserPersistenceMode.MEMORY) {
            memoryStore.seedDemoUser(passwordEncoder.encode(DemoConstants.DEMO_PASSWORD));
        }
    }

    public boolean isPostgres() {
        return mode == UserPersistenceMode.POSTGRES;
    }

    public Optional<UserStore.UserAccount> findByEmail(String email) {
        var normalized = email.trim().toLowerCase();
        if (mode == UserPersistenceMode.POSTGRES && userRepo != null) {
            return userRepo.findByEmailIgnoreCase(normalized).map(this::toAccount);
        }
        return Optional.ofNullable(memoryStore.usersByEmail.get(normalized));
    }

    public Optional<UserStore.UserAccount> findById(UUID userId) {
        if (mode == UserPersistenceMode.POSTGRES && userRepo != null) {
            return userRepo.findById(userId).map(this::toAccount);
        }
        return Optional.ofNullable(memoryStore.usersById.get(userId));
    }

    public UserProfileDto register(String email, String password, String fullName) {
        var normalized = email.trim().toLowerCase();
        if (findByEmail(normalized).isPresent()) {
            throw new IllegalStateException("exists");
        }
        var profile = new UserProfileDto(
                UUID.randomUUID(), normalized, fullName.trim(), OffsetDateTime.now(ZoneOffset.UTC));
        var hash = passwordEncoder.encode(password);
        if (mode == UserPersistenceMode.POSTGRES && userRepo != null) {
            var entity = new UserEntity();
            entity.setUserId(profile.userId());
            entity.setEmail(normalized);
            entity.setPasswordHash(hash);
            entity.setFullName(profile.fullName());
            entity.setCreatedAt(profile.createdAt());
            entity.setUpdatedAt(profile.createdAt());
            userRepo.save(entity);
        } else {
            var account = new UserStore.UserAccount(profile, hash);
            memoryStore.usersById.put(profile.userId(), account);
            memoryStore.usersByEmail.put(normalized, account);
        }
        return profile;
    }

    public void saveRefreshToken(UUID userId, String refreshHash, OffsetDateTime expiresAt) {
        if (mode == UserPersistenceMode.POSTGRES && refreshRepo != null) {
            var entity = new RefreshTokenEntity();
            entity.setTokenId(UUID.randomUUID());
            entity.setUserId(userId);
            entity.setTokenHash(refreshHash);
            entity.setExpiresAt(expiresAt);
            entity.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));
            refreshRepo.save(entity);
            return;
        }
        memoryStore.refreshTokens.put(
                refreshHash, new UserStore.RefreshTokenRecord(userId, refreshHash, expiresAt, null, null));
    }

    public Optional<UserStore.RefreshTokenRecord> findRefreshToken(String hash) {
        if (mode == UserPersistenceMode.POSTGRES && refreshRepo != null) {
            return refreshRepo.findByTokenHash(hash)
                    .map(e -> new UserStore.RefreshTokenRecord(
                            e.getUserId(), e.getTokenHash(), e.getExpiresAt(), e.getRevokedAt(), e.getReplacedByTokenHash()));
        }
        return Optional.ofNullable(memoryStore.refreshTokens.get(hash));
    }

    public void revokeRefreshToken(String hash, String replacedBy) {
        if (mode == UserPersistenceMode.POSTGRES && refreshRepo != null) {
            refreshRepo.findByTokenHash(hash).ifPresent(entity -> {
                entity.setRevokedAt(OffsetDateTime.now(ZoneOffset.UTC));
                if (replacedBy != null) {
                    entity.setReplacedByTokenHash(replacedBy);
                }
                refreshRepo.save(entity);
            });
            return;
        }
        var existing = memoryStore.refreshTokens.get(hash);
        if (existing != null) {
            memoryStore.refreshTokens.put(
                    hash,
                    new UserStore.RefreshTokenRecord(
                            existing.userId(),
                            hash,
                            existing.expiresAt(),
                            OffsetDateTime.now(ZoneOffset.UTC),
                            replacedBy));
        }
    }

    private UserStore.UserAccount toAccount(UserEntity entity) {
        var profile = new UserProfileDto(
                entity.getUserId(),
                entity.getEmail(),
                entity.getFullName() != null ? entity.getFullName() : "",
                entity.getCreatedAt());
        return new UserStore.UserAccount(profile, entity.getPasswordHash());
    }
}
