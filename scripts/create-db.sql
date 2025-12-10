-- Script: create-db.sql
-- Usage: used by run-create-db.ps1 or run manually in MySQL client
-- Adjust user/password/dbname as needed

CREATE DATABASE IF NOT EXISTS `bankinc_db` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'bankinc_user'@'localhost' IDENTIFIED BY 'change_me_password';
GRANT ALL PRIVILEGES ON `bankinc_db`.* TO 'bankinc_user'@'localhost';
FLUSH PRIVILEGES;
