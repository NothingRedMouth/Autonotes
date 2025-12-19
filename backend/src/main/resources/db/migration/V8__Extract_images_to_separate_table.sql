CREATE TABLE note_images
(
    id                 BIGSERIAL PRIMARY KEY,
    note_id            BIGINT        NOT NULL,
    file_storage_path  VARCHAR(1024) NOT NULL,
    original_file_name VARCHAR(255)  NOT NULL,
    order_index        INT           NOT NULL DEFAULT 0,
    created_at         TIMESTAMPTZ   NOT NULL DEFAULT now(),

    CONSTRAINT fk_note_images_note FOREIGN KEY (note_id) REFERENCES lecture_notes (id) ON DELETE CASCADE
);

CREATE INDEX idx_note_images_note_id ON note_images (note_id);

INSERT INTO note_images (note_id, file_storage_path, original_file_name, order_index, created_at)
SELECT id, file_storage_path, original_file_name, 0, created_at
FROM lecture_notes
WHERE file_storage_path IS NOT NULL;

ALTER TABLE lecture_notes
DROP COLUMN original_file_name,
    DROP COLUMN file_storage_path;
