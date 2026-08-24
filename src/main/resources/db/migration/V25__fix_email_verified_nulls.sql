UPDATE sellers SET email_verified = FALSE WHERE email_verified IS NULL;
ALTER TABLE sellers
    ALTER COLUMN email_verified SET DEFAULT FALSE,
    ALTER COLUMN email_verified SET NOT NULL;

UPDATE users SET email_verified = FALSE WHERE email_verified IS NULL;
ALTER TABLE users
    ALTER COLUMN email_verified SET DEFAULT FALSE,
    ALTER COLUMN email_verified SET NOT NULL;
