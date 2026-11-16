-- Latest supplied StayBook MVP ERD: local development candidate.
-- MySQL 8.0.16+ required for enforced CHECK constraints. Exact team version pending.
-- First-time setup ONLY. Do not run against existing or shared databases.
-- Stop at the FIRST error; do not use mysql --force or execute later statements manually.
-- MySQL DDL can commit partially. No DROP, ALTER, or data deletion is included.
-- SQL widths/nullability not specified in the ERD are provisional; see README_ERD.md.
CREATE DATABASE preen_local_moon_erd CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE preen_local_moon_erd;

CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    role VARCHAR(20) NOT NULL DEFAULT 'GUEST',

    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    deleted_at DATETIME NULL,

    CONSTRAINT chk_users_role
        CHECK (role IN ('GUEST', 'HOST', 'ADMIN')),




    CONSTRAINT chk_users_status
        CHECK (status IN ('ACTIVE', 'LOCKED', 'WITHDRAWN'))
);

CREATE TABLE verification_codes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    target_type VARCHAR(20) NOT NULL,
    target_hash CHAR(64) NOT NULL,
    purpose VARCHAR(20) NOT NULL,
    code_hash CHAR(64) NOT NULL,
    attempt_count SMALLINT UNSIGNED NOT NULL DEFAULT 0,
    expires_at DATETIME NOT NULL,
    verified_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_verification_target
    CHECK (target_type IN ('EMAIL', 'PHONE')),

    CONSTRAINT chk_verification_purpose
    CHECK (purpose IN ('SIGNUP', 'RESET'))
    );

CREATE TABLE terms (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    type VARCHAR(30) NOT NULL,
    version varchar(20) NOT NULL,
    content TEXT NOT NULL,
    required BOOLEAN NOT NULL DEFAULT TRUE,
    effective_at DATETIME NOT NULL,

    CONSTRAINT uq_terms_version
    UNIQUE (type, version)
);

CREATE TABLE regions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    parent_id BIGINT NULL,
    name VARCHAR(100) NOT NULL,
    level TINYINT UNSIGNED NOT NULL,

    CONSTRAINT fk_region_parent
    FOREIGN KEY (parent_id) REFERENCES regions(id)
);

CREATE TABLE amenities (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE accommodation_categories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    sort_order INT NOT NULL DEFAULT 0
);

CREATE TABLE login_attempts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NULL,
    login_id_hash CHAR(64),
    ip_address VARCHAR(45),
    result VARCHAR(20) NOT NULL,
    attempted_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_login_attempts_user
    FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE password_reset_tokens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token_hash CHAR(64) NOT NULL UNIQUE,
    expires_at DATETIME NOT NULL,
    used_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_reset_tokens_user
    FOREIGN KEY (user_id) REFERENCES users(id)
    );

CREATE TABLE user_term_agreements (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    term_id BIGINT NOT NULL,
    is_agreed BOOLEAN NOT NULL,
    agreed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_user_term
    UNIQUE (user_id, term_id),

    CONSTRAINT fk_agreements_user
    FOREIGN KEY (user_id) REFERENCES users(id),

    CONSTRAINT fk_agreements_term
    FOREIGN KEY (term_id) REFERENCES terms(id)
);

CREATE TABLE host_businesses (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    host_user_id BIGINT NOT NULL,
    business_name VARCHAR(100) NOT NULL,
    business_registration_no VARCHAR(20) NOT NULL UNIQUE,
    approval_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_host_business_user
    FOREIGN KEY (host_user_id) REFERENCES users(id),

    CONSTRAINT chk_host_approval
    CHECK (approval_status IN ( 'PENDING', 'APPROVED', 'REJECTED'))
);

CREATE TABLE user_credentials (
    user_id BIGINT PRIMARY KEY,
    password_hash VARCHAR(255) NOT NULL,
    password_changed_at DATETIME NULL,
    failed_login_count SMALLINT UNSIGNED NOT NULL DEFAULT 0,
    locked_until DATETIME NULL,
    CONSTRAINT fk_credentials_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE user_personal_info (
    user_id BIGINT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(512) NULL,
    email_hash CHAR(64) NULL UNIQUE,
    phone VARCHAR(512) NULL,
    phone_hash CHAR(64) NULL,
    birth_date DATE NOT NULL,
    CONSTRAINT fk_personal_info_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE user_social_accounts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    provider VARCHAR(20) NOT NULL,
    provider_user_id VARCHAR(255) NOT NULL,
    connected_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_social_account_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT uq_social_provider_user UNIQUE (provider, provider_user_id),
    CONSTRAINT chk_social_provider CHECK (provider IN ('KAKAO', 'NAVER'))
);

CREATE TABLE accommodations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    host_business_id BIGINT NOT NULL,
    region_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    address VARCHAR(255) NOT NULL,
    latitude DECIMAL(10, 7),
    longitude DECIMAL(10, 7),
    check_in_time  TIME NOT NULL,
    check_out_time TIME NOT NULL,
    listing_status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_accommodation_business
    FOREIGN KEY (host_business_id) REFERENCES host_businesses(id),

    CONSTRAINT fk_accommodation_region
    FOREIGN KEY (region_id) REFERENCES regions(id),

    CONSTRAINT fk_accommodation_category FOREIGN KEY (category_id) REFERENCES accommodation_categories(id),

    CONSTRAINT chk_accommodation_status
    CHECK (listing_status IN ('DRAFT', 'LISTED', 'HIDDEN'))

);

