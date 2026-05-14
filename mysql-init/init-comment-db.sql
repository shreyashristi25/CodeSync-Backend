CREATE DATABASE IF NOT EXISTS comment_db;
USE comment_db;

CREATE TABLE IF NOT EXISTS comments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    file_id BIGINT NOT NULL,
    line_number INT NOT NULL,
    content LONGTEXT NOT NULL,
    resolved BOOLEAN DEFAULT FALSE,
    author_id BIGINT NOT NULL,
    parent_comment_id BIGINT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (parent_comment_id) REFERENCES comments(id) ON DELETE CASCADE,
    INDEX idx_file_line (file_id, line_number),
    INDEX idx_parent (parent_comment_id),
    INDEX idx_author (author_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
