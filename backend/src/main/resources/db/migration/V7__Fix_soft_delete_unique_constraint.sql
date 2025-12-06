ALTER TABLE lecture_notes DROP CONSTRAINT lecture_notes_file_storage_path_key;

CREATE UNIQUE INDEX idx_unique_file_storage_path_active
ON lecture_notes (file_storage_path)
WHERE deleted_at IS NULL;
