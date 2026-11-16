CREATE TABLE user_credentials (
    user_id BIGINT NOT NULL PRIMARY KEY,
    password_hash VARCHAR(255) NOT NULL,
    password_changed_at DATETIME(6) NOT NULL,
    failed_login_count SMALLINT NOT NULL DEFAULT 0,
    locked_until DATETIME(6),
    CONSTRAINT fk_credentials_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE terms (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    type VARCHAR(30) NOT NULL,
    version VARCHAR(50) NOT NULL,
    content TEXT NOT NULL,
    required BOOLEAN NOT NULL,
    effective_at DATETIME(6) NOT NULL,
    CONSTRAINT uk_terms_version UNIQUE (type, version)
);

CREATE TABLE user_term_agreements (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    term_id BIGINT NOT NULL,
    is_agreed BOOLEAN NOT NULL,
    agreed_at DATETIME(6) NOT NULL,
    CONSTRAINT uk_user_term UNIQUE (user_id, term_id),
    CONSTRAINT fk_agreement_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_agreement_term FOREIGN KEY (term_id) REFERENCES terms(id)
);

-- 약관 본문 미제공 상태의 개발 테스트 버전입니다.
-- 정식 약관은 별도 마이그레이션으로 새 버전을 추가합니다.
INSERT INTO terms (type, version, content, required, effective_at) VALUES
('SERVICE', 'development-v1', '', TRUE, '2026-01-01 00:00:00'),
('PRIVACY', 'development-v1', '', TRUE, '2026-01-01 00:00:00'),
('MARKETING', 'development-v1', '', FALSE, '2026-01-01 00:00:00');
