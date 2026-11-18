package com.preen.preen_backend.auth.service;

import lombok.RequiredArgsConstructor;
import com.preen.preen_backend.auth.config.AuthProperties;
import com.preen.preen_backend.auth.dto.*;
import com.preen.preen_backend.auth.entity.VerificationCode;
import com.preen.preen_backend.auth.event.VerificationEmailEvent;
import com.preen.preen_backend.auth.exception.*;
import com.preen.preen_backend.auth.repository.*;
import com.preen.preen_backend.common.exception.BusinessException;
import com.preen.preen_backend.common.util.TokenUtil;
import java.time.Clock;
import java.time.Instant;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 이메일 인증번호 발급·검증과 가입 인증 증명을 관리합니다.
@Service
@RequiredArgsConstructor
public class EmailVerificationService {
    private final VerificationCodeRepository codes;
    private final UserPersonalInfoRepository users;
    private final AuthHashService hashes;
    private final AuthProperties properties;
    private final ApplicationEventPublisher events;
    private final Clock clock;


    // 중복 가입과 재발송 간격을 확인한 뒤 인증번호를 저장하고 발송 이벤트를 발생시킵니다.
    @Transactional
    public SendEmailResponse send(SendEmailRequest request) {
        String targetHash = hashes.hash(request.email());
        ensureAvailable(targetHash);
        Instant now = clock.instant();
        codes.ensureTarget(targetHash, now);
        VerificationCode stored = codes.findForUpdate(targetHash).orElseThrow();
        // 빈 해시는 최초 요청의 잠금용 행입니다.
        if (!stored.getCodeHash().isEmpty()) {
            Instant issuedAt = stored.getExpiresAt().minus(properties.codeExpiration());
            if (issuedAt.plus(properties.resendInterval()).isAfter(now)) {
                throw new BusinessException(AuthErrorCode.RESEND_TOO_SOON);
            }
        }
        // 인증번호는 해시로 저장하고 원문은 메일 발송에만 사용합니다.
        String code = TokenUtil.verificationCode();
        stored.issue(hashes.hash(targetHash + ":code:" + code), now.plus(properties.codeExpiration()));
        codes.saveAndFlush(stored);
        events.publishEvent(new VerificationEmailEvent(request.email(), code));
        return SendEmailResponse.of(stored, properties.resendInterval().toSeconds());
    }

    // 인증번호와 만료·시도 횟수를 검사하고 가입에 사용할 일회성 증명을 발급합니다.
    @Transactional(noRollbackFor = InvalidVerificationCodeException.class)
    public VerifyEmailResponse verify(VerifyEmailRequest request) {
        String targetHash = hashes.hash(request.email());
        ensureAvailable(targetHash);
        VerificationCode stored = codes.findForUpdate(targetHash)
                .orElseThrow(() -> new BusinessException(AuthErrorCode.CODE_EXPIRED));
        Instant now = clock.instant();
        if (!stored.getId().equals(request.verificationId())) {
            throw new BusinessException(AuthErrorCode.CODE_INVALID);
        }
        if (stored.getVerifiedAt() != null) {
            throw new BusinessException(AuthErrorCode.CODE_ALREADY_VERIFIED);
        }
        if (!stored.getExpiresAt().isAfter(now)) {
            throw new BusinessException(AuthErrorCode.CODE_EXPIRED);
        }
        if (stored.getAttemptCount() >= properties.maxAttempts()) {
            throw new BusinessException(AuthErrorCode.CODE_ATTEMPTS_EXCEEDED);
        }
        if (!hashes.matches(targetHash + ":code:" + request.code(), stored.getCodeHash())) {
            stored.fail();
            AuthErrorCode error = stored.getAttemptCount() >= properties.maxAttempts()
                    ? AuthErrorCode.CODE_ATTEMPTS_EXCEEDED : AuthErrorCode.CODE_INVALID;
            throw new InvalidVerificationCodeException(error);
        }
        // 인증 성공 후 로그인 JWT와 별개인 가입용 증명을 발급합니다.
        String token = TokenUtil.randomToken();
        Instant expiry = now.plus(properties.signupTokenExpiration());
        stored.verify(hashes.hash(targetHash + ":signup:" + token), now, expiry);
        return new VerifyEmailResponse(token, expiry);
    }

    // 이미 가입한 이메일인지 확인합니다.
    private void ensureAvailable(String targetHash) {
        if (users.existsByEmailHash(targetHash)) {
            throw new BusinessException(AuthErrorCode.EMAIL_ALREADY_REGISTERED);
        }
    }
}
