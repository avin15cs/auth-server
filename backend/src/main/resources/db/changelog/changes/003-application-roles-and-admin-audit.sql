--liquibase formatted sql
--changeset auth-server:003-application-roles-and-admin-audit

CREATE TABLE auth_user_role (
    user_id uuid NOT NULL,
    audience varchar(200) NOT NULL,
    role varchar(100) NOT NULL,
    assignment_source varchar(50) NOT NULL,
    assigned_by uuid,
    assigned_at timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_auth_user_role PRIMARY KEY (user_id, audience, role),
    CONSTRAINT fk_auth_user_role_user FOREIGN KEY (user_id) REFERENCES auth_user (id),
    CONSTRAINT fk_auth_user_role_assigned_by FOREIGN KEY (assigned_by) REFERENCES auth_user (id),
    CONSTRAINT ck_auth_user_role_audience_not_blank CHECK (btrim(audience) <> ''),
    CONSTRAINT ck_auth_user_role_role_not_blank CHECK (btrim(role) <> '')
);

CREATE INDEX idx_auth_user_role_audience_role
    ON auth_user_role (audience, role);

CREATE TABLE admin_audit_event (
    id uuid NOT NULL,
    actor_user_id uuid,
    action varchar(100) NOT NULL,
    target_type varchar(100) NOT NULL,
    target_id varchar(255) NOT NULL,
    details text NOT NULL,
    occurred_at timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_admin_audit_event PRIMARY KEY (id),
    CONSTRAINT fk_admin_audit_event_actor FOREIGN KEY (actor_user_id) REFERENCES auth_user (id),
    CONSTRAINT ck_admin_audit_action_not_blank CHECK (btrim(action) <> ''),
    CONSTRAINT ck_admin_audit_target_type_not_blank CHECK (btrim(target_type) <> ''),
    CONSTRAINT ck_admin_audit_target_id_not_blank CHECK (btrim(target_id) <> '')
);

CREATE INDEX idx_admin_audit_event_occurred_at
    ON admin_audit_event (occurred_at DESC);
