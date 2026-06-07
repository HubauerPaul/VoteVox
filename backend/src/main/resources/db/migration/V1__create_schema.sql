-- =====================================================================
-- VoteVox - V1: Initial Schema
-- =====================================================================
-- Security note: The `votes` table intentionally has NO column or foreign
-- key referencing students. Voter anonymity is enforced at the schema
-- layer. Do NOT add such a column under any circumstances.
-- =====================================================================

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ---------------------------------------------------------------------
-- admin_users
-- ---------------------------------------------------------------------
CREATE TABLE admin_users (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    name          VARCHAR(255) NOT NULL,
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role          VARCHAR(32)  NOT NULL,
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_admin_users_email ON admin_users(email);

-- ---------------------------------------------------------------------
-- students
-- ---------------------------------------------------------------------
CREATE TABLE students (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(255) NOT NULL,
    student_id  VARCHAR(64)  NOT NULL UNIQUE,
    class_name  VARCHAR(32)  NOT NULL,
    department  VARCHAR(64)  NOT NULL,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_students_student_id ON students(student_id);
CREATE INDEX idx_students_class      ON students(class_name);
CREATE INDEX idx_students_department ON students(department);

-- ---------------------------------------------------------------------
-- candidates
-- ---------------------------------------------------------------------
CREATE TABLE candidates (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(255) NOT NULL,
    class_name  VARCHAR(32)  NOT NULL,
    department  VARCHAR(64)  NOT NULL,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ---------------------------------------------------------------------
-- elections
-- ---------------------------------------------------------------------
CREATE TABLE elections (
    id           UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    title        VARCHAR(255) NOT NULL,
    description  TEXT,
    type         VARCHAR(32)  NOT NULL,
    start_time   TIMESTAMP    NOT NULL,
    end_time     TIMESTAMP    NOT NULL,
    status       VARCHAR(32)  NOT NULL,
    created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_elections_window CHECK (end_time > start_time)
);

CREATE INDEX idx_elections_status ON elections(status);
CREATE INDEX idx_elections_type   ON elections(type);

-- ---------------------------------------------------------------------
-- election_candidates (join: candidates eligible per election)
-- ---------------------------------------------------------------------
CREATE TABLE election_candidates (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    election_id   UUID NOT NULL REFERENCES elections(id)  ON DELETE CASCADE,
    candidate_id  UUID NOT NULL REFERENCES candidates(id) ON DELETE CASCADE,
    CONSTRAINT uq_election_candidate UNIQUE (election_id, candidate_id)
);

CREATE INDEX idx_election_candidates_election ON election_candidates(election_id);

-- ---------------------------------------------------------------------
-- tokens (only hash stored - plaintext lives ONLY in QR code)
-- ---------------------------------------------------------------------
CREATE TABLE tokens (
    id                UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    token_value_hash  VARCHAR(64)  NOT NULL UNIQUE,
    generated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_used           BOOLEAN      NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_tokens_hash ON tokens(token_value_hash);
CREATE INDEX idx_tokens_used ON tokens(is_used);

-- ---------------------------------------------------------------------
-- student_tokens (issuance ledger - links student to token per election)
-- This is the only table mapping a student to a token. It is used during
-- token validation to deliver the correct ballot, but the resulting Vote
-- row contains NO reference to this table.
-- ---------------------------------------------------------------------
CREATE TABLE student_tokens (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id   UUID NOT NULL REFERENCES students(id)  ON DELETE CASCADE,
    election_id  UUID NOT NULL REFERENCES elections(id) ON DELETE CASCADE,
    token_id     UUID NOT NULL UNIQUE REFERENCES tokens(id) ON DELETE CASCADE,
    CONSTRAINT uq_student_election UNIQUE (student_id, election_id)
);

CREATE INDEX idx_student_tokens_election ON student_tokens(election_id);
CREATE INDEX idx_student_tokens_student  ON student_tokens(student_id);

-- ---------------------------------------------------------------------
-- votes (ANONYMOUS BALLOT)
-- Intentionally NO student_id, NO token_id, NO student_token_id.
-- Anonymity is enforced at schema level.
-- ---------------------------------------------------------------------
CREATE TABLE votes (
    id                     UUID      PRIMARY KEY DEFAULT gen_random_uuid(),
    election_id            UUID      NOT NULL REFERENCES elections(id)            ON DELETE CASCADE,
    election_candidate_id  UUID      NOT NULL REFERENCES election_candidates(id)  ON DELETE CASCADE,
    cast_at                TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_votes_election           ON votes(election_id);
CREATE INDEX idx_votes_election_candidate ON votes(election_candidate_id);

-- ---------------------------------------------------------------------
-- audit_log
-- ---------------------------------------------------------------------
CREATE TABLE audit_log (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    timestamp   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    user_type   VARCHAR(32)  NOT NULL,
    user_id     UUID,
    action_type VARCHAR(64)  NOT NULL,
    details     TEXT
);

CREATE INDEX idx_audit_log_timestamp ON audit_log(timestamp);
CREATE INDEX idx_audit_log_action    ON audit_log(action_type);
CREATE INDEX idx_audit_log_user      ON audit_log(user_type, user_id);
