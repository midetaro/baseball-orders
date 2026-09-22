INSERT INTO users (username, password_hash, status, created_at, updated_at)
VALUES (
    'local:test',
    '$2a$10$mYygCQfZMi3Z5gdLlyFBue50HQ5qAb3CqP1Z2nag9BSnUGDsS8G62',
    'ACTIVE',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
