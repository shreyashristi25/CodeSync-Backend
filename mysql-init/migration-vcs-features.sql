-- Migration script for version-service GitHub-like features
-- Run this SQL manually after updating the codebase

-- Add new columns to snapshots table (backward compatible)
-- Note: Run each ALTER separately if column may already exist
-- ALTER TABLE snapshots ADD COLUMN commit_message VARCHAR(500);
-- ALTER TABLE snapshots ADD COLUMN author_id BIGINT;
-- ALTER TABLE snapshots ADD COLUMN branch_id BIGINT;

-- Add index for branch queries (run if needed)
-- ALTER TABLE snapshots ADD INDEX idx_branch (branch_id);

-- Create branches table
CREATE TABLE IF NOT EXISTS branches (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    project_id BIGINT NOT NULL,
    latest_snapshot_hash VARCHAR(64),
    is_default BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY idx_project_name (project_id, name),
    INDEX idx_project (project_id)
);

-- Example: Create default main branch for an existing project
-- INSERT INTO branches (name, project_id, is_default, created_at)
-- VALUES ('main', 1, TRUE, NOW());