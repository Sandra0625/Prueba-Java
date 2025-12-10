-- Flyway migration: create users table and link cards to users (MariaDB)
-- This script uses IF NOT EXISTS clauses supported by MariaDB to be safe for repeated runs.

CREATE TABLE IF NOT EXISTS `users` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `created_at` DATETIME(6) NULL,
  `password` VARCHAR(255) NOT NULL,
  `roles` VARCHAR(255) NOT NULL,
  `username` VARCHAR(255) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_users_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Add user_id column to cards if it does not exist
ALTER TABLE `cards` ADD COLUMN IF NOT EXISTS `user_id` BIGINT NULL;

-- Add foreign key if not exists (MariaDB does not have a simple IF NOT EXISTS for constraints,
-- so attempt to add only if there's no existing constraint with the chosen name).
-- The following block checks information_schema and adds the constraint dynamically if missing.

SET @constraint_name := 'fk_cards_user';
SET @exists := (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
                 WHERE CONSTRAINT_SCHEMA = DATABASE()
                   AND TABLE_NAME = 'cards'
                   AND CONSTRAINT_NAME = @constraint_name);

-- If constraint does not exist, add it
-- Use prepared statement to avoid syntax issues when the IF branch is not executed

SET @sql := IF(@exists = 0,
  CONCAT('ALTER TABLE `cards` ADD CONSTRAINT ', @constraint_name, ' FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE SET NULL ON UPDATE CASCADE'),
  'SELECT 1');

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- End of migration
