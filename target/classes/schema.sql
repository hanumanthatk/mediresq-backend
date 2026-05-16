-- =====================================================
-- Smart Emergency Medical Response System
-- Complete Database Schema v1.0
-- =====================================================

CREATE DATABASE IF NOT EXISTS smart_emergency_db;
USE smart_emergency_db;

-- =====================================================
-- TABLE: users
-- Stores all system users (patients, hospital staff, admins)
-- =====================================================
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    phone VARCHAR(20) NOT NULL,
    password VARCHAR(255) NOT NULL,
    role ENUM('PATIENT', 'HOSPITAL', 'ADMIN') NOT NULL DEFAULT 'PATIENT',
    is_active BOOLEAN DEFAULT TRUE,
    is_verified BOOLEAN DEFAULT FALSE,
    profile_image VARCHAR(500),
    address TEXT,
    date_of_birth DATE,
    blood_group VARCHAR(10),
    emergency_contact VARCHAR(20),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_email (email),
    INDEX idx_role (role),
    INDEX idx_active (is_active)
);

-- =====================================================
-- TABLE: hospitals
-- Stores hospital information
-- =====================================================
CREATE TABLE IF NOT EXISTS hospitals (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    registration_number VARCHAR(100) NOT NULL UNIQUE,
    type ENUM('GOVERNMENT', 'PRIVATE', 'SEMI_GOVERNMENT', 'CHARITABLE') NOT NULL,
    specialization VARCHAR(500),
    address TEXT NOT NULL,
    city VARCHAR(100) NOT NULL,
    state VARCHAR(100) NOT NULL,
    pincode VARCHAR(10) NOT NULL,
    latitude DECIMAL(10, 8),
    longitude DECIMAL(11, 8),
    phone VARCHAR(20) NOT NULL,
    emergency_phone VARCHAR(20),
    email VARCHAR(255),
    website VARCHAR(500),
    total_beds INT DEFAULT 0,
    total_icu_beds INT DEFAULT 0,
    is_verified BOOLEAN DEFAULT FALSE,
    is_active BOOLEAN DEFAULT TRUE,
    rating DECIMAL(3, 2) DEFAULT 0.00,
    established_year INT,
    description TEXT,
    image_url VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_city (city),
    INDEX idx_state (state),
    INDEX idx_location (latitude, longitude),
    INDEX idx_verified (is_verified)
);

-- =====================================================
-- TABLE: beds
-- Tracks real-time bed availability per hospital
-- =====================================================
CREATE TABLE IF NOT EXISTS beds (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    hospital_id BIGINT NOT NULL,
    bed_type ENUM('GENERAL', 'ICU', 'CCU', 'NICU', 'HDU', 'ISOLATION', 'EMERGENCY') NOT NULL,
    total_count INT DEFAULT 0,
    available_count INT DEFAULT 0,
    occupied_count INT DEFAULT 0,
    under_maintenance INT DEFAULT 0,
    cost_per_day DECIMAL(10, 2) DEFAULT 0.00,
    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    notes TEXT,
    FOREIGN KEY (hospital_id) REFERENCES hospitals(id) ON DELETE CASCADE,
    UNIQUE KEY uq_hospital_bed_type (hospital_id, bed_type),
    INDEX idx_bed_type (bed_type),
    INDEX idx_available (available_count)
);

-- =====================================================
-- TABLE: ambulances
-- Manages ambulance fleet per hospital
-- =====================================================
CREATE TABLE IF NOT EXISTS ambulances (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    hospital_id BIGINT NOT NULL,
    vehicle_number VARCHAR(50) NOT NULL UNIQUE,
    ambulance_type ENUM('BASIC', 'ADVANCED', 'NEONATAL', 'AIR', 'MOBILE_ICU') NOT NULL,
    driver_name VARCHAR(100),
    driver_phone VARCHAR(20),
    paramedic_name VARCHAR(100),
    paramedic_phone VARCHAR(20),
    status ENUM('AVAILABLE', 'DISPATCHED', 'ON_DUTY', 'MAINTENANCE', 'OUT_OF_SERVICE') NOT NULL DEFAULT 'AVAILABLE',
    current_latitude DECIMAL(10, 8),
    current_longitude DECIMAL(11, 8),
    equipment_list TEXT,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (hospital_id) REFERENCES hospitals(id) ON DELETE CASCADE,
    INDEX idx_status (status),
    INDEX idx_hospital_id (hospital_id)
);

