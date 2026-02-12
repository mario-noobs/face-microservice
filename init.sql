-- Ensure proper character set
ALTER DATABASE backend_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Grant privileges
GRANT ALL PRIVILEGES ON backend_db.* TO 'root'@'%';
FLUSH PRIVILEGES;

-- =============================================
-- RBAC Tables
-- =============================================

CREATE TABLE IF NOT EXISTS permissions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(200),
    service VARCHAR(30),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS roles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(200),
    is_default BIT(1) DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS role_permissions (
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, permission_id),
    FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE,
    FOREIGN KEY (permission_id) REFERENCES permissions(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- Seed Permissions
-- =============================================

INSERT INTO permissions (name, description, service) VALUES
-- Face service
('face:register', 'Register a face identity', 'face'),
('face:recognize', 'Recognize a face identity', 'face'),
('face:delete', 'Delete a face identity', 'face'),
('face:check', 'Check if face is registered', 'face'),
-- User service
('user:read_self', 'Read own profile', 'user'),
('user:update_self', 'Update own profile', 'user'),
('user:read_any', 'Read any user profile', 'user'),
('user:update_any', 'Update any user profile', 'user'),
('user:delete_any', 'Delete any user', 'user'),
('user:list', 'List all users', 'user'),
-- Audit service
('audit:read_self', 'Read own audit logs', 'audit'),
('audit:read_all', 'Read all audit logs', 'audit'),
-- RBAC management
('rbac:manage_roles', 'Create, update, delete roles', 'rbac'),
('rbac:assign_roles', 'Assign roles to users', 'rbac'),
('rbac:manage_permissions', 'Manage role permissions', 'rbac');

-- =============================================
-- Seed Roles
-- =============================================

INSERT INTO roles (name, description, is_default) VALUES
('SUPERADMIN', 'Full system access', 0),
('PREMIUM_USER', 'Premium user with full AI service access', 0),
('BASIC_USER', 'Basic user with limited access', 1);

-- =============================================
-- Seed Role-Permission Mappings
-- =============================================

-- SUPERADMIN: all permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.name = 'SUPERADMIN';

-- PREMIUM_USER permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.name = 'PREMIUM_USER'
  AND p.name IN (
    'user:read_self', 'user:update_self',
    'face:register', 'face:recognize', 'face:delete', 'face:check',
    'audit:read_self'
  );

-- BASIC_USER permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.name = 'BASIC_USER'
  AND p.name IN (
    'user:read_self', 'user:update_self',
    'face:check',
    'audit:read_self'
  );
