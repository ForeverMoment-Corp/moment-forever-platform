ALTER TABLE auth_users
ADD COLUMN IF NOT EXISTS deleted BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE IF NOT EXISTS auth_identities (
    id BIGSERIAL PRIMARY KEY,
    auth_user_id BIGINT NOT NULL,
    provider VARCHAR(30) NOT NULL,
    provider_subject VARCHAR(255) NOT NULL,
    provider_email VARCHAR(150),
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    provider_display_name VARCHAR(150),
    provider_picture_url VARCHAR(500),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login_at TIMESTAMP,
    CONSTRAINT fk_auth_identities_auth_user
        FOREIGN KEY (auth_user_id) REFERENCES auth_users(id)
);

CREATE INDEX IF NOT EXISTS idx_auth_identities_auth_user_id
    ON auth_identities (auth_user_id);

CREATE UNIQUE INDEX IF NOT EXISTS ux_auth_identities_provider_subject_active
    ON auth_identities (provider, provider_subject)
    WHERE deleted = FALSE;

CREATE UNIQUE INDEX IF NOT EXISTS ux_auth_identities_auth_user_provider_active
    ON auth_identities (auth_user_id, provider)
    WHERE deleted = FALSE;