CREATE TABLE accommodation_images (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    accommodation_id BIGINT NOT NULL,
    image_url VARCHAR(500) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,

    CONSTRAINT fk_accommodation_images
        FOREIGN KEY (accommodation_id) REFERENCES accommodations(id)
);

CREATE TABLE accommodation_amenities (
    accommodation_id BIGINT NOT NULL,
    amenity_id BIGINT NOT NULL,

    PRIMARY KEY (accommodation_id, amenity_id),

    CONSTRAINT fk_amenity_accommodation
        FOREIGN KEY (accommodation_id) REFERENCES accommodations(id),

    CONSTRAINT fk_amenity
        FOREIGN KEY (amenity_id) REFERENCES amenities(id)
);

CREATE TABLE rooms (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    accommodation_id BIGINT NOT NULL,
    name VARCHAR(150) NOT NULL,
    capacity INT NOT NULL,
    max_capacity INT NOT NULL,
    extra_person_fee DECIMAL(12, 2) NOT NULL DEFAULT 0,
    nightly_price DECIMAL(12, 2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',

    CONSTRAINT fk_room_accommodation
    FOREIGN KEY (accommodation_id) REFERENCES accommodations(id),

    CONSTRAINT chk_room_capacity
    CHECK (capacity > 0 AND max_capacity >= capacity),

    CONSTRAINT chk_room_price
    CHECK (extra_person_fee >= 0 AND nightly_price >= 0)
);

CREATE TABLE refund_policies (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    accommodation_id BIGINT NOT NULL,
    version VARCHAR(20) NOT NULL,
    policy_text TEXT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT FALSE,

    CONSTRAINT fk_refund_policy_accommodation
    FOREIGN KEY (accommodation_id)
    REFERENCES accommodations(id),

    CONSTRAINT uq_refund_policy_version
    UNIQUE (accommodation_id, version)
);

CREATE TABLE favorites (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    accommodation_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_favorite_user_accommodation
    UNIQUE (user_id, accommodation_id),

    CONSTRAINT fk_favorite_user
    FOREIGN KEY (user_id)
    REFERENCES users(id),

    CONSTRAINT fk_favorite_accommodation
    FOREIGN KEY (accommodation_id)
    REFERENCES accommodations(id)
);

CREATE TABLE room_images (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    room_id BIGINT NOT NULL,
    image_url VARCHAR(500) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,

    CONSTRAINT fk_room_images_room
    FOREIGN KEY (room_id) REFERENCES rooms(id)
);

CREATE TABLE room_inventories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    room_id BIGINT NOT NULL,
    stay_date DATE NOT NULL,
    total_count SMALLINT UNSIGNED NOT NULL DEFAULT 0,
    booked_count SMALLINT UNSIGNED NOT NULL DEFAULT 0,
    held_count SMALLINT UNSIGNED NOT NULL DEFAULT 0,
    price DECIMAL(12, 2) NOT NULL,
    is_closed BOOLEAN NOT NULL DEFAULT FALSE,

    CONSTRAINT uq_room_inventory_date
    UNIQUE (room_id, stay_date),

    CONSTRAINT fk_inventory_room
    FOREIGN KEY (room_id) REFERENCES rooms(id),

    CONSTRAINT chk_inventory_counts
    CHECK (booked_count + held_count <= total_count),

    CONSTRAINT chk_inventory_price
    CHECK (price >= 0)
);

CREATE TABLE refund_policy_rules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    refund_policy_id BIGINT NOT NULL,
    days_before INT NOT NULL,
    refund_rate DECIMAL(5, 2) NOT NULL,

    CONSTRAINT fk_refund_rule_policy
    FOREIGN KEY (refund_policy_id)
    REFERENCES refund_policies(id),

    CONSTRAINT chk_refund_days
    CHECK (days_before >= 0),

    CONSTRAINT chk_refund_rate
    CHECK (refund_rate BETWEEN 0 AND 100)
);

