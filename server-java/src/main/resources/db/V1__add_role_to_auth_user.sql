-- Add role column to auth_user table.
-- Existing users default to 'user'. Manually update the blog owner to 'admin'.
ALTER TABLE auth_user
  ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'user' AFTER password_hash;

-- Set the first registered user (blog owner) as admin.
UPDATE auth_user SET role = 'admin' WHERE id = 1;
