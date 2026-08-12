-- Flyway Migration V3: neutralise plaintext credentials and tighten constraints.
--
-- Up to V2 the `password` columns held plaintext. Application code now writes only BCrypt
-- hashes, but any row written by an earlier build still contains a readable password. Those
-- values must not remain usable: a stale plaintext value would otherwise be compared against
-- a BCrypt hash and simply fail, while still sitting in the database as a leaked secret.
--
-- BCrypt hashes always begin with the "$2" version marker, so anything else is legacy.
-- Setting it to NULL both erases the secret and makes the account unauthenticatable, which
-- AuthService reports as invalid credentials. Affected accounts must be re-registered (or
-- go through password reset once that flow exists).

UPDATE students
   SET password = NULL
 WHERE password IS NOT NULL
   AND password NOT LIKE '$2%';

UPDATE parents
   SET password = NULL
 WHERE password IS NOT NULL
   AND password NOT LIKE '$2%';

-- The parent portal resolves children by parent_phone_number on every page load, and the
-- link check compares against it. V2 added the index; this widens the column to match the
-- canonical "+91XXXXXXXXXX" form the application now writes.
ALTER TABLE students ALTER COLUMN parent_phone_number TYPE VARCHAR(20);

-- Reports are always read newest-first for one student; the single-column index on
-- student_id alone forced a sort on every parent portal load.
CREATE INDEX IF NOT EXISTS idx_parent_reports_student_created
    ON parent_weekly_reports (student_id, created_at DESC);
