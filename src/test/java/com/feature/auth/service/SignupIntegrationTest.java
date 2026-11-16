package com.feature.auth.service;

import com.feature.auth.dto.SignupRequest;
import com.feature.auth.exception.AuthErrorCode;
import com.feature.auth.repository.VerificationCodeRepository;
import com.feature.common.exception.BusinessException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@Tag("integration")
@SpringBootTest
@Transactional
class SignupIntegrationTest {
    @Autowired SignupService service;
    @Autowired AuthHashService hashes;
    @Autowired EmailCryptoService crypto;
    @Autowired PasswordEncoder encoder;
    @Autowired VerificationCodeRepository codes;
    @Autowired JdbcTemplate jdbc;
    private String email;
    private final String token = "test-proof-for-signup-integration";
    private final String password = "PreenTest123!";

    @BeforeEach
    void prepareProof() {
        email = "signup-test-" + UUID.randomUUID() + "@example.com";
        Instant now = Instant.now();
        String target = hashes.hash(email);
        jdbc.update("""
            INSERT INTO verification_codes
            (target_type,target_hash,purpose,code_hash,attempt_count,expires_at,verified_at,
             signup_token_hash,signup_token_expires_at,created_at)
            VALUES ('EMAIL',?,'SIGNUP',?,0,?,?,?,?,?)
            """, target, hashes.hash("test-code"), Timestamp.from(now.plusSeconds(300)),
                Timestamp.from(now), hashes.hash(target + ":signup:" + token),
                Timestamp.from(now.plusSeconds(900)), Timestamp.from(now));
    }

    private SignupRequest request(String proof, LocalDate birthDate, String confirmation) {
        return new SignupRequest(email, proof, "가입 테스트", birthDate,
                password, confirmation, true, true, false, "2026-11-16-draft-v1");
    }

    @Test
    void savesMemberEncryptedEmailPasswordAndAllAgreements() {
        var result = service.signup(request(token, LocalDate.of(1990, 1, 1), password));
        // 저장 계층을 실제로 flush한 뒤 DB 값과 인증 증명 소비를 확인합니다.
        codes.flush();
        assertEquals("GUEST", jdbc.queryForObject("SELECT role FROM users WHERE id=?", String.class, result.userId()));
        assertEquals("ACTIVE", result.status());
        String encrypted = jdbc.queryForObject("SELECT email FROM user_personal_info WHERE user_id=?", String.class, result.userId());
        assertNotEquals(email, encrypted);
        assertEquals(email, crypto.decrypt(encrypted));
        String passwordHash = jdbc.queryForObject("SELECT password_hash FROM user_credentials WHERE user_id=?", String.class, result.userId());
        assertNotEquals(password, passwordHash);
        assertTrue(encoder.matches(password, passwordHash));
        assertEquals(3L, jdbc.queryForObject("SELECT COUNT(*) FROM user_term_agreements WHERE user_id=?", Long.class, result.userId()));
        assertEquals(1L, jdbc.queryForObject("SELECT COUNT(*) FROM user_term_agreements a JOIN terms t ON a.term_id=t.id WHERE a.user_id=? AND t.type='MARKETING' AND a.is_agreed=FALSE", Long.class, result.userId()));
        assertEquals(3L, jdbc.queryForObject("SELECT COUNT(*) FROM user_term_agreements a JOIN terms t ON a.term_id=t.id WHERE a.user_id=? AND t.version='2026-11-16-draft-v1' AND LENGTH(t.content)>0", Long.class, result.userId()));
        assertFalse(codes.findForUpdate(hashes.hash(email)).orElseThrow().hasSignupProof(Instant.now()));
        var repeated = assertThrows(BusinessException.class,
                () -> service.signup(request(token, LocalDate.of(1990, 1, 1), password)));
        assertEquals(AuthErrorCode.EMAIL_ALREADY_REGISTERED, repeated.getErrorCode());
    }

    @Test
    void rejectsIncorrectProofWithoutCreatingMember() {
        long before = jdbc.queryForObject("SELECT COUNT(*) FROM users", Long.class);
        var failure = assertThrows(BusinessException.class,
                () -> service.signup(request("wrong-proof", LocalDate.of(1990, 1, 1), password)));
        assertEquals(AuthErrorCode.SIGNUP_PROOF_INVALID, failure.getErrorCode());
        assertEquals(before, jdbc.queryForObject("SELECT COUNT(*) FROM users", Long.class));
    }

    @Test
    void rejectsPreviousEmptyTermsVersionWithoutCreatingMember() {
        long before = jdbc.queryForObject("SELECT COUNT(*) FROM users", Long.class);
        var oldRequest = new SignupRequest(email, token, "가입 테스트", LocalDate.of(1990, 1, 1),
                password, password, true, true, false, "development-v1");
        var failure = assertThrows(BusinessException.class, () -> service.signup(oldRequest));
        assertEquals(AuthErrorCode.TERMS_INVALID, failure.getErrorCode());
        assertEquals(before, jdbc.queryForObject("SELECT COUNT(*) FROM users", Long.class));
        assertTrue(codes.findForUpdate(hashes.hash(email)).orElseThrow().hasSignupProof(Instant.now()));
    }

    @Test
    void rejectsExpiredProof() {
        jdbc.update("UPDATE verification_codes SET signup_token_expires_at=? WHERE target_hash=?",
                Timestamp.from(Instant.now().minusSeconds(1)), hashes.hash(email));
        var failure = assertThrows(BusinessException.class,
                () -> service.signup(request(token, LocalDate.of(1990, 1, 1), password)));
        assertEquals(AuthErrorCode.SIGNUP_PROOF_INVALID, failure.getErrorCode());
    }

    @Test
    void rejectsUnderFourteenAndPasswordMismatch() {
        var child = assertThrows(BusinessException.class,
                () -> service.signup(request(token, LocalDate.now().minusYears(13), password)));
        assertEquals(AuthErrorCode.AGE_NOT_ALLOWED, child.getErrorCode());
        var mismatch = assertThrows(BusinessException.class,
                () -> service.signup(request(token, LocalDate.of(1990, 1, 1), "WrongPassword123!")));
        assertEquals(AuthErrorCode.PASSWORD_INVALID, mismatch.getErrorCode());
    }
}
