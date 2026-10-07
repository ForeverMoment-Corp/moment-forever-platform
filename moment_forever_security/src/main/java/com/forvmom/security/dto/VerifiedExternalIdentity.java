package com.forvmom.security.dto;

import com.forvmom.common.enums.AuthIdentityProvider;

/**
 * Normalized output of a verified external OIDC identity.
 *
 * <p>
 * Provider-specific token validation is kept outside the login orchestration.
 * Once a token is verified, the rest of the system only works with this
 * normalized shape so Google and future Microsoft login follow the same
 * provisioning and linking flow.
 */
public class VerifiedExternalIdentity {

    private final AuthIdentityProvider provider;
    private final String providerSubject;
    private final String email;
    private final boolean emailVerified;
    private final String displayName;
    private final String pictureUrl;

    public VerifiedExternalIdentity(AuthIdentityProvider provider, String providerSubject, String email,
            boolean emailVerified, String displayName, String pictureUrl) {
        this.provider = provider;
        this.providerSubject = providerSubject;
        this.email = email;
        this.emailVerified = emailVerified;
        this.displayName = displayName;
        this.pictureUrl = pictureUrl;
    }

    public AuthIdentityProvider getProvider() {
        return provider;
    }

    public String getProviderSubject() {
        return providerSubject;
    }

    public String getEmail() {
        return email;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getPictureUrl() {
        return pictureUrl;
    }
}
