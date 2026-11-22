ALTER TABLE users
    DROP CONSTRAINT uq_users_username;

ALTER TABLE users
    RENAME COLUMN username TO display_name;
