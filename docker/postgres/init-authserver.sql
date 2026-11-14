SELECT 'CREATE ROLE authserver LOGIN PASSWORD ''local-auth-password'''
WHERE NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'authserver')\gexec

ALTER ROLE authserver WITH LOGIN PASSWORD 'local-auth-password';

SELECT 'CREATE DATABASE authserver OWNER authserver'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'authserver')\gexec

ALTER DATABASE authserver OWNER TO authserver;
