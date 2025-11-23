CREATE TABLE users
(
    id            BIGSERIAL PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL UNIQUE,
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL
);

CREATE TABLE lecture_notes
(
    id                   BIGSERIAL PRIMARY KEY,
    user_id              BIGINT       NOT NULL,
    title                VARCHAR(255),
    original_file_name   VARCHAR(255) NOT NULL,
    file_storage_path    VARCHAR(1024) NOT NULL UNIQUE,
    status               VARCHAR(20)  NOT NULL,
    recognized_text      TEXT,
    summary_text         TEXT,
    created_at           TIMESTAMPTZ  NOT NULL,
    updated_at           TIMESTAMPTZ  NOT NULL,

    CONSTRAINT fk_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT check_status CHECK (status IN ('PROCESSING', 'COMPLETED', 'FAILED'))
);

CREATE INDEX idx_lecture_notes_user_id ON lecture_notes (user_id);
