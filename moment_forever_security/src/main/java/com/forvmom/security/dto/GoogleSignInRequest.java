package com.forvmom.security.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body for Google sign-in.
 *
 * <p>
 * The client authenticates with Google first, then sends the returned OIDC ID
 * token here so the backend can verify it and issue the platform's own JWT and
 * refresh token pair.
 */
public class GoogleSignInRequest {

    @NotBlank(message = "Google ID token is required")
    private String idToken;

    public String getIdToken() {
        return idToken;
    }

    public void setIdToken(String idToken) {
        this.idToken = idToken;
    }
}