CREATE TABLE reservations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    reservation_no VARCHAR(50) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    room_id BIGINT NOT NULL,
    refund_policy_id BIGINT NOT NULL,

    check_in DATE NOT NULL,
    check_out DATE NOT NULL,
    guest_count SMALLINT UNSIGNED NOT NULL,
    guest_name VARCHAR(100) NOT NULL,
    guest_phone VARCHAR(512),

    total_price DECIMAL(12, 2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    hold_expires_at DATETIME,
    refund_policy_snapshot JSON NOT NULL,
    cancelled_at DATETIME,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_reservation_user
    FOREIGN KEY (user_id) REFERENCES users(id),

    CONSTRAINT fk_reservation_room
    FOREIGN KEY (room_id) REFERENCES rooms(id),

    CONSTRAINT fk_reservationa_policy
    FOREIGN KEY (refund_policy_id) REFERENCES refund_policies(id),

    CONSTRAINT chk_reservation_dates
    CHECK (check_out > check_in),

    CONSTRAINT chk_reservation_guests
    CHECK (guest_count > 0),
    CONSTRAINT chk_reservation_price
    CHECK (total_price >= 0),

    CONSTRAINT chk_reservation_status
    CHECK (status IN ('PENDING', 'CONFIRMED', 'CANCELLED', 'COMPLETED'
    ))
);

CREATE TABLE inventory_holds (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    reservation_id BIGINT NOT NULL,
    room_inventory_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'HELD',
    expires_at DATETIME NOT NULL,

    CONSTRAINT uq_inventory_hold
    UNIQUE (reservation_id, room_inventory_id),

    CONSTRAINT fk_hold_reservation
    FOREIGN KEY (reservation_id) REFERENCES reservations(id),

    CONSTRAINT fk_hold_inventory
    FOREIGN KEY (room_inventory_id) REFERENCES room_inventories(id),

    CONSTRAINT chk_hold_status
    CHECK (status IN ( 'HELD', 'CONFIRMED', 'RELEASED'))
);

CREATE TABLE reservation_price_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    reservation_id BIGINT NOT NULL,
    item_type VARCHAR(20) NOT NULL,
    name VARCHAR(100) NOT NULL,
    amount DECIMAL(12, 2) NOT NULL,

    CONSTRAINT fk_price_item_reservation
    FOREIGN KEY (reservation_id)
    REFERENCES reservations(id),

    CONSTRAINT chk_price_item_type
    CHECK (item_type IN ('ROOM', 'EXTRA_PERSON')),

    CONSTRAINT chk_price_item_amount
    CHECK (amount >= 0)

);

CREATE TABLE reservation_status_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    reservation_id BIGINT NOT NULL,
    changed_by BIGINT NULL,
    previous_status VARCHAR(20),
    new_status VARCHAR(20) NOT NULL,
    reason TEXT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_status_history_reservation
    FOREIGN KEY (reservation_id)
    REFERENCES reservations(id),

    CONSTRAINT fk_status_history_user
    FOREIGN KEY (changed_by)
    REFERENCES users(id)
);

CREATE TABLE payments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    reservation_id BIGINT NOT NULL,
    order_id VARCHAR(100) NOT NULL UNIQUE,
    pg_provider VARCHAR(50) NOT NULL,
    payment_key VARCHAR(255) UNIQUE,
    amount DECIMAL(12, 2) NOT NULL,
    method VARCHAR(50),
    status VARCHAR(20) NOT NULL DEFAULT 'READY',
    paid_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_payment_reservation
    FOREIGN KEY (reservation_id)
    REFERENCES reservations(id),

    CONSTRAINT chk_payment_amount
    CHECK (amount >= 0),

    CONSTRAINT chk_payment_status
    CHECK (status IN ( 'READY', 'PAID', 'FAILED', 'REFUNDED'))
);

CREATE TABLE reviews (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    reservation_id BIGINT NOT NULL UNIQUE,
    accommodation_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    spotify_track_id VARCHAR(100) NOT NULL,
    rating TINYINT UNSIGNED NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'VISIBLE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_review_reservation FOREIGN KEY (reservation_id) REFERENCES reservations(id),
    CONSTRAINT fk_review_accommodation FOREIGN KEY (accommodation_id) REFERENCES accommodations(id),
    CONSTRAINT fk_review_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT chk_review_rating CHECK (rating BETWEEN 1 AND 5),
    CONSTRAINT chk_review_status CHECK (status IN ('VISIBLE', 'HIDDEN')),
    INDEX idx_reviews_accommodation_rating (accommodation_id, rating)
);

CREATE TABLE payment_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    payment_id BIGINT NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    provider_event_id VARCHAR(255) UNIQUE,
    response_code VARCHAR(50),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_payment_log_payment
    FOREIGN KEY (payment_id)
    REFERENCES payments(id)
);

CREATE TABLE refunds (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    payment_id BIGINT NOT NULL,
    amount DECIMAL(12, 2) NOT NULL,
    penalty_amount DECIMAL(12, 2) NOT NULL DEFAULT 0,
    reason VARCHAR(255),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    refunded_at DATETIME NUll,


    CONSTRAINT fk_refund_payment
    FOREIGN KEY (payment_id)
    REFERENCES payments(id),

    CONSTRAINT chk_refund_amount
    CHECK (amount >= 0 AND penalty_amount >= 0)
);
