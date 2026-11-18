# 회원가입 구현 가이드

## 읽는 순서

```text
1. 이메일 인증 API → EmailVerificationController
2. 인증 처리 → EmailVerificationService
3. 가입 API → SignupController
4. 가입 검증·저장 → SignupService
5. 테이블 구조 → auth/entity 및 resources/db/migration
6. 공통 응답·보안 → common/response 및 common/security
```

Java 경로의 기준은 `src/main/java/com/preen/preen_backend` (회원가입은 `auth`, 공통은 `common` 패키지)입니다.

## 가입 순서와 테이블 연결

```text
이메일 입력
└─ verification_codes에 인증번호 해시와 만료 시각 저장
   └─ SMTP로 6자리 번호 발송
      └─ 번호 검증 성공 → signupToken 발급
         └─ 회원 정보와 약관 동의를 가입 API로 제출
            └─ users 생성
               ├─ user_personal_info에 개인정보 저장
               ├─ user_credentials에 비밀번호 해시 저장
               ├─ user_term_agreements에 약관별 동의 저장
               └─ 사용한 signupToken 증명 제거 → 가입 완료
```

| 테이블·컬럼 | 연결 대상 | 역할 |
| --- | --- | --- |
| users.id | 회원의 기준 PK | 회원 번호·권한·계정 상태 |
| user_personal_info.user_id | users.id | 회원별 개인정보 |
| user_credentials.user_id | users.id | 회원별 비밀번호 인증 정보 |
| user_term_agreements.user_id | users.id | 동의한 회원 |
| user_term_agreements.term_id | terms.id | 동의 대상 약관의 종류·버전·본문 |
| verification_codes.target_hash | 이메일의 HMAC 해시로 식별 | 가입 전 인증 대상 확인 |

`verification_codes`는 가입 전에도 사용하므로 `users`를 참조하는 FK가 없습니다. 인증이 끝난 이메일의 해시와 가입용 토큰으로 가입 자격을 확인합니다. PK는 테이블의 행을 구분하는 키이고 FK는 다른 테이블의 행을 참조하는 키입니다.

## API와 ApiResponse

아래 세 API는 로그인 없이 호출합니다.

| 요청 | 입력 | 성공 시 data | HTTP 상태 |
| --- | --- | --- | --- |
| POST /api/auth/email/send | email | verificationId, expiresAt, resendAfterSeconds | 200 |
| POST /api/auth/email/verify | email, verificationId, code | signupToken, expiresAt | 200 |
| POST /api/auth/signup | email, signupToken, name, birthDate, password, passwordConfirmation, termsAgreed, privacyAgreed, marketingAgreed, termsVersion | userId, role, status | 201 |

`common/response/ApiResponse.java`가 응답 형식을 정의합니다. Controller는 성공 DTO를 `ApiResponse.success()`로 감싸고, `GlobalExceptionHandler`는 업무·입력 오류를 `ApiResponse.failure()`로 전달합니다.

```json
{
  "success": true,
  "data": { "userId": 3, "role": "GUEST", "status": "ACTIVE" },
  "code": null,
  "message": null
}
```

```json
{
  "success": false,
  "data": null,
  "code": "CODE_INVALID",
  "message": "인증번호가 올바르지 않습니다."
}
```

프론트 API 함수는 `response.data`에서 결과를 꺼냅니다. `http.ts`는 실패 응답을 `HttpError`로 전달하고 Hook은 오류 메시지를 표시합니다. 보안 필터가 직접 반환하는 미인증 응답은 별도 처리 경로입니다.

## 인증·보안 처리

```text
EmailVerificationService
├─ 인증번호 유효 시간 5분
├─ 재발송 간격 60초
├─ 잘못된 인증번호 5회 입력 시 검증 차단
└─ 인증 성공 시 가입용 토큰 발급·유효 시간 15분

SignupService
├─ 이미 가입한 이메일 거부
├─ 만 14세 이상 확인
├─ 비밀번호 8~50자·영문·숫자·특수문자와 확인 값 검사
├─ 현재 약관 버전과 필수 동의 확인
└─ 트랜잭션으로 회원·인증 정보·동의 내역 함께 저장

common/security/SecurityConfig
├─ 기능별 공개 API 경로 적용
├─ 허용한 프론트 주소의 CORS 요청 처리
└─ 공개 경로 외 요청에 인증 요구
```

이메일은 `EmailCryptoService`의 AES-GCM으로 암호화하여 `user_personal_info.email`에 저장합니다. ERD의 이메일 암호화 요구를 구현하는 알고리즘으로 AES-GCM을 선택했습니다. 조회·중복 확인에는 `AuthHashService`의 HMAC-SHA256으로 만든 `email_hash`를 사용합니다. 비밀번호는 `AuthConfig`의 PBKDF2 처리기로 해시합니다.

`signupToken`은 이메일 인증을 마쳤다는 가입용 증명입니다. 로그인 성공 시 발급할 Access JWT·Refresh Token과 별도로 관리합니다. 현재 로그인 화면 연결과 JWT·Refresh Token 기능은 후속 구현 대상입니다.

## 약관과 실행 확인

[약관 관리 가이드](terms/README.md)에서 본문·버전·수정 위치를 확인합니다.

```bash
# Java 21 환경에서 실행
./gradlew test bootJar
./gradlew integrationTest
```

`integrationTest`는 설정된 MySQL에 연결합니다. Flyway 마이그레이션으로 테이블과 약관 마스터를 반영하고, 테스트에서 생성한 회원·인증 데이터는 롤백합니다. 실제 메일 발송은 사용자 화면이나 Postman에서 확인합니다.

```sql
SELECT id, role, status FROM preen_dev.users ORDER BY id DESC;

SELECT a.user_id, t.type, t.version, a.is_agreed, a.agreed_at
FROM preen_dev.user_term_agreements a
JOIN preen_dev.terms t ON t.id = a.term_id
ORDER BY a.id DESC;
```
