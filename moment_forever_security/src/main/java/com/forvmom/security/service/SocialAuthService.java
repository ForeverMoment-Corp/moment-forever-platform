package com.forvmom.security.service;

import com.forvmom.common.enums.AuthIdentityProvider;
import com.forvmom.common.errorhandler.ConflictException;
import com.forvmom.common.errorhandler.CustomAuthException;
import com.forvmom.common.errorhandler.ResourceNotFoundException;
import com.forvmom.data.dao.ApplicationUserDao;
import com.forvmom.data.dao.auth.AuthIdentityDao;
import com.forvmom.data.dao.auth.AuthUserDao;
import com.forvmom.data.dao.auth.RoleDao;
import com.forvmom.data.entities.ApplicationUser;
import com.forvmom.data.entities.auth.AuthIdentity;
import com.forvmom.data.entities.auth.AuthUser;
import com.forvmom.data.entities.auth.Role;
import com.forvmom.security.dto.AuthResponse;
import com.forvmom.security.dto.VerifiedExternalIdentity;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Handles federated/social sign-in while keeping the platform's local account
 * model unchanged.
 *
 * <p>
 * External providers only establish identity. Once the provider token has been
 * verified, the flow still resolves a local {@link AuthUser} /
 * {@link ApplicationUser} pair and issues the platform's own JWT and refresh
 * token so all downstream auth, roles, revocation, and gateway integration keep
 * working exactly as they do for email/password login.
 */
@Service
public class SocialAuthService {

    private final Map<AuthIdentityProvider, ExternalIdentityVerifier> verifierByProvider;
    private final AuthIdentityDao authIdentityDao;
    private final AuthUserDao authUserDao;
    private final ApplicationUserDao applicationUserDao;
    private final RoleDao roleDao;
    private final PasswordEncoder passwordEncoder;
    private final AuthSessionService authSessionService;
    private final AuthService authService;

    public SocialAuthService(List<ExternalIdentityVerifier> verifiers,
            AuthIdentityDao authIdentityDao,
            AuthUserDao authUserDao,
            ApplicationUserDao applicationUserDao,
            RoleDao roleDao,
            PasswordEncoder passwordEncoder,
            AuthSessionService authSessionService,
            AuthService authService) {
        this.authIdentityDao = authIdentityDao;
        this.authUserDao = authUserDao;
        this.applicationUserDao = applicationUserDao;
        this.roleDao = roleDao;
        this.passwordEncoder = passwordEncoder;
        this.authSessionService = authSessionService;
        this.authService = authService;
        this.verifierByProvider = new EnumMap<>(AuthIdentityProvider.class);
        for (ExternalIdentityVerifier verifier : verifiers) {
            this.verifierByProvider.put(verifier.getProvider(), verifier);
        }
    }

    @Transactional
    public AuthResponse signInWithGoogle(String idToken) {
        return signIn(AuthIdentityProvider.GOOGLE, idToken);
    }

