package com.forvmom.security.service;

import com.forvmom.common.enums.AuthIdentityProvider;
import com.forvmom.security.dto.VerifiedExternalIdentity;

/**
 * Verifies an external provider token and converts it into a normalized
 * identity object that the rest of the auth system can consume.
 */
public interface ExternalIdentityVerifier {

    AuthIdentityProvider getProvider();

    VerifiedExternalIdentity verify(String providerToken);
}
