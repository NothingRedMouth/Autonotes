ALTER TABLE users
    ALTER COLUMN created_at SET DEFAULT now();

ALTER TABLE lecture_notes
    ALTER COLUMN created_at SET DEFAULT now(),
    ALTER COLUMN updated_at SET DEFAULT now();

CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_lecture_notes_updated_at
BEFORE UPDATE ON lecture_notes
FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();