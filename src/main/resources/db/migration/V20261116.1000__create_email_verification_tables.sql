CREATE TABLE users (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    role VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    deleted_at DATETIME(6)
);

CREATE TABLE user_personal_info (
    user_id BIGINT NOT NULL PRIMARY KEY,
    name VARCHAR(100),
    email VARCHAR(1024),
    email_hash CHAR(64),
    phone VARCHAR(1024),
    phone_hash CHAR(64),
    birth_date DATE,
    CONSTRAINT uk_personal_email UNIQUE (email_hash),
    CONSTRAINT fk_personal_user FOREIGN KEY (user_id) REFERENCES users(id)
);

-- 대상별 최신 코드 한 건을 유지하여 동시 발송 요청도 같은 행에서 잠급니다.
CREATE TABLE verification_codes (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    target_type VARCHAR(20) NOT NULL,
    target_hash CHAR(64) NOT NULL,
    purpose VARCHAR(20) NOT NULL,
    code_hash CHAR(64) NOT NULL,
    attempt_count SMALLINT NOT NULL DEFAULT 0,
    expires_at DATETIME(6) NOT NULL,
    verified_at DATETIME(6),
    signup_token_hash CHAR(64),
    signup_token_expires_at DATETIME(6),
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT uk_verification_target UNIQUE (target_type, target_hash, purpose)
);
