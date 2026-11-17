--liquibase formatted sql

--changeset auth-server:005-local-credentials
CREATE TABLE auth_local_credential (
    user_id uuid NOT NULL,
    username varchar(100) NOT NULL,
    password_hash varchar(255) NOT NULL,
    enabled boolean NOT NULL DEFAULT true,
    created_at timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_auth_local_credential PRIMARY KEY (user_id),
    CONSTRAINT uk_auth_local_credential_username UNIQUE (username),
    CONSTRAINT fk_auth_local_credential_user FOREIGN KEY (user_id)
        REFERENCES auth_user (id) ON DELETE CASCADE
);
