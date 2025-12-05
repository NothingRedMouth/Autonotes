ALTER TABLE lecture_notes
ADD COLUMN deleted_at TIMESTAMPTZ;

CREATE INDEX idx_lecture_notes_deleted_at ON lecture_notes (deleted_at) WHERE deleted_at IS NOT NULL;
