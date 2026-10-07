package com.forvmom.security.service;

import com.forvmom.common.enums.AuthIdentityProvider;
import com.forvmom.common.errorhandler.CustomAuthException;
import com.forvmom.security.dto.VerifiedExternalIdentity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Verifies Google OIDC ID tokens using Google's published JWK set.
 *
 * <p>
 * The implementation is intentionally provider-specific only at the verification
 * boundary. After verification, Google identities are converted into a shared
 * {@link VerifiedExternalIdentity} shape so the provisioning/linking logic can
 * be reused for future Microsoft support.
 */
@Service
public class GoogleExternalIdentityVerifier implements ExternalIdentityVerifier {

    private final String googleClientId;
    private final String googleIssuerUri;
    private final String googleJwkSetUri;

    private volatile JwtDecoder jwtDecoder;

    public GoogleExternalIdentityVerifier(
            @Value("${auth.social.google.client-id:}") String googleClientId,
            @Value("${auth.social.google.issuer-uri:https://accounts.google.com}") String googleIssuerUri,
            @Value("${auth.social.google.jwk-set-uri:https://www.googleapis.com/oauth2/v3/certs}") String googleJwkSetUri) {
        this.googleClientId = googleClientId;
        this.googleIssuerUri = googleIssuerUri;
        this.googleJwkSetUri = googleJwkSetUri;
    }

    @Override
    public AuthIdentityProvider getProvider() {
        return AuthIdentityProvider.GOOGLE;
    }

    @Override
    public VerifiedExternalIdentity verify(String providerToken) {
        if (googleClientId == null || googleClientId.isBlank()) {
            throw new CustomAuthException("Google sign-in is not configured on the server",
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }

        try {
            Jwt jwt = getJwtDecoder().decode(providerToken);
            String subject = jwt.getSubject();
            String email = jwt.getClaimAsString("email");
            Boolean emailVerified = jwt.getClaimAsBoolean("email_verified");

            if (subject == null || subject.isBlank()) {
                throw new CustomAuthException("Google token is missing subject", HttpStatus.UNAUTHORIZED);
            }
            if (email == null || email.isBlank()) {
                throw new CustomAuthException("Google token is missing email", HttpStatus.UNAUTHORIZED);
            }

            return new VerifiedExternalIdentity(
                    AuthIdentityProvider.GOOGLE,
                    subject,
                    email.trim().toLowerCase(),
                    Boolean.TRUE.equals(emailVerified),
                    jwt.getClaimAsString("name"),
                    jwt.getClaimAsString("picture"));
        } catch (JwtException ex) {
            throw new CustomAuthException("Invalid Google ID token", HttpStatus.UNAUTHORIZED);
        }
    }

    private JwtDecoder getJwtDecoder() {
        JwtDecoder decoder = this.jwtDecoder;
        if (decoder == null) {
            synchronized (this) {
                decoder = this.jwtDecoder;
                if (decoder == null) {
                    NimbusJwtDecoder nimbusJwtDecoder = NimbusJwtDecoder.withJwkSetUri(googleJwkSetUri).build();
                    OAuth2TokenValidator<Jwt> withIssuer = JwtValidators.createDefaultWithIssuer(googleIssuerUri);
                    OAuth2TokenValidator<Jwt> audienceValidator = token -> {
                        List<String> audiences = token.getAudience();
                        if (audiences != null && audiences.contains(googleClientId)) {
                            return OAuth2TokenValidatorResult.success();
                        }
                        return OAuth2TokenValidatorResult.failure(new OAuth2Error(
                                "invalid_token",
                                "Google token audience does not match the configured client id",
                                null));
                    };
                    nimbusJwtDecoder.setJwtValidator(
                            new DelegatingOAuth2TokenValidator<>(withIssuer, audienceValidator));
                    decoder = nimbusJwtDecoder;
                    this.jwtDecoder = decoder;
                }
            }
        }
        return decoder;
    }
}