    private AuthResponse signIn(AuthIdentityProvider provider, String providerToken) {
        ExternalIdentityVerifier verifier = verifierByProvider.get(provider);
        if (verifier == null) {
            throw new CustomAuthException(provider + " sign-in is not configured on the server",
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }

        VerifiedExternalIdentity verifiedIdentity = verifier.verify(providerToken);
        if (!verifiedIdentity.isEmailVerified()) {
            throw new CustomAuthException(provider + " account email is not verified", HttpStatus.FORBIDDEN);
        }

        ResolvedAccount resolvedAccount = resolveAccount(verifiedIdentity);
        authService.assertAccountEligibleForLogin(resolvedAccount.authUser, verifiedIdentity.getEmail());
        updateIdentityMetadata(resolvedAccount.identity, verifiedIdentity);

        return authSessionService.createAuthenticatedSession(
                resolvedAccount.authUser,
                resolvedAccount.applicationUser,
                provider + " sign-in successful");
    }

    /**
     * Resolve a verified provider identity into the existing local account model.
     *
     * <p>
     * Resolution order:
     * 1. Existing provider link
     * 2. Existing soft-deleted provider link (reactivate)
     * 3. Verified-email auto-link to existing local account
     * 4. Verified-email reactivation of a deleted local account
     * 5. Fresh local account creation with default USER role
     */
    private ResolvedAccount resolveAccount(VerifiedExternalIdentity verifiedIdentity) {
        Optional<AuthIdentity> linkedIdentity = authIdentityDao.findByProviderAndProviderSubject(
                verifiedIdentity.getProvider(), verifiedIdentity.getProviderSubject());
        if (linkedIdentity.isPresent()) {
            return rehydrateLinkedAccount(linkedIdentity.get(), verifiedIdentity);
        }

        Optional<AuthIdentity> deletedLinkedIdentity = authIdentityDao.findByProviderAndProviderSubjectIncludingDeleted(
                verifiedIdentity.getProvider(), verifiedIdentity.getProviderSubject());
        if (deletedLinkedIdentity.isPresent()) {
            AuthIdentity identity = deletedLinkedIdentity.get();
            identity.setDeleted(false);
            return rehydrateLinkedAccount(identity, verifiedIdentity);
        }

        Optional<ApplicationUser> anyApplicationUser = applicationUserDao.findByEmailIgnoreCaseIncludingDeleted(
                verifiedIdentity.getEmail());
        if (anyApplicationUser.isPresent()) {
            ApplicationUser applicationUser = anyApplicationUser.get();
            AuthUser authUser = resolveAuthUserForApplicationUser(applicationUser, verifiedIdentity.getEmail());

            reactivateSoftDeletedAuthUserIfNeeded(authUser);
            reactivateApplicationUserIfNeeded(applicationUser, verifiedIdentity);
            ensureDefaultUserRoleIfMissing(authUser);

            Optional<AuthIdentity> existingProviderLink = authIdentityDao.findByAuthUserIdAndProvider(
                    authUser.getId(), verifiedIdentity.getProvider());
            if (existingProviderLink.isPresent()
                    && !existingProviderLink.get().getProviderSubject().equals(verifiedIdentity.getProviderSubject())) {
                throw new ConflictException(
                        "This account is already linked to a different " + verifiedIdentity.getProvider() + " identity");
            }

            AuthIdentity identity = existingProviderLink.orElseGet(() -> createNewIdentity(authUser, verifiedIdentity));
            authUserDao.update(authUser);
            applicationUserDao.update(applicationUser);
            return new ResolvedAccount(authUser, applicationUser, identity);
        }

        Optional<AuthUser> orphanAuthUser = authUserDao.findByUsernameIncludingDeleted(verifiedIdentity.getEmail());
        if (orphanAuthUser.isPresent()) {
            AuthUser authUser = orphanAuthUser.get();
            reactivateSoftDeletedAuthUserIfNeeded(authUser);
            ensureDefaultUserRoleIfMissing(authUser);
            authUserDao.update(authUser);

            ApplicationUser applicationUser = createApplicationUser(authUser, verifiedIdentity);
            AuthIdentity identity = createNewIdentity(authUser, verifiedIdentity);
            return new ResolvedAccount(authUser, applicationUser, identity);
        }

        AuthUser authUser = createAuthUserForSocialIdentity(verifiedIdentity.getEmail());
        ApplicationUser applicationUser = createApplicationUser(authUser, verifiedIdentity);
        AuthIdentity identity = createNewIdentity(authUser, verifiedIdentity);
        return new ResolvedAccount(authUser, applicationUser, identity);
    }

    private ResolvedAccount rehydrateLinkedAccount(AuthIdentity identity, VerifiedExternalIdentity verifiedIdentity) {
        AuthUser authUser = authUserDao.findByIdIncludingDeleted(identity.getAuthUser().getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Linked auth account not found for provider identity: " + verifiedIdentity.getProvider()));
        ApplicationUser applicationUser = applicationUserDao.findByAuthUserIdIncludingDeleted(authUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Linked application profile not found for auth user id: " + authUser.getId()));

        reactivateSoftDeletedAuthUserIfNeeded(authUser);
        reactivateApplicationUserIfNeeded(applicationUser, verifiedIdentity);
        ensureDefaultUserRoleIfMissing(authUser);
        authUserDao.update(authUser);
        applicationUserDao.update(applicationUser);

        return new ResolvedAccount(authUser, applicationUser, identity);
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
                        "Linked auth account not found for email: " + fallbackEmail));
    }

    private void reactivateSoftDeletedAuthUserIfNeeded(AuthUser authUser) {
        if (!authUser.isDeleted()) {
            return;
        }
        authUser.setDeleted(false);
        authUser.setEnabled(true);
        authUser.setAccountNonLocked(true);
        authUser.setAccountNonExpired(true);
        authUser.setCredentialsNonExpired(true);
    }

    private void reactivateApplicationUserIfNeeded(ApplicationUser applicationUser, VerifiedExternalIdentity verifiedIdentity) {
        if (applicationUser.isDeleted()) {
            applicationUser.setDeleted(false);
            applicationUser.setActive(true);
        }
        if (applicationUser.getFullName() == null || applicationUser.getFullName().isBlank()) {
            applicationUser.setFullName(resolveDisplayName(verifiedIdentity));
        }
        applicationUser.setEmail(verifiedIdentity.getEmail());
        if (applicationUser.getProfilePictureUrl() == null || applicationUser.getProfilePictureUrl().isBlank()) {
            applicationUser.setProfilePictureUrl(verifiedIdentity.getPictureUrl());
        }
    }

    private void ensureDefaultUserRoleIfMissing(AuthUser authUser) {
        if (authUser.getUserRoles() != null && !authUser.getUserRoles().isEmpty()) {
            return;
        }
        Role role = roleDao.findByNameIgnoreCase("USER")
                .orElseThrow(() -> new ResourceNotFoundException("Role 'USER' not initialized in system"));
        authUser.addRole(role);
    }

    private AuthUser createAuthUserForSocialIdentity(String email) {
        Role role = roleDao.findByNameIgnoreCase("USER")
                .orElseThrow(() -> new ResourceNotFoundException("Role 'USER' not initialized in system"));

        AuthUser authUser = new AuthUser();
        authUser.setUsername(email);
        authUser.setPassword(passwordEncoder.encode(generateRandomPasswordPlaceholder()));
        authUser.setEnabled(true);
        authUser.setAccountNonExpired(true);
        authUser.setAccountNonLocked(true);
        authUser.setCredentialsNonExpired(true);
        authUser.addRole(role);
        return authUserDao.save(authUser);
    }

    private ApplicationUser createApplicationUser(AuthUser authUser, VerifiedExternalIdentity verifiedIdentity) {
        ApplicationUser applicationUser = new ApplicationUser();
        applicationUser.setAuthUser(authUser);
        applicationUser.setEmail(verifiedIdentity.getEmail());
        applicationUser.setFullName(resolveDisplayName(verifiedIdentity));
        applicationUser.setProfilePictureUrl(verifiedIdentity.getPictureUrl());
        applicationUser.setActive(true);
        return applicationUserDao.save(applicationUser);
    }

    private AuthIdentity createNewIdentity(AuthUser authUser, VerifiedExternalIdentity verifiedIdentity) {
        AuthIdentity identity = new AuthIdentity();
        identity.setAuthUser(authUser);
        identity.setProvider(verifiedIdentity.getProvider());
        identity.setProviderSubject(verifiedIdentity.getProviderSubject());
        identity.setProviderEmail(verifiedIdentity.getEmail());
        identity.setEmailVerified(verifiedIdentity.isEmailVerified());
        identity.setProviderDisplayName(verifiedIdentity.getDisplayName());
        identity.setProviderPictureUrl(verifiedIdentity.getPictureUrl());
        identity.setLastLoginAt(LocalDateTime.now());
        return authIdentityDao.save(identity);
    }

    private void updateIdentityMetadata(AuthIdentity identity, VerifiedExternalIdentity verifiedIdentity) {
        identity.setDeleted(false);
        identity.setProviderEmail(verifiedIdentity.getEmail());
        identity.setEmailVerified(verifiedIdentity.isEmailVerified());
        identity.setProviderDisplayName(verifiedIdentity.getDisplayName());
        identity.setProviderPictureUrl(verifiedIdentity.getPictureUrl());
        identity.setLastLoginAt(LocalDateTime.now());
        authIdentityDao.update(identity);
    }

    private String resolveDisplayName(VerifiedExternalIdentity verifiedIdentity) {
        if (verifiedIdentity.getDisplayName() != null && !verifiedIdentity.getDisplayName().isBlank()) {
            return verifiedIdentity.getDisplayName();
        }
        return verifiedIdentity.getEmail();
    }

    private String generateRandomPasswordPlaceholder() {
        return "Social-" + UUID.randomUUID() + "-Aa1!";
    }

    private static final class ResolvedAccount {
        private final AuthUser authUser;
        private final ApplicationUser applicationUser;
        private final AuthIdentity identity;

        private ResolvedAccount(AuthUser authUser, ApplicationUser applicationUser, AuthIdentity identity) {
            this.authUser = authUser;
            this.applicationUser = applicationUser;
            this.identity = identity;
        }
    }
}
