package com.forvmom.security.service;

import com.forvmom.common.errorhandler.ConflictException;
import com.forvmom.common.errorhandler.CustomAuthException;
import com.forvmom.common.errorhandler.ResourceNotFoundException;
import com.forvmom.data.dao.ApplicationUserDao;
import com.forvmom.data.dao.auth.AuthUserDao;
import com.forvmom.data.dao.auth.RoleDao;
import com.forvmom.data.entities.ApplicationUser;
import com.forvmom.data.entities.auth.AuthUser;
import com.forvmom.data.entities.auth.Role;
import com.forvmom.security.dto.AppUserBeanMapper;
import com.forvmom.security.dto.AuthBeanMapper;
import com.forvmom.security.dto.AuthResponse;
import com.forvmom.security.dto.JwtUserDetails;
import com.forvmom.security.dto.LoginRequest;
import com.forvmom.security.dto.RegisterRequestDto;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    private final AuthUserDao authUserDao;
    private final ApplicationUserDao applicationUserDao;
    private final RoleDao roleDao;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthSessionService authSessionService;

    public AuthService(AuthUserDao authUserDao,
            ApplicationUserDao applicationUserDao, RoleDao roleDao,
            PasswordEncoder passwordEncoder, JwtService jwtService,
            AuthSessionService authSessionService) {
        this.authUserDao = authUserDao;
        this.applicationUserDao = applicationUserDao;
        this.roleDao = roleDao;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authSessionService = authSessionService;
    }

    /**
     * Registers a local email/password account.
     *
     * <p>
     * The method preserves the existing AuthUser/ApplicationUser split and also
     * handles the historical soft-delete gap: if an account with the same email
     * was soft-deleted earlier, the same rows are reactivated instead of failing on
     * the database's unique email/username constraints or creating duplicate
     * accounts.
     */
    @Transactional
    public AuthResponse register(RegisterRequestDto request) {
        String normalizedEmail = normalizeEmail(request.getEmail());
        Role role = roleDao.findById(request.getRoleId());
        if (role == null) {
            throw new ResourceNotFoundException("Role not found: " + request.getRoleId());
        }

        Optional<ApplicationUser> existingByEmail = applicationUserDao.findByEmailIgnoreCaseIncludingDeleted(normalizedEmail);
        if (existingByEmail.isPresent()) {
            ApplicationUser existing = existingByEmail.get();
            if (!existing.isDeleted()) {
                logger.warn("Registration failed: Email already exists - {}", normalizedEmail);
                throw new ConflictException("Email already in use: " + normalizedEmail);
            }
            ApplicationUser restored = reactivateDeletedAccount(existing, request, role, normalizedEmail);
            return buildRegistrationResponseWithoutToken(restored.getAuthUser(), restored);
        }

        Optional<AuthUser> orphanAuthUser = authUserDao.findByUsernameIncludingDeleted(normalizedEmail);
        if (orphanAuthUser.isPresent()) {
            AuthUser authUser = orphanAuthUser.get();
            if (!authUser.isDeleted()) {
                logger.warn("Registration failed: Email already exists - {}", normalizedEmail);
                throw new ConflictException("Email already in use: " + normalizedEmail);
            }
            AuthUser restoredAuthUser = resetAuthUserForLogin(authUser, normalizedEmail,
                    passwordEncoder.encode(request.getPassword()), role);
            authUserDao.update(restoredAuthUser);

            ApplicationUser applicationUser = AppUserBeanMapper.mapDtoToEntity(request);
            applicationUser.setEmail(normalizedEmail);
            applicationUser.setAuthUser(restoredAuthUser);
            ApplicationUser savedApplicationUser = applicationUserDao.save(applicationUser);
            return buildRegistrationResponseWithoutToken(restoredAuthUser, savedApplicationUser);
        }

        AuthUser authUser = AuthBeanMapper.mapDtoToEntity(request);
        authUser.setUsername(normalizedEmail);
        authUser.setPassword(passwordEncoder.encode(request.getPassword()));
        authUser.addRole(role);
        AuthUser savedAuthUser = authUserDao.save(authUser);

        ApplicationUser appUser = AppUserBeanMapper.mapDtoToEntity(request);
        appUser.setEmail(normalizedEmail);
        appUser.setAuthUser(savedAuthUser);
        ApplicationUser savedAppUser = applicationUserDao.save(appUser);
        return buildRegistrationResponseWithoutToken(savedAuthUser, savedAppUser);
    }

    @Transactional
    public AuthResponse login(@Valid LoginRequest request) {
        String normalizedEmail = normalizeEmail(request.getEmail());
        Optional<ApplicationUser> applicationUser = applicationUserDao.findByEmailIgnoreCase(normalizedEmail);
        if (applicationUser.isEmpty()) {
            logger.warn("Login failed: User not found - {}", normalizedEmail);
            throw new CustomAuthException("Please register before logging in. User not found: " + normalizedEmail,
                    HttpStatus.UNAUTHORIZED);
        }

        AuthUser authUser = applicationUser.get().getAuthUser();
        assertAccountEligibleForLogin(authUser, normalizedEmail);
        validateUserCredentials(request, authUser);
        return authSessionService.createAuthenticatedSession(authUser, applicationUser.get(), "Login successful");
    }

    @Transactional
    public void logout(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new CustomAuthException("Refresh token is required for logout", HttpStatus.BAD_REQUEST);
        }
        try {
            jwtService.revokeRefreshToken(refreshToken);
            logger.info("User logged out successfully");
        } catch (Exception e) {
            logger.error("Logout failed: unable to revoke refresh token", e);
            throw new CustomAuthException("Logout failed", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Transactional
    public AuthResponse generateRefreshToken(String oldRefreshToken) {
        return jwtService.generateRefreshTokenWithAccessToken(oldRefreshToken);
    }

    /**
     * Shared eligibility check used by email/password and social login.
     */
    void assertAccountEligibleForLogin(AuthUser authUser, String identifier) {
        if (authUser == null || authUser.isDeleted()) {
            logger.warn("Login failed: User not found - {}", identifier);
            throw new CustomAuthException("Please register before logging in. User not found: " + identifier,
                    HttpStatus.UNAUTHORIZED);
        }
        if (!authUser.isEnabled()) {
            logger.warn("Login failed: Account is disabled for user - {}", identifier);
            throw new CustomAuthException("Your account is disabled. Please contact support.", HttpStatus.FORBIDDEN);
        }
        if (!authUser.isAccountNonLocked()) {
            logger.warn("Login failed: Account is locked for user - {}", identifier);
            throw new CustomAuthException("Your account is locked. Please contact support.", HttpStatus.FORBIDDEN);
        }
        if (!authUser.isAccountNonExpired()) {
            logger.warn("Login failed: Account is expired for user - {}", identifier);
            throw new CustomAuthException("Your account is expired. Please verify your email.", HttpStatus.FORBIDDEN);
        }
        if (!authUser.isCredentialsNonExpired()) {
            logger.warn("Login failed: Credentials are expired for user - {}", identifier);
            throw new CustomAuthException("Your credentials are expired. Please reset your password.",
                    HttpStatus.FORBIDDEN);
        }
    }

    private void validateUserCredentials(@Valid LoginRequest request, AuthUser authUser) {
        if (!passwordEncoder.matches(request.getPassword(), authUser.getPassword())) {
            logger.warn("Login failed: Invalid password for user - {}", request.getEmail());
            throw new CustomAuthException("Invalid email or password", HttpStatus.UNAUTHORIZED);
        }
    }

    private ApplicationUser reactivateDeletedAccount(ApplicationUser existing, RegisterRequestDto request,
            Role role, String normalizedEmail) {
        AuthUser authUser = resolveAuthUserForApplicationUser(existing, normalizedEmail);

        resetAuthUserForLogin(authUser, normalizedEmail, passwordEncoder.encode(request.getPassword()), role);
        authUserDao.update(authUser);

        existing.setDeleted(false);
        existing.setActive(true);
        existing.setEmail(normalizedEmail);
        existing.setFullName(request.getFullName());
        existing.setPhoneNumber(request.getPhoneNumber());
        existing.setPreferredCity(request.getPreferredCity());
        existing.setAuthUser(authUser);
        return applicationUserDao.update(existing);
    }

    private AuthUser resolveAuthUserForApplicationUser(ApplicationUser applicationUser, String fallbackEmail) {
        if (applicationUser.getAuthUser() != null && applicationUser.getAuthUser().getId() != null) {
            Optional<AuthUser> authUser = authUserDao.findByIdIncludingDeleted(applicationUser.getAuthUser().getId());
            if (authUser.isPresent()) {
                return authUser.get();
            }
        }
        return authUserDao.findByUsernameIncludingDeleted(fallbackEmail)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Linked auth account not found for profile email: " + fallbackEmail));
    }

    private AuthUser resetAuthUserForLogin(AuthUser authUser, String normalizedEmail, String encodedPassword,
            Role primaryRole) {
        authUser.setDeleted(false);
        authUser.setUsername(normalizedEmail);
        authUser.setPassword(encodedPassword);
        authUser.setEnabled(true);
        authUser.setAccountNonExpired(true);
        authUser.setAccountNonLocked(true);
        authUser.setCredentialsNonExpired(true);
        authUser.getUserRoles().clear();
        authUser.addRole(primaryRole);
        return authUser;
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }

    private AuthResponse buildRegistrationResponseWithoutToken(AuthUser authUser, ApplicationUser appUser) {
        AuthResponse registrationResponse = new AuthResponse();
        registrationResponse.setEmail(authUser.getUsername());
        registrationResponse.setMessage("Registration successful for " + authUser.getUsername() + " Please verify");
        List<Long> roleIdList = authUser.getUserRoles().stream().map(authUserRole -> authUserRole.getRole().getId())
                .collect(Collectors.toList());
        registrationResponse.setRoleIds(roleIdList);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            Object o = authentication.getPrincipal();
            if (o instanceof JwtUserDetails) {
                JwtUserDetails currentUser = (JwtUserDetails) o;
                registrationResponse.setAssignedBy(currentUser.getUsername());
            } else if (o instanceof String) {
                registrationResponse.setAssignedBy((String) o);
            }
        } else {
            registrationResponse.setAssignedBy("SYSTEM");
        }
        registrationResponse.setUserId(authUser.getId());
        registrationResponse.setEmail(authUser.getUsername());
        return registrationResponse;
    }
}
