# API Error Handling Reference

This document explains the **error payload shape**, **HTTP codes**, **`status` values**, and the **business/code paths** that currently produce them in the platform service.

Its purpose is simple: when UI, QA, backend, or support sees an API failure, they should be able to understand **why that code was returned** without reading the source first.

## 1. Standard error response shape

All API error responses are expected to use the shared `ApiResponse` envelope:

```json
{
  "code": 409,
  "status": "CONFLICT",
  "msg": "Coupon code already exists: SAVE10",
  "response": null,
  "errors": null
}
```

For validation failures, the `errors` array is populated:

```json
{
  "code": 400,
  "status": "BAD_REQUEST",
  "msg": "Validation failed",
  "response": null,
  "errors": [
    "email: must not be blank",
    "password: size must be between 8 and 30"
  ]
}
```

### Canonical fields

| Field | Meaning |
|---|---|
| `code` | HTTP status code returned by the API |
| `status` | `HttpStatus.name()` value, for example `BAD_REQUEST`, `CONFLICT`, `NOT_FOUND` |
| `msg` | Human-readable summary of the failure |
| `response` | Always `null` for error cases |
| `errors` | Present mainly for validation-style failures; usually `null` otherwise |

## 2. Core error-mapping code

These files define the platform-wide error behavior:

| File | Responsibility |
|---|---|
| `moment_forever_commons/src/main/java/com/forvmom/common/response/ApiResponse.java` | Shared API envelope |
| `moment_forever_commons/src/main/java/com/forvmom/common/response/ResponseUtil.java` | Builds success/error payloads |
| `moment_forever_commons/src/main/java/com/forvmom/common/errorhandler/GlobalExceptionHandler.java` | Maps most Spring/business exceptions to HTTP responses |
| `moment_forever_security/src/main/java/com/forvmom/security/jwt_exception_handler/JwtAuthenticationEntryPoint.java` | Handles unauthenticated security failures |
| `moment_forever_security/src/main/java/com/forvmom/security/jwt_exception_handler/JwtAccessDeniedHandler.java` | Handles authenticated-but-forbidden security failures |
| `moment_forever_object_store/src/main/java/com/forvmom/store/exception/ImageExceptionHandler.java` | Handles image/object-store related exceptions |

## 3. HTTP code and status reference

### 400 — `BAD_REQUEST`

Used when the request is **syntactically wrong**, **missing required input**, or **fails validation/business preconditions** that are the caller's responsibility.

| HTTP | `status` | Typical `msg` | Why this happens | Source |
|---|---|---|---|---|
| 400 | `BAD_REQUEST` | `Validation failed` | DTO bean validation failed on `@Valid` request body | `GlobalExceptionHandler.handleValidationExceptions()` |
| 400 | `BAD_REQUEST` | `Validation failed` | Query/path/header binding validation failed | `GlobalExceptionHandler.handleBindException()` |
| 400 | `BAD_REQUEST` | `Validation failed` | Constraint validations failed outside request body binding | `GlobalExceptionHandler.handleConstraintViolation()` |
| 400 | `BAD_REQUEST` | raw exception message | Service explicitly rejected caller input with `IllegalArgumentException` | `GlobalExceptionHandler.handleIllegalArgument()` |
| 400 | `BAD_REQUEST` | `Required request parameter 'x' is missing` | Mandatory request parameter not provided | `GlobalExceptionHandler.handleBadRequest()` |
| 400 | `BAD_REQUEST` | `Required request header 'X-User-Id' is missing` | Mandatory header missing | `GlobalExceptionHandler.handleBadRequest()` |
| 400 | `BAD_REQUEST` | `Request parameter 'id' must be of type Long` | Type mismatch during binding | `GlobalExceptionHandler.handleBadRequest()` |
| 400 | `BAD_REQUEST` | `Malformed or unreadable request body` | Invalid JSON or unreadable body | `GlobalExceptionHandler.handleBadRequest()` |

#### Common business examples currently returning 400

