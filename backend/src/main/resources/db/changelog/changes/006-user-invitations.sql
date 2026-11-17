--liquibase formatted sql

--changeset auth-server:006-user-invitations
ALTER TABLE auth_local_credential
    ADD COLUMN credential_source varchar(50) NOT NULL DEFAULT 'local-bootstrap';

ALTER TABLE auth_local_credential
    ADD CONSTRAINT ck_auth_local_credential_source
        CHECK (credential_source IN ('local-bootstrap', 'invitation'));

CREATE TABLE auth_user_invitation (
    id uuid NOT NULL,
    email varchar(254) NOT NULL,
    token_hash varchar(64) NOT NULL,
    grant_auth_admin boolean NOT NULL DEFAULT false,
    expires_at timestamp with time zone NOT NULL,
    created_by uuid NOT NULL,
    created_at timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    consumed_at timestamp with time zone,
    consumed_user_id uuid,
    revoked_at timestamp with time zone,
    CONSTRAINT pk_auth_user_invitation PRIMARY KEY (id),
    CONSTRAINT uk_auth_user_invitation_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_auth_user_invitation_created_by FOREIGN KEY (created_by) REFERENCES auth_user (id),
    CONSTRAINT fk_auth_user_invitation_consumed_user FOREIGN KEY (consumed_user_id) REFERENCES auth_user (id),
    CONSTRAINT ck_auth_user_invitation_email_not_blank CHECK (btrim(email) <> ''),
    CONSTRAINT ck_auth_user_invitation_expiry CHECK (expires_at > created_at),
    CONSTRAINT ck_auth_user_invitation_consumption CHECK (
        (consumed_at IS NULL AND consumed_user_id IS NULL) OR
        (consumed_at IS NOT NULL AND consumed_user_id IS NOT NULL)
    )
);

CREATE INDEX idx_auth_user_invitation_email ON auth_user_invitation (lower(email));
CREATE INDEX idx_auth_user_invitation_created_at ON auth_user_invitation (created_at DESC);
