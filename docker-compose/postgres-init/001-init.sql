-- elastic-query-service uses schema "public" (default, no action needed).
-- analytics-service uses its own schema.
CREATE SCHEMA IF NOT EXISTS analytics;

-- keycloak-authorization-server (services.yml) expects its own database.
CREATE DATABASE keycloak;
