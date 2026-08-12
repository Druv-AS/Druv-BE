-- Pre-deployment inspection for the Supabase database.
-- Read-only: run this in the Supabase SQL editor before pointing the hardened backend
-- at it. Nothing here modifies data.
--
-- What we need to know:
--   1. Has Flyway ever run here, or did Hibernate's ddl-auto=update improvise the schema?
--   2. Do real user accounts exist that V3 would lock out?
--   3. Does the existing shape match what ddl-auto=validate will demand?

-- 1 ── Flyway history -------------------------------------------------------------
-- Empty result => Flyway has never run. The tables (if any) were created by Hibernate,
-- and baseline-on-migrate will skip V1 rather than apply it.
SELECT installed_rank, version, description, success, installed_on
FROM flyway_schema_history
ORDER BY installed_rank;
-- If this errors with "relation does not exist", that is the answer: Flyway never ran.


-- 2 ── What tables actually exist -------------------------------------------------
SELECT table_name
FROM information_schema.tables
WHERE table_schema = 'public'
ORDER BY table_name;


-- 3 ── Accounts at risk from V3 ---------------------------------------------------
-- V3 nulls every password that is not a BCrypt hash ("$2..."), which makes those
-- accounts unauthenticatable. There is no password-reset flow yet, so each one must be
-- re-registered. Confirm this count is acceptable BEFORE deploying.
SELECT
    count(*)                                                        AS total_students,
    count(*) FILTER (WHERE password IS NULL)                        AS no_password,
    count(*) FILTER (WHERE password LIKE '$2%')                     AS already_hashed,
    count(*) FILTER (WHERE password IS NOT NULL
                       AND password NOT LIKE '$2%')                 AS plaintext_to_be_cleared
FROM students;

SELECT
    count(*)                                                        AS total_parents,
    count(*) FILTER (WHERE password IS NULL)                        AS no_password,
    count(*) FILTER (WHERE password LIKE '$2%')                     AS already_hashed,
    count(*) FILTER (WHERE password IS NOT NULL
                       AND password NOT LIKE '$2%')                 AS plaintext_to_be_cleared
FROM parents;


-- 4 ── Are these real users or leftover seed rows? --------------------------------
-- The dev seeder used +919876543210 / +919876543211 / +919876543222. Anything else is
-- a real signup. (Passwords are deliberately not selected.)
SELECT user_id, phone_number, name, target_course, created_at
FROM students
ORDER BY created_at;


-- 5 ── Column shape vs. what the entities expect ----------------------------------
-- ddl-auto=validate compares these against the JPA entities and refuses to start on a
-- mismatch. Extra columns are tolerated; missing or wrongly-typed ones are not.
SELECT table_name, column_name, data_type, character_maximum_length, is_nullable
FROM information_schema.columns
WHERE table_schema = 'public'
  AND table_name IN ('students', 'parents', 'parent_weekly_reports',
                     'timetables', 'timetable_slots')
ORDER BY table_name, ordinal_position;


-- 6 ── Anything Supabase-specific attached to these tables? -----------------------
-- Row Level Security is off by default on tables Hibernate created, but confirm: the
-- backend connects as a single role and does not set request.jwt.claims, so an enabled
-- RLS policy would silently return zero rows rather than error.
SELECT relname AS table_name, relrowsecurity AS rls_enabled
FROM pg_class
WHERE relnamespace = 'public'::regnamespace
  AND relkind = 'r'
ORDER BY relname;