-- =====================================================
-- TABLE: emergency_requests
-- Central table for all emergency and ambulance requests
-- =====================================================
CREATE TABLE IF NOT EXISTS emergency_requests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    request_number VARCHAR(50) NOT NULL UNIQUE,
    patient_id BIGINT NOT NULL,
    hospital_id BIGINT,
    ambulance_id BIGINT,
    request_type ENUM('SOS', 'AMBULANCE', 'BED_RESERVATION', 'CONSULTATION', 'TRANSFER') NOT NULL,
    priority ENUM('CRITICAL', 'HIGH', 'MEDIUM', 'LOW') NOT NULL DEFAULT 'HIGH',
    status ENUM('PENDING', 'ACCEPTED', 'DISPATCHED', 'EN_ROUTE', 'ARRIVED', 'IN_TREATMENT', 'COMPLETED', 'CANCELLED', 'REJECTED') NOT NULL DEFAULT 'PENDING',
    patient_condition TEXT,
    symptoms TEXT,
    patient_latitude DECIMAL(10, 8),
    patient_longitude DECIMAL(11, 8),
    patient_address TEXT,
    notes TEXT,
    rejection_reason TEXT,
    estimated_arrival_minutes INT,
    actual_arrival_time TIMESTAMP,
    completed_at TIMESTAMP,
    is_emergency BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (patient_id) REFERENCES users(id),
    FOREIGN KEY (hospital_id) REFERENCES hospitals(id),
    FOREIGN KEY (ambulance_id) REFERENCES ambulances(id),
    INDEX idx_patient_id (patient_id),
    INDEX idx_hospital_id (hospital_id),
    INDEX idx_status (status),
    INDEX idx_request_type (request_type),
    INDEX idx_priority (priority),
    INDEX idx_created_at (created_at)
);

-- =====================================================
-- TABLE: notifications
-- System notifications for all users
-- =====================================================
CREATE TABLE IF NOT EXISTS notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    type ENUM('INFO', 'SUCCESS', 'WARNING', 'ERROR', 'EMERGENCY', 'SYSTEM') NOT NULL DEFAULT 'INFO',
    is_read BOOLEAN DEFAULT FALSE,
    related_request_id BIGINT,
    action_url VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_is_read (is_read),
    INDEX idx_created_at (created_at)
);

-- =====================================================
-- TABLE: doctors
-- Hospital medical staff management
-- =====================================================
CREATE TABLE IF NOT EXISTS doctors (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    hospital_id BIGINT NOT NULL,
    name VARCHAR(200) NOT NULL,
    specialization VARCHAR(200) NOT NULL,
    qualification VARCHAR(500),
    registration_number VARCHAR(100),
    phone VARCHAR(20),
    email VARCHAR(255),
    is_available BOOLEAN DEFAULT TRUE,
    shift ENUM('MORNING', 'AFTERNOON', 'NIGHT', 'ON_CALL') DEFAULT 'MORNING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (hospital_id) REFERENCES hospitals(id) ON DELETE CASCADE,
    INDEX idx_hospital_id (hospital_id),
    INDEX idx_specialization (specialization)
);

-- =====================================================
-- TABLE: request_status_history
-- Tracks status change history for audit trail
-- =====================================================
CREATE TABLE IF NOT EXISTS request_status_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    request_id BIGINT NOT NULL,
    old_status VARCHAR(50),
    new_status VARCHAR(50) NOT NULL,
    changed_by BIGINT,
    notes TEXT,
    changed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (request_id) REFERENCES emergency_requests(id) ON DELETE CASCADE,
    INDEX idx_request_id (request_id)
);

-- =====================================================
-- DEFAULT ADMIN USER (Password: Admin@123)
-- BCrypt hash for "Admin@123"
-- =====================================================
INSERT IGNORE INTO users (first_name, last_name, email, phone, password, role, is_active, is_verified)
VALUES ('System', 'Admin', 'admin@smartemergency.com', '9999999999',
        '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj/RK.s5uKDu',
        'ADMIN', TRUE, TRUE);

-- =====================================================
-- SAMPLE HOSPITAL DATA
-- =====================================================
INSERT IGNORE INTO users (first_name, last_name, email, phone, password, role, is_active, is_verified)
VALUES ('City', 'Hospital', 'cityhosp@smartemergency.com', '9876543210',
        '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj/RK.s5uKDu',
        'HOSPITAL', TRUE, TRUE);

INSERT IGNORE INTO hospitals (user_id, name, registration_number, type, address, city, state, pincode,
    latitude, longitude, phone, emergency_phone, email, total_beds, total_icu_beds, is_verified, is_active)
VALUES (2, 'City General Hospital', 'HOS2024001', 'GOVERNMENT',
    '123 Medical Street, Koramangala', 'Bengaluru', 'Karnataka', '560034',
    12.9352, 77.6245, '080-12345678', '080-99999999', 'cityhosp@smartemergency.com',
    200, 30, TRUE, TRUE);

-- Sample beds for hospital
INSERT IGNORE INTO beds (hospital_id, bed_type, total_count, available_count, occupied_count, cost_per_day)
VALUES
    (1, 'GENERAL', 150, 45, 105, 500.00),
    (1, 'ICU', 20, 5, 15, 5000.00),
    (1, 'CCU', 10, 3, 7, 6000.00),
    (1, 'EMERGENCY', 20, 8, 12, 1000.00);

-- Sample ambulance
INSERT IGNORE INTO ambulances (hospital_id, vehicle_number, ambulance_type, driver_name, driver_phone,
    paramedic_name, status)
VALUES (1, 'KA01-AB-1234', 'ADVANCED', 'Raju Kumar', '9988776655', 'Dr. Priya', 'AVAILABLE');
