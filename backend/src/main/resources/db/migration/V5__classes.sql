-- =====================================================================
-- VoteVox - V5: Classes replace named students
-- =====================================================================
-- The admin no longer manages individual, named students. Instead a global
-- list of classes (name + student count) is maintained, and each election
-- selects which classes participate. Tokens are minted per class with NO
-- reference to any person, strengthening voter anonymity.
--
-- This drops the students / election_students / student_tokens tables and
-- introduces classes / election_classes / election_tokens.
-- =====================================================================

-- ---------------------------------------------------------------------
-- classes (global, reusable across elections)
-- ---------------------------------------------------------------------
CREATE TABLE classes (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    name          VARCHAR(64)  NOT NULL UNIQUE,
    student_count INTEGER      NOT NULL DEFAULT 0 CHECK (student_count >= 0),
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_classes_name ON classes(name);

-- ---------------------------------------------------------------------
-- election_classes (which classes take part in an election)
-- ---------------------------------------------------------------------
CREATE TABLE election_classes (
    id           UUID      PRIMARY KEY DEFAULT gen_random_uuid(),
    election_id  UUID      NOT NULL REFERENCES elections(id) ON DELETE CASCADE,
    class_id     UUID      NOT NULL REFERENCES classes(id)   ON DELETE CASCADE,
    created_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_election_class UNIQUE (election_id, class_id)
);

CREATE INDEX idx_election_classes_election ON election_classes(election_id);
CREATE INDEX idx_election_classes_class    ON election_classes(class_id);

-- ---------------------------------------------------------------------
-- election_tokens (issuance ledger - anonymous token per election + class)
-- Replaces student_tokens. There is intentionally NO student reference:
-- a token belongs to an election and (for grouping the printed sheet and
-- participation counts) a class, but never a person.
-- ---------------------------------------------------------------------
CREATE TABLE election_tokens (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    token_id     UUID NOT NULL UNIQUE REFERENCES tokens(id)   ON DELETE CASCADE,
    election_id  UUID NOT NULL        REFERENCES elections(id) ON DELETE CASCADE,
    class_id     UUID                 REFERENCES classes(id)   ON DELETE SET NULL
);

CREATE INDEX idx_election_tokens_election ON election_tokens(election_id);
CREATE INDEX idx_election_tokens_class    ON election_tokens(class_id);

-- ---------------------------------------------------------------------
-- Drop the old student-centric tables (order respects FKs).
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS student_tokens;
DROP TABLE IF EXISTS election_students;
DROP TABLE IF EXISTS students;

-- ---------------------------------------------------------------------
-- Sample classes for quick testing.
-- ---------------------------------------------------------------------
INSERT INTO classes (name, student_count) VALUES
    ('4AHIT', 25),
    ('4BHIT', 24),
    ('4AHWI', 22)
ON CONFLICT (name) DO NOTHING;
