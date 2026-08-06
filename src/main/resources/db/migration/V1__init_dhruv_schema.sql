-- Flyway Migration V1: Initial PostgreSQL Schema for Dhruv
-- Designed for high scalability, UUID keys, and extensible JSON metadata

CREATE TABLE IF NOT EXISTS students (
    id UUID PRIMARY KEY,
    phone_number VARCHAR(20) UNIQUE NOT NULL,
    name VARCHAR(100) NOT NULL,
    target_course VARCHAR(100) NOT NULL,
    level INT DEFAULT 12,
    xp INT DEFAULT 3450,
    streak_count INT DEFAULT 47,
    freeze_buffer_count INT DEFAULT 1,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    metadata TEXT
);

CREATE TABLE IF NOT EXISTS parent_profiles (
    id UUID PRIMARY KEY,
    student_id UUID NOT NULL REFERENCES students(id) ON DELETE CASCADE,
    phone_number VARCHAR(20) NOT NULL,
    name VARCHAR(100) NOT NULL,
    relationship VARCHAR(50) DEFAULT 'Parent',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS timetables (
    id UUID PRIMARY KEY,
    student_id UUID NOT NULL REFERENCES students(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    active_days VARCHAR(100) NOT NULL DEFAULT 'Mon,Tue,Wed,Thu,Fri',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS timetable_slots (
    id UUID PRIMARY KEY,
    timetable_id UUID NOT NULL REFERENCES timetables(id) ON DELETE CASCADE,
    time_slot VARCHAR(50) NOT NULL,
    activity_name TEXT NOT NULL,
    target_mcq_count VARCHAR(50) DEFAULT '—',
    is_completed BOOLEAN DEFAULT FALSE,
    display_order INT NOT NULL
);

CREATE TABLE IF NOT EXISTS readiness_ledgers (
    id UUID PRIMARY KEY,
    student_id UUID NOT NULL REFERENCES students(id) ON DELETE CASCADE,
    overall_eri DOUBLE PRECISION NOT NULL,
    delta_weekly DOUBLE PRECISION DEFAULT 0.0,
    coverage_score DOUBLE PRECISION NOT NULL,
    mastery_score DOUBLE PRECISION NOT NULL,
    retention_score DOUBLE PRECISION NOT NULL,
    exam_skill_score DOUBLE PRECISION NOT NULL,
    consistency_score DOUBLE PRECISION NOT NULL,
    top_leverage_action TEXT,
    status_message TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS syllabus_concept_mastery (
    id UUID PRIMARY KEY,
    student_id UUID NOT NULL REFERENCES students(id) ON DELETE CASCADE,
    subject VARCHAR(50) NOT NULL,
    concept_name VARCHAR(200) NOT NULL,
    weightage_percent DOUBLE PRECISION NOT NULL,
    decay_adjusted_mastery DOUBLE PRECISION NOT NULL,
    status VARCHAR(20) NOT NULL,
    questions_available INT DEFAULT 100
);

CREATE TABLE IF NOT EXISTS focus_sessions (
    id UUID PRIMARY KEY,
    student_id UUID NOT NULL REFERENCES students(id) ON DELETE CASCADE,
    start_time TIMESTAMP WITH TIME ZONE NOT NULL,
    end_time TIMESTAMP WITH TIME ZONE NOT NULL,
    duration_minutes INT NOT NULL,
    subject VARCHAR(50),
    questions_solved INT DEFAULT 0,
    correctness_percent DOUBLE PRECISION DEFAULT 0.0
);

CREATE TABLE IF NOT EXISTS parent_weekly_reports (
    id UUID PRIMARY KEY,
    student_id UUID NOT NULL REFERENCES students(id) ON DELETE CASCADE,
    verified_study_minutes INT NOT NULL,
    effort_rating VARCHAR(100) NOT NULL,
    weekly_win TEXT NOT NULL,
    script_what_to_say TEXT NOT NULL,
    script_what_not_to_say TEXT NOT NULL,
    is_approved BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Performance Indexes
CREATE INDEX IF NOT EXISTS idx_students_phone ON students(phone_number);
CREATE INDEX IF NOT EXISTS idx_timetables_student ON timetables(student_id);
CREATE INDEX IF NOT EXISTS idx_slots_timetable ON timetable_slots(timetable_id);
CREATE INDEX IF NOT EXISTS idx_eri_student ON readiness_ledgers(student_id);
CREATE INDEX IF NOT EXISTS idx_concept_student ON syllabus_concept_mastery(student_id);
CREATE INDEX IF NOT EXISTS idx_parent_reports_student ON parent_weekly_reports(student_id);
