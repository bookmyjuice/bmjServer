-- ============================================================
-- Migration V2: Delivery Domain
-- BookMyJuice Enterprise — Delivery domain ownership
-- ============================================================

-- Service Areas: Pincode-based serviceability
CREATE TABLE IF NOT EXISTS service_areas (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    pincode VARCHAR(10) NOT NULL,
    city VARCHAR(100) NOT NULL,
    state VARCHAR(100) NOT NULL,
    is_serviced BOOLEAN NOT NULL DEFAULT TRUE,
    cutoff_time TIME NOT NULL DEFAULT '21:00:00', -- 9 PM IST default cutoff
    min_lead_hours INT NOT NULL DEFAULT 24,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_pincode (pincode),
    INDEX idx_city (city),
    INDEX idx_state (state),
    INDEX idx_serviced (is_serviced)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Delivery Slots: Available delivery windows
CREATE TABLE IF NOT EXISTS delivery_slots (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    service_area_id BIGINT NOT NULL,
    slot_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    max_orders INT NOT NULL DEFAULT 50,
    current_orders INT NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (service_area_id) REFERENCES service_areas(id) ON DELETE CASCADE,
    INDEX idx_slot_date (slot_date),
    INDEX idx_area_date (service_area_id, slot_date),
    INDEX idx_active (is_active),
    CONSTRAINT chk_orders CHECK (current_orders <= max_orders)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- User Addresses: Multi-address management
CREATE TABLE IF NOT EXISTS user_addresses (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    label VARCHAR(50) NOT NULL DEFAULT 'Home', -- Home, Work, Other
    full_name VARCHAR(100) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    address_line1 VARCHAR(255) NOT NULL,
    address_line2 VARCHAR(255),
    landmark VARCHAR(255),
    city VARCHAR(100) NOT NULL,
    state VARCHAR(100) NOT NULL,
    pincode VARCHAR(10) NOT NULL,
    latitude DECIMAL(10, 8),
    longitude DECIMAL(11, 8),
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    delivery_instructions TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user (user_id),
    INDEX idx_default (user_id, is_default),
    INDEX idx_pincode (pincode)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
