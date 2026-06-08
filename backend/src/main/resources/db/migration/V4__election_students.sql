-- =====================================================================
-- VoteVox - V4: Election Enrollment Ledger
-- =====================================================================
-- Records which students are enrolled (eligible) for a given election.
-- This is intentionally separate from `student_tokens`: a student is
-- enrolled BEFORE any token is issued, so the admin can build the roster
-- and only afterwards generate one-time tokens for the enrolled set.
--
-- Security note: like `student_tokens`, this table maps a student to an
-- election. The anonymous `votes` table still references neither, so
-- ballot secrecy is unaffected.
-- =====================================================================

CREATE TABLE election_students (
    id           UUID      PRIMARY KEY DEFAULT gen_random_uuid(),
    election_id  UUID      NOT NULL REFERENCES elections(id) ON DELETE CASCADE,
    student_id   UUID      NOT NULL REFERENCES students(id)  ON DELETE CASCADE,
    enrolled_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_election_student UNIQUE (election_id, student_id)
);

CREATE INDEX idx_election_students_election ON election_students(election_id);
CREATE INDEX idx_election_students_student  ON election_students(student_id);

-- Backfill: enroll the V3 sample students into the V3 sample election so the
-- demo data is immediately testable. The WHERE guard makes this a no-op if the
-- sample election was already deleted (no FK violation).
INSERT INTO election_students (election_id, student_id)
SELECT e.id, s.id
FROM elections e
CROSS JOIN students s
WHERE e.id = '11111111-1111-1111-1111-111111111111'
ON CONFLICT (election_id, student_id) DO NOTHING;