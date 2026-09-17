SET search_path TO studypilot, public;

-- Keeping archived courses preserves their documents, question history and mock exams.
ALTER TABLE projects ADD COLUMN IF NOT EXISTS archived_at TIMESTAMPTZ;
CREATE INDEX IF NOT EXISTS idx_projects_active_updated
  ON projects (updated_at DESC, id DESC) WHERE archived_at IS NULL;
