package com.preen.preen_backend.auth.service;

import com.preen.preen_backend.auth.dto.*;
import com.preen.preen_backend.auth.entity.*;
import com.preen.preen_backend.auth.exception.AuthErrorCode;
import com.preen.preen_backend.auth.repository.*;
import com.preen.preen_backend.common.exception.BusinessException;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 가입 조건과 이메일 인증 증명을 확인하고 회원 정보를 함께 저장합니다.
@Service
@RequiredArgsConstructor
public class SignupService {
    private static final String SIGNUP_TERMS_VERSION = "2026-11-16-draft-v1";

    private final UserRepository users;
    private final UserPersonalInfoRepository personalInfo;
    private final UserCredentialsRepository credentials;
    private final VerificationCodeRepository codes;
    private final TermRepository terms;
    private final UserTermAgreementRepository agreements;
    private final AuthHashService hashes;
    private final EmailCryptoService crypto;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    // 인증 증명과 약관 버전을 확인하고 회원·비밀번호·동의 내역을 하나의 트랜잭션으로 저장합니다.
    @Transactional
    public SignupResponse signup(SignupRequest request) {
        validate(request);
        String emailHash = hashes.hash(request.email());
        VerificationCode verification = codes.findForUpdate(emailHash)
                .orElseThrow(() -> new BusinessException(AuthErrorCode.SIGNUP_PROOF_INVALID));

        if (personalInfo.existsByEmailHash(emailHash)) {
            throw new BusinessException(AuthErrorCode.EMAIL_ALREADY_REGISTERED);
        }

        if (!verification.hasSignupProof(clock.instant())
                || !hashes.matches(emailHash + ":signup:" + request.signupToken(), verification.getSignupTokenHash())) {
            throw new BusinessException(AuthErrorCode.SIGNUP_PROOF_INVALID);
        }

        if (!SIGNUP_TERMS_VERSION.equals(request.termsVersion())) {
            throw new BusinessException(AuthErrorCode.TERMS_INVALID);
        }

        List<Term> currentTerms = terms.findByVersionAndEffectiveAtLessThanEqual(request.termsVersion(), clock.instant());
        Map<String, Boolean> consent = Map.of("SERVICE", request.termsAgreed(),
                "PRIVACY", request.privacyAgreed(), "MARKETING", request.marketingAgreed());

        if (currentTerms.size() != consent.size() || currentTerms.stream().anyMatch(term ->
                !consent.containsKey(term.getType()) || term.isRequired() && !consent.get(term.getType()))) {
            throw new BusinessException(AuthErrorCode.TERMS_INVALID);
        }
        User user = users.saveAndFlush(User.guest());
        try {
            personalInfo.saveAndFlush(UserPersonalInfo.create(user.getId(), request.name(),
                    crypto.encrypt(request.email()), emailHash, request.birthDate()));
        } catch (DataIntegrityViolationException exception) {
            throw new BusinessException(AuthErrorCode.EMAIL_ALREADY_REGISTERED);
        }
        credentials.save(UserCredentials.create(user.getId(), passwordEncoder.encode(request.password()), clock.instant()));
        agreements.saveAll(currentTerms.stream().map(term -> UserTermAgreement.create(
                user.getId(), term.getId(), consent.get(term.getType()), clock.instant())).toList());
        verification.consumeSignupProof();
        return SignupResponse.of(user);
    }

    // 비밀번호 구성과 일치 여부, 가입 가능 나이와 필수 동의를 확인합니다.
    private void validate(SignupRequest request) {
        if (!request.password().matches("(?Us)(?=.*[A-Za-z])(?=.*[0-9])(?=.*[^A-Za-z0-9\s])(?!.*\s).{8,50}")
                || !request.password().equals(request.passwordConfirmation())) {
            throw new BusinessException(AuthErrorCode.PASSWORD_INVALID);
        }
        LocalDate today = LocalDate.now(clock.withZone(java.time.ZoneId.of("Asia/Seoul")));
        if (request.birthDate().isAfter(today.minusYears(14))) {
            throw new BusinessException(AuthErrorCode.AGE_NOT_ALLOWED);
        }
        if (!Boolean.TRUE.equals(request.termsAgreed()) || !Boolean.TRUE.equals(request.privacyAgreed())) {
            throw new BusinessException(AuthErrorCode.TERMS_INVALID);
        }
    }
}
