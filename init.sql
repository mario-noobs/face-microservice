-- Create the backend database (default is already created via MYSQL_DATABASE)
-- This script creates additional schemas and initial setup

-- Ensure proper character set
ALTER DATABASE backend_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- The tables will be auto-created by JPA/Hibernate ddl-auto: update
-- This file is for any additional initialization if needed

-- Grant privileges
GRANT ALL PRIVILEGES ON backend_db.* TO 'root'@'%';
FLUSH PRIVILEGES;
