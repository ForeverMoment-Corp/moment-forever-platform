package com.forvmom.common.enums;

/**
 * External identity providers that can be linked to a local auth account.
 *
 * <p>
 * Google is implemented first, but the enum intentionally includes Microsoft so
 * the persistence and service layers are provider-agnostic from day one.
 */
public enum AuthIdentityProvider {
    GOOGLE,
    MICROSOFT
}
