CREATE DATABASE IF NOT EXISTS `auth-db`;
CREATE DATABASE IF NOT EXISTS `user-db`;
CREATE DATABASE IF NOT EXISTS `face-db`;
CREATE DATABASE IF NOT EXISTS `ai-backend-service-db`;

CREATE TABLE IF NOT EXISTS `auth-db`.`auths` (
  `id` int NOT NULL AUTO_INCREMENT,
  `user_id` int NOT NULL,
  `auth_type` enum('email_password','gmail','facebook') DEFAULT 'email_password',
  `email` varchar(255) CHARACTER SET utf8mb4 NOT NULL,
  `salt` varchar(40) CHARACTER SET utf8mb4 DEFAULT NULL,
  `password` varchar(100) CHARACTER SET utf8mb4 DEFAULT NULL,
  `facebook_id` varchar(35) CHARACTER SET utf8mb4 DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `email` (`email`) USING BTREE,
  KEY `user_id` (`user_id`) USING BTREE,
  KEY `facebook_id` (`facebook_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `user-db`.`users` (
  `id` int NOT NULL AUTO_INCREMENT,
  `first_name` varchar(30) CHARACTER SET utf8mb4 NOT NULL,
  `last_name` varchar(30) CHARACTER SET utf8mb4 NOT NULL,
  `email` varchar(255) CHARACTER SET utf8mb4 NOT NULL,
  `phone` varchar(30) DEFAULT NULL,
  `avatar` json DEFAULT NULL,
  `gender` enum('male','female','unknown') DEFAULT 'unknown',
  `dob` date DEFAULT NULL,
  `system_role` enum('sadmin','admin','user') DEFAULT 'user',
  `status` enum('active','waiting_verify','banned') DEFAULT 'active',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `email` (`email`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `ai-backend-service-db`.`audit_logs` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `created_at` datetime(3) DEFAULT NULL,
  `updated_at` datetime(3) DEFAULT NULL,
  `deleted_at` datetime(3) DEFAULT NULL,
  `time` datetime(3) DEFAULT NULL,
  `ip_address` varchar(191) DEFAULT NULL,
  `api_call` varchar(191) DEFAULT NULL,
  `method` varchar(191) DEFAULT NULL,
  `status` bigint DEFAULT NULL,
  `response_time` bigint DEFAULT NULL,
  `user_id` varchar(191) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_audit_logs_deleted_at` (`deleted_at`),
  KEY `idx_audit_logs_user_id` (`user_id`),
  KEY `idx_audit_logs_time` (`time`),
  KEY `idx_audit_logs_ip_address` (`ip_address`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;