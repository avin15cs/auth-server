--liquibase formatted sql
--changeset auth-server:004-managed-oauth-clients

CREATE TABLE auth_client_metadata (
    registered_client_id varchar(100) NOT NULL,
    client_type varchar(20) NOT NULL,
    audience varchar(200) NOT NULL,
    enabled boolean NOT NULL DEFAULT true,
    source varchar(20) NOT NULL,
    created_by uuid,
    created_at timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by uuid,
    updated_at timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_auth_client_metadata PRIMARY KEY (registered_client_id),
    CONSTRAINT fk_auth_client_metadata_client FOREIGN KEY (registered_client_id)
        REFERENCES oauth2_registered_client (id) ON DELETE CASCADE,
    CONSTRAINT fk_auth_client_metadata_created_by FOREIGN KEY (created_by) REFERENCES auth_user (id),
    CONSTRAINT fk_auth_client_metadata_updated_by FOREIGN KEY (updated_by) REFERENCES auth_user (id),
    CONSTRAINT ck_auth_client_metadata_type CHECK (client_type IN ('INTERACTIVE', 'SERVICE')),
    CONSTRAINT ck_auth_client_metadata_source CHECK (source IN ('BOOTSTRAP', 'DYNAMIC')),
    CONSTRAINT ck_auth_client_metadata_audience_not_blank CHECK (btrim(audience) <> '')
);

CREATE INDEX idx_auth_client_metadata_audience ON auth_client_metadata (audience);
CREATE INDEX idx_auth_client_metadata_enabled ON auth_client_metadata (enabled);