| Message pattern | Why |
|---|---|
| `slotMapperId and bookingDate are required` | Required booking inventory input missing |
| `guestCount must be greater than zero` | Guest count is invalid |
| `Refresh token is required for logout` | Logout API called without refresh token |
| `name, email and phone are required for a guest support query` | Guest support form submission is incomplete |

## 4. 401 — `UNAUTHORIZED`

Used when the caller is **not authenticated**, **has an invalid/expired token**, or the backend cannot trust the current principal/session.

| HTTP | `status` | Typical `msg` | Why this happens | Source |
|---|---|---|---|---|
| 401 | `UNAUTHORIZED` | `Authentication required or token is invalid` | Request hit a protected endpoint without valid authentication | `JwtAuthenticationEntryPoint.commence()` |
| 401 | `UNAUTHORIZED` | `Please register before logging in. User not found: {email}` | Login attempted for an email not registered in `ApplicationUser` | `AuthService.login()` |
| 401 | `UNAUTHORIZED` | `Invalid email or password` | Login password mismatch | `AuthService.validateUserCredentials()` |
| 401 | `UNAUTHORIZED` | `Invalid refresh token` | Refresh token rotation/revocation lookup failed | `JwtService.generateRefreshTokenWithAccessToken()`, `JwtService.revokeRefreshToken()` |
| 401 | `UNAUTHORIZED` | `Refresh token expired` | Refresh token exists but is expired or invalid in storage | `JwtService.generateRefreshTokenWithAccessToken()` |
| 401 | `UNAUTHORIZED` | `User not found from refresh token` | Refresh token points to a user record that no longer exists | `JwtService.generateRefreshTokenWithAccessToken()` |
| 401 | `UNAUTHORIZED` | `No authenticated user` | Service expected a logged-in principal but found none | `UserProfileService`, `VendorServiceImpl` |
| 401 | `UNAUTHORIZED` | `Invalid principal type` | Principal in security context could not be resolved to the expected user id | `UserProfileService`, `VendorServiceImpl` |
| 401 | `UNAUTHORIZED` | `Invalid password` | Password re-check failed before account deletion | `UserProfileService.deleteCurrentUserProfile()` |
| 401 | `UNAUTHORIZED` | `Authenticated user record not found` | Security context contained a user id but auth user record was missing | `VendorServiceImpl.createOrUpdateCurrentVendorProfile()` |

### Business meaning

For UI purposes, **401 means the user should re-authenticate or the session/token state is invalid**.

## 5. 403 — `FORBIDDEN`

Used when the caller is authenticated, but the action is **not allowed** due to role or account state.

| HTTP | `status` | Typical `msg` | Why this happens | Source |
|---|---|---|---|---|
| 403 | `FORBIDDEN` | `Forbidden - you do not have permission to perform this action` | Spring Security denied access due to role/authority mismatch | `JwtAccessDeniedHandler.handle()` |
| 403 | `FORBIDDEN` | `Your account is locked. Please contact support.` | Auth user exists but account is locked | `AuthService.login()` |
| 403 | `FORBIDDEN` | `Your account is expired. Please verify your email.` | Auth user exists but account is expired | `AuthService.login()` |
| 403 | `FORBIDDEN` | custom business message | Operation explicitly blocked by business rule | `NotAllowedCustomException` via `GlobalExceptionHandler.handleNotAllowed()` |

### Business meaning

For UI purposes, **403 means “you are logged in, but you cannot do this action.”**

## 6. 404 — `NOT_FOUND`

Used when the requested business resource does not exist, or does not exist in the expected relationship.

| HTTP | `status` | Typical `msg` | Why this happens | Source |
|---|---|---|---|---|
| 404 | `NOT_FOUND` | dynamic resource-specific message | Service threw `ResourceNotFoundException` | `GlobalExceptionHandler.handleResourceNotFound()` |
| 404 | `NOT_FOUND` | image/object specific message | Image lookup failed | `ImageExceptionHandler.handleImageNotFound()` |

### Common message patterns

These are intentionally specific so the UI and QA can understand exactly what is missing:

