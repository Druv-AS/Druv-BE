-- Flyway Migration V2: Align schema with JPA entities
-- Fixes schema mismatches between V1 migration and actual Hibernate entities

-- Add missing columns to students table
ALTER TABLE students ADD COLUMN IF NOT EXISTS user_id VARCHAR(100) UNIQUE;
ALTER TABLE students ADD COLUMN IF NOT EXISTS parent_phone_number VARCHAR(20);
ALTER TABLE students ADD COLUMN IF NOT EXISTS password VARCHAR(255);

-- Create parents table (JPA entity maps to 'parents', not 'parent_profiles')
CREATE TABLE IF NOT EXISTS parents (
    id UUID PRIMARY KEY,
    user_id VARCHAR(100) UNIQUE NOT NULL,
    name VARCHAR(100) NOT NULL,
    phone_number VARCHAR(20) UNIQUE NOT NULL,
    password VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Add missing columns to parent_weekly_reports table
ALTER TABLE parent_weekly_reports ADD COLUMN IF NOT EXISTS student_name VARCHAR(100);
ALTER TABLE parent_weekly_reports ADD COLUMN IF NOT EXISTS exam_target VARCHAR(100);
ALTER TABLE parent_weekly_reports ADD COLUMN IF NOT EXISTS overall_eri DOUBLE PRECISION DEFAULT 72.4;
ALTER TABLE parent_weekly_reports ADD COLUMN IF NOT EXISTS is_sent_to_parent BOOLEAN DEFAULT FALSE;
ALTER TABLE parent_weekly_reports ADD COLUMN IF NOT EXISTS sent_at TIMESTAMP WITH TIME ZONE;

-- Performance indexes for new columns
CREATE INDEX IF NOT EXISTS idx_students_user_id ON students(user_id);
CREATE INDEX IF NOT EXISTS idx_students_parent_phone ON students(parent_phone_number);
CREATE INDEX IF NOT EXISTS idx_parents_phone ON parents(phone_number);
CREATE INDEX IF NOT EXISTS idx_parents_user_id ON parents(user_id);
