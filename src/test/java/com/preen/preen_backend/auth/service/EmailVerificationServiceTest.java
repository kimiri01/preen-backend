package com.preen.preen_backend.auth.service;

import com.preen.preen_backend.auth.config.AuthProperties;
import com.preen.preen_backend.auth.dto.SendEmailRequest;
import com.preen.preen_backend.auth.dto.VerifyEmailRequest;
import com.preen.preen_backend.auth.entity.VerificationCode;
import com.preen.preen_backend.auth.event.VerificationEmailEvent;
import com.preen.preen_backend.auth.exception.AuthErrorCode;
import com.preen.preen_backend.auth.exception.InvalidVerificationCodeException;
import com.preen.preen_backend.auth.repository.UserPersonalInfoRepository;
import com.preen.preen_backend.auth.repository.VerificationCodeRepository;
import com.preen.preen_backend.common.exception.BusinessException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.BeanUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EmailVerificationServiceTest {
    private final VerificationCodeRepository codes = mock(VerificationCodeRepository.class);
    private final UserPersonalInfoRepository users = mock(UserPersonalInfoRepository.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final Instant now = Instant.parse("2026-11-16T03:00:00Z");
    private final AuthProperties properties = new AuthProperties(Duration.ofMinutes(5),
            Duration.ofSeconds(60), 5, Duration.ofMinutes(15),
            "test-only-secret-at-least-32-characters", "sender@example.com");
    private final AuthHashService hashes = new AuthHashService(properties);
    private EmailVerificationService service;
    private VerificationCode stored;
    private String targetHash;

    @BeforeEach
    void setUp() {
        service = new EmailVerificationService(codes, users, hashes, properties,
                events, Clock.fixed(now, ZoneOffset.UTC));
        stored = BeanUtils.instantiateClass(VerificationCode.class);
        ReflectionTestUtils.setField(stored, "id", 1L);
        stored.issue("", now);
        targetHash = hashes.hash("guest@example.com");
        when(codes.findForUpdate(targetHash)).thenReturn(Optional.of(stored));
    }

    @Test
    void sendsSixDigitsAndStoresOnlyHash() {
        var response = service.send(new SendEmailRequest(" GUEST@example.com "));
        var captured = ArgumentCaptor.forClass(VerificationEmailEvent.class);
        verify(events).publishEvent(captured.capture());
        String code = captured.getValue().code();
        assertTrue(code.matches("[0-9]{6}"));
        assertTrue(hashes.matches(targetHash + ":code:" + code, stored.getCodeHash()));
        assertEquals(now.plusSeconds(300), response.expiresAt());
        assertFalse(captured.getValue().toString().contains(code));
    }

    @Test
    void rejectsDuplicateAccountBeforeSending() {
        when(users.existsByEmailHash(targetHash)).thenReturn(true);
        var exception = assertThrows(BusinessException.class,
                () -> service.send(new SendEmailRequest("guest@example.com")));
        assertEquals(AuthErrorCode.EMAIL_ALREADY_REGISTERED, exception.getErrorCode());
        verifyNoInteractions(events);
    }

    @Test
    void rejectsResendDuringCooldown() {
        stored.issue("previous-hash", now.plusSeconds(280));
        var exception = assertThrows(BusinessException.class,
                () -> service.send(new SendEmailRequest("guest@example.com")));
        assertEquals(AuthErrorCode.RESEND_TOO_SOON, exception.getErrorCode());
        verifyNoInteractions(events);
    }

    @Test
    void fifthIncorrectAttemptLocksCode() {
        stored.issue(hashes.hash(targetHash + ":code:123456"), now.plusSeconds(300));
        for (int i = 0; i < 4; i++) stored.fail();
        var exception = assertThrows(InvalidVerificationCodeException.class,
                () -> service.verify(new VerifyEmailRequest("guest@example.com", 1L, "999999")));
        assertEquals(5, stored.getAttemptCount());
        assertEquals(AuthErrorCode.CODE_ATTEMPTS_EXCEEDED, exception.getErrorCode());
    }

    @Test
    void rejectsExpiredCode() {
        stored.issue(hashes.hash(targetHash + ":code:123456"), now);
        var exception = assertThrows(BusinessException.class,
                () -> service.verify(new VerifyEmailRequest("guest@example.com", 1L, "123456")));
        assertEquals(AuthErrorCode.CODE_EXPIRED, exception.getErrorCode());
    }

    @Test
    void verificationIssuesSignupProofAndCannotReplay() {
        stored.issue(hashes.hash(targetHash + ":code:123456"), now.plusSeconds(300));
        var request = new VerifyEmailRequest("guest@example.com", 1L, "123456");
        var response = service.verify(request);
        assertEquals(now, stored.getVerifiedAt());
        assertTrue(hashes.matches(targetHash + ":signup:" + response.signupToken(), stored.getSignupTokenHash()));
        assertEquals(now.plusSeconds(900), response.expiresAt());
        var exception = assertThrows(BusinessException.class, () -> service.verify(request));
        assertEquals(AuthErrorCode.CODE_ALREADY_VERIFIED, exception.getErrorCode());
    }
}
