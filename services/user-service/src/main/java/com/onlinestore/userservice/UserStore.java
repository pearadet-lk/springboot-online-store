package com.onlinestore.userservice;

import com.onlinestore.contracts.DemoConstants;
import com.onlinestore.contracts.Dtos.UserProfileDto;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class UserStore {

    public final Map<UUID, UserAccount> usersById = new ConcurrentHashMap<>();
    public final Map<String, UserAccount> usersByEmail = new ConcurrentHashMap<>();
    public final Map<String, RefreshTokenRecord> refreshTokens = new ConcurrentHashMap<>();

    public record UserAccount(UserProfileDto profile, String passwordHash) {}

    public record RefreshTokenRecord(
            UUID userId, String tokenHash, OffsetDateTime expiresAt, OffsetDateTime revokedAt, String replacedBy) {}

    public void seedDemoUser(String passwordHash) {
        var profile = new UserProfileDto(
                DemoConstants.DEMO_USER_ID,
                DemoConstants.DEMO_EMAIL,
                DemoConstants.DEMO_FULL_NAME,
                OffsetDateTime.now(ZoneOffset.UTC));
        var account = new UserAccount(profile, passwordHash);
        usersById.put(profile.userId(), account);
        usersByEmail.put(DemoConstants.DEMO_EMAIL, account);
    }
}