- `Location not found: {id}`
- `Experience not found: {id}`
- `Policy not found: {id}`
- `Inclusion not found: {id}`
- `Coupon not found with id: {id}`
- `Category media mapping not found: {mapperId}`
- `Category {id} is not attached to location {locationId}`
- `SubCategory {id} is not attached to location {locationId}`
- `AppUser not found for id: {id}`
- `Image not found with id: {id}`

### Business meaning

For UI purposes, **404 means the requested entity is missing or the relationship the API expects is missing**.

## 7. 405 — `METHOD_NOT_ALLOWED`

Used when the endpoint exists but the HTTP method is wrong.

| HTTP | `status` | `msg` pattern | Why this happens | Source |
|---|---|---|---|---|
| 405 | `METHOD_NOT_ALLOWED` | `HTTP method 'POST' is not supported for this endpoint. Supported methods: GET, PUT` | Wrong HTTP method used on a valid route | `GlobalExceptionHandler.handleMethodNotSupported()` |

### Business meaning

This is usually an API integration issue or a frontend wiring bug.

## 8. 409 — `CONFLICT`

Used when the request is valid, but **current business state does not allow the action**.

| HTTP | `status` | Typical `msg` | Why this happens | Source |
|---|---|---|---|---|
| 409 | `CONFLICT` | dynamic conflict message | Service threw `ConflictException` | `GlobalExceptionHandler.handleConflict()` |
| 409 | `CONFLICT` | idempotency-specific message | Duplicate request key or in-progress request | `IdempotencyConflictException`, `IdempotencyRequestInProgressException` |

### Important special case

When the request is still being processed under the same idempotency key:

- HTTP code: `409`
- `status`: `CONFLICT`
- header: `Retry-After: 1`

This is emitted by `GlobalExceptionHandler.handleIdempotencyRequestInProgress()`.

### Current business conflict messages in code

#### Duplicate master/business identifiers

- `Coupon code already exists: {code}`
- `Email already in use: {email}`

#### Duplicate experience/location/media mappings

- `Location {locationId} is already attached to experience {experienceId}`
- `Policy {policyId} is already attached to experience {experienceId}`
- `Inclusion {inclusionId} is already attached to experience {experienceId}`
- `TimeSlot {timeSlotId} is already attached to this experience-location.`
- `Media {mediaId} is already attached to experience {experienceId}.`
- `Media {mediaId} is already attached to category {categoryId}.`
- `Media {mediaId} is already attached to sub-category {subCategoryId}.`
- `Addon {addonId} is already attached to experience {experienceId}`
- `Category {categoryId} is already attached to location {locationId}`
- `SubCategory {subCategoryId} is already attached to location {locationId}`

#### Inactive or unusable business state

- `Media is inactive: {mediaId}`
- `Time slot is missing, inactive, or invalid on date {bookingDate}`
- `Time slot does not have enough capacity. Requested={guestCount}, booked={currentlyBooked}, maxCapacity={maxCapacity}`
- `No matching inventory reservation for slotMapperId={slotMapperId}, bookingDate={bookingDate}, guestCount={guestCount}`
- `Booking reservation not found: {bookingReferenceId}`
- `Booking reservation could not be released from status {status}: {bookingReferenceId}`
- `Booking failure details do not match reservation: {bookingReferenceId}`

### Business meaning

For UI purposes, **409 means the user request is understood, but the operation collides with current business state** — duplicate attach, duplicate code/email, inactive media, invalid slot state, capacity exhausted, or idempotency collision.

## 9. 413 — `PAYLOAD_TOO_LARGE`

Used when an uploaded file exceeds configured multipart limits.

| HTTP | `status` | `msg` | Why this happens | Source |
|---|---|---|---|---|
| 413 | `PAYLOAD_TOO_LARGE` | `Uploaded file is too large` | Multipart upload exceeded configured max size | `GlobalExceptionHandler.handleMaxUploadSizeExceeded()` |

## 10. 415 — `UNSUPPORTED_MEDIA_TYPE`

