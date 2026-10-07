# Authentication Flow Guide

This document explains how authentication works in Moment Forever after the Google sign-in enhancement. It is written for developers and support teammates who need to understand the codebase quickly without tracing every class.

## 1. Why we keep two user tables

The platform intentionally separates **authentication** from **application profile** data.

| Table / entity | Purpose |
|---|---|
| `auth_users` / `AuthUser` | Login identity, password, account flags, last login, soft delete state |
| `application_users` / `ApplicationUser` | Business/profile data such as full name, phone number, city, picture |
| `auth_user_roles` / `AuthUserRole` | Many-to-many mapping between users and roles |
| `refresh_tokens` / `RefreshToken` | Long-lived session continuation tokens |
| `auth_identities` / `AuthIdentity` | External provider links such as Google now, Microsoft later |

This split is useful because account-security state and user-facing profile state change for different reasons:

- security/admin teams may lock or disable an account,
- product flows may update profile details without touching credentials,
- deleting a profile should also remove login access,
- external providers can be linked without changing the local JWT model.

## 2. Local email/password flow

### Registration

`POST /auth/register`

Current registration still creates:

1. one `AuthUser`,
2. one `ApplicationUser`,
3. one or more `AuthUserRole` rows.

What changed:

- email is normalized before storing,
- if the same email belongs to a **soft-deleted** account, the backend now **reactivates** that account instead of creating duplicates or failing later on unique constraints.

Important classes:

- `AuthService`
- `AuthBeanMapper`
- `AppUserBeanMapper`
- `RoleDao`

### Login

`POST /auth/login`

The backend:

1. finds the `ApplicationUser` by email,
2. loads the linked `AuthUser`,
3. checks account flags (`enabled`, `accountNonLocked`, `accountNonExpired`, `credentialsNonExpired`, `deleted`),
4. verifies the password,
5. issues the platform JWT access token and refresh token.

The token issuing logic is centralized in `AuthSessionService` so every login mechanism returns the same session shape.

## 3. Refresh and logout flow

### Refresh

`POST /auth/refresh`

The refresh token is exchanged for a new access token and refresh token pair through `JwtService`.

### Logout

`POST /auth/logout`

Logout revokes the refresh token. After revocation, the client can no longer continue the session indefinitely through refresh.

## 4. Google sign-in flow

### Endpoint

`POST /auth/social/google`

Request body:

```json
{
  "idToken": "google-id-token-from-client"
}
```

### What happens internally

1. The client signs in with Google and gets a Google ID token.
2. The backend verifies that token using Google's issuer, audience, and JWK set.
3. The verified Google identity is converted into a shared internal model: `VerifiedExternalIdentity`.
4. The backend resolves that identity into the local account model.
5. The backend issues the platform's own JWT + refresh token, exactly like local login.

This means Google is used only for **identity proof**. The platform still owns:

- roles,
- access tokens,
- refresh tokens,
- account lock/disable rules,
- downstream authorization behavior.

### Account resolution rules

For Google sign-in, resolution happens in this order:

1. existing active `AuthIdentity` link,
2. existing soft-deleted `AuthIdentity` link,
3. existing local account by verified email,
4. soft-deleted local account by verified email,
5. create a brand-new local account.

### Existing email behavior

If Google returns a **verified email** that already exists locally, the backend **auto-links** the Google account to that existing local account.

That matches the business decision taken for this implementation.

### Why `AuthIdentity` was added

Google and Microsoft do not fit cleanly into `auth_users.external_user_id` because:

- provider subject ids are not guaranteed to be numeric,
- one local account may need multiple provider links,
- each provider carries different metadata.

So `AuthIdentity` is now the dedicated provider-link table.

## 5. Soft delete behavior

### Auth side

`AuthUser` now supports soft delete through the `deleted` flag.

On delete, the backend marks the auth record as:

- `deleted = true`
- `enabled = false`
- `accountNonLocked = false`
- `accountNonExpired = false`
- `credentialsNonExpired = false`

### Profile side

`ApplicationUser` already used soft delete and continues to do so.

### Why this matters

Before this change, a soft-deleted user could still block re-registration because the unique email/username rows still existed in the database.

Now the backend restores the deleted records when the business flow wants the same person back, instead of inserting conflicting duplicates.

## 6. Delete and restore lifecycle

Deleting a profile/account now also:

- revokes all active refresh tokens for that auth user,
- soft-deletes the `ApplicationUser`,
- soft-deletes/disables the linked `AuthUser`.

This closes the gap where a deleted user could still continue with refresh tokens or where only one side of the account pair was deleted.

## 7. Key classes to know

| Class | Responsibility |
|---|---|
| `AuthController` | Auth API entry points |
| `AuthService` | Local register/login flow |
| `SocialAuthService` | Google sign-in orchestration and account linking |
| `GoogleExternalIdentityVerifier` | Validates Google ID tokens |
| `AuthSessionService` | Issues platform JWT + refresh-token session |
| `JwtService` | Token generation, refresh, revocation |
| `AuthUserDao` | Auth-user reads including active vs deleted access |
| `ApplicationUserDao` | Profile reads including deleted-account recovery |
| `AuthIdentityDao` | Provider-link lookups |
| `UserProfileService` | Self-service profile updates/deletes |
| `AdminUserService` | Admin-driven account/profile operations |

## 8. Configuration needed for Google

`application.yml` now expects:

```yaml
auth:
  social:
    google:
      client-id: ${GOOGLE_OIDC_CLIENT_ID:}
      issuer-uri: ${GOOGLE_OIDC_ISSUER:https://accounts.google.com}
      jwk-set-uri: ${GOOGLE_OIDC_JWK_SET_URI:https://www.googleapis.com/oauth2/v3/certs}
```

At minimum, `GOOGLE_OIDC_CLIENT_ID` must be configured for Google login to work.

## 9. How this extends to Microsoft later

The design is already prepared for more providers:

- `AuthIdentityProvider` is an enum,
- `ExternalIdentityVerifier` is the extension point,
- `VerifiedExternalIdentity` is provider-neutral,
- `SocialAuthService` contains shared link/provision/session logic.

To add Microsoft later, the expected approach is:

1. add `MICROSOFT` verifier implementation,
2. verify Microsoft ID tokens,
3. map claims into `VerifiedExternalIdentity`,
4. reuse the same `SocialAuthService` account-resolution flow.

## 10. Mental model for debugging

When auth issues happen, check in this order:

1. **Provider verification** — invalid or misconfigured Google token?
2. **Identity link** — is `auth_identities` missing, duplicated, or soft-deleted?
3. **Profile link** — does `application_users.auth_user_id` still point correctly?
4. **Account flags** — is the auth user locked, disabled, expired, or deleted?
5. **Refresh token state** — was the token revoked?

That sequence matches how the code now resolves and authenticates users.
