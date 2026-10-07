package com.forvmom.security.service;

import com.forvmom.data.dao.auth.AuthUserDao;
import com.forvmom.data.entities.ApplicationUser;
import com.forvmom.data.entities.auth.AuthUser;
import com.forvmom.security.dto.AuthResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Central place for issuing the platform's own authenticated session after a
 * user has been verified by any login mechanism.
 *
 * <p>
 * Email/password login, Google sign-in, and future Microsoft sign-in should all
 * terminate here so access-token, refresh-token, last-login, and response-shape
 * behavior stay consistent.
 */
@Service
public class AuthSessionService {

    private final JwtService jwtService;
    private final AuthUserDao authUserDao;
    private final long jwtExpirationMillis;

    public AuthSessionService(JwtService jwtService,
            AuthUserDao authUserDao,
            @Value("${jwt.expiration}") long jwtExpirationMillis) {
        this.jwtService = jwtService;
        this.authUserDao = authUserDao;
        this.jwtExpirationMillis = jwtExpirationMillis;
    }

    @Transactional
    public AuthResponse createAuthenticatedSession(AuthUser authUser, ApplicationUser applicationUser, String message) {
        authUser.updateLastLogin();
        authUserDao.update(authUser);

        String jwtToken = jwtService.generateToken(authUser);
        String refreshToken = jwtService.generateAndSaveRefreshToken(authUser);

        AuthResponse authResponse = new AuthResponse();
        authResponse.setToken(jwtToken);
        authResponse.setRefreshToken(refreshToken);
        authResponse.setExpiresIn(jwtExpirationMillis / 1000);
        authResponse.setUserId(authUser.getId());
        authResponse.setEmail(authUser.getUsername());
        authResponse.setFullName(applicationUser != null ? applicationUser.getFullName() : null);
        authResponse.setRoleIds(authUser.getUserRoles().stream()
                .map(authUserRole -> authUserRole.getRole().getId())
                .collect(Collectors.toList()));
        authResponse.setRoleNames(authUser.getUserRoles().stream()
                .map(authUserRole -> authUserRole.getRole().getName())
                .collect(Collectors.toList()));
        authResponse.setRoles(authUser.getUserRoles().stream()
                .map(authUserRole -> authUserRole.getRole().getName())
                .collect(Collectors.joining(",")));
        authResponse.setMessage(message);
        return authResponse;
    }
}