Used when request `Content-Type` is not acceptable for the endpoint.

| HTTP | `status` | `msg` pattern | Why this happens | Source |
|---|---|---|---|---|
| 415 | `UNSUPPORTED_MEDIA_TYPE` | `Unsupported Content-Type. Supported types: ...` | Wrong `Content-Type` for endpoint, usually multipart vs JSON mismatch | `GlobalExceptionHandler.handleUnsupportedMediaType()` |

### Business meaning

This is especially relevant for **media upload APIs**.

## 11. 500 — `INTERNAL_SERVER_ERROR`

Used when the backend hits an **unexpected technical failure**, or when the failure is internal and not attributable to caller input or business-state conflict.

| HTTP | `status` | Typical `msg` | Why this happens | Source |
|---|---|---|---|---|
| 500 | `INTERNAL_SERVER_ERROR` | `Unexpected server error. Please contact support if the problem persists.` | Unhandled exception bubbled up to global fallback | `GlobalExceptionHandler.handleUnexpectedException()` |
| 500 | `INTERNAL_SERVER_ERROR` | `Logout failed` | Refresh-token revoke flow failed unexpectedly | `AuthService.logout()` |
| 500 | `INTERNAL_SERVER_ERROR` | image/object-store exception message | File storage, retrieval, metadata, list, or delete failed | `ImageExceptionHandler` |

### Business meaning

For UI purposes, **500 means a server-side technical problem**. The request may be valid, but the system could not complete it safely.

## 12. Status selection rules

These are the design rules currently enforced by the code:

1. **400** — caller sent bad or incomplete input.
2. **401** — caller is unauthenticated, session is invalid, or token flow failed.
3. **403** — caller is authenticated but blocked by permissions/account state.
4. **404** — entity or expected relationship does not exist.
5. **405** — route exists, wrong HTTP method used.
6. **409** — entity exists or request is valid, but business state conflicts with the operation.
7. **413** — uploaded payload too large.
8. **415** — unsupported content type.
9. **500** — internal technical failure or unknown/unhandled server error.

## 13. UI handling guidance

Recommended UI behavior:

| HTTP code | UI behavior |
|---|---|
| 400 | Show inline validation or form-level error; use `errors[]` when present |
| 401 | Redirect to login / refresh auth / ask user to sign in again |
| 403 | Show “not allowed” state; do not ask user to retry blindly |
| 404 | Show not-found / stale-link / deleted-resource state |
| 405 | Treat as frontend integration bug |
| 409 | Show actionable business conflict message from `msg` |
| 413 | Ask user to reduce file size |
| 415 | Ask user/frontend to send the correct content type |
| 500 | Show generic retry/support message |

## 14. Notes for future developers

- If you add a new business-state collision, prefer **`ConflictException`** so the API returns **409 `CONFLICT`**.
- If you add a new missing-resource path, prefer **`ResourceNotFoundException`** so the API returns **404 `NOT_FOUND`**.
- If you add a new auth/business permission failure, set the appropriate status on **`CustomAuthException`** or **`NotAllowedCustomException`** rather than returning a generic 500.
- If you add a new framework-level failure path, update `GlobalExceptionHandler` and this document together.

## 15. Current implementation files to review first

If behavior changes in future, start with these files:

- `moment_forever_commons/src/main/java/com/forvmom/common/errorhandler/GlobalExceptionHandler.java`
- `moment_forever_commons/src/main/java/com/forvmom/common/response/ResponseUtil.java`
- `moment_forever_commons/src/main/java/com/forvmom/common/errorhandler/ConflictException.java`
- `moment_forever_commons/src/main/java/com/forvmom/common/errorhandler/CustomAuthException.java`
- `moment_forever_security/src/main/java/com/forvmom/security/jwt_exception_handler/JwtAuthenticationEntryPoint.java`
- `moment_forever_security/src/main/java/com/forvmom/security/jwt_exception_handler/JwtAccessDeniedHandler.java`
- `moment_forever_object_store/src/main/java/com/forvmom/store/exception/ImageExceptionHandler.java`

