# 회원가입 약관 관리

현재 화면과 DB에 적용된 버전은 `2026-11-16-draft-v1`입니다. 이 폴더의 본문은 해당 마이그레이션에 등록된 초안을 복사한 문서입니다.

| 종류 | 필수 여부 | 본문 |
| --- | --- | --- |
| SERVICE | 필수 | [서비스 이용약관](service-terms.md) |
| PRIVACY | 필수 | [개인정보 수집·이용 동의](privacy-consent.md) |
| MARKETING | 선택 | [이메일 수신 동의](marketing-consent.md) |

## 코드와 연결되는 위치

```text
프론트 PREEN_Front_Git
└─ src/features/auth/
   ├─ data/signupTerms.ts → 화면 본문·SIGNUP_TERMS_VERSION
   ├─ components/SignupDetailsForm.tsx → 내용 보기·동의 체크박스
   └─ hooks/useSignupDetails.ts → 가입 요청에 약관 버전·동의 값 포함

백엔드 PREEN_Back_Git
└─ src/main/
   ├─ java/com/preen/preen_backend/auth/service/SignupService.java
   │  └─ SIGNUP_TERMS_VERSION 검사·약관 조회·동의 저장
   └─ resources/db/migration/
      ├─ V1__user_auth_terms.sql → 약관 테이블 생성
      └─ V102__auth_signup_email_verification.sql → 본문이 있는 초안 추가
```

가입 시 `user_term_agreements.term_id`가 `terms.id`를 참조합니다. 따라서 기존 회원이 어느 버전의 약관에 동의했는지 확인할 수 있습니다. 마케팅을 선택하지 않은 경우도 `is_agreed = false`로 저장합니다.

## 개정 순서

1. 새 약관 본문과 버전을 결정합니다.
2. 새 Flyway SQL 파일에 새 버전의 약관 세 건을 추가합니다. 이미 적용된 SQL과 기존 약관 행은 보존합니다.
3. 프론트 `signupTerms.ts`의 본문·버전과 백엔드 `SignupService`의 버전을 함께 맞춥니다.
4. 이 폴더의 본문과 버전 안내도 갱신합니다.
5. 서버 실행으로 마이그레이션을 적용하고 내용 보기·필수 동의·선택 미동의·가입 저장을 확인합니다.

문서 파일을 편집한 내용은 화면이나 DB에 자동 반영되지 않습니다. 화면은 프론트 정적 데이터, DB는 Flyway SQL에서 본문을 읽습니다.

## 공개 전 확정할 사항

- 운영자의 법적 명칭·주소·공식 문의처와 개인정보 보호 담당자
- 예약의 판매 주체와 취소·환불 정책
- 개인정보 항목별 보존 근거·기간과 실제 탈퇴·파기 절차
- 개인정보 열람·정정·삭제 등 권리 행사 접수 경로
- 처리 위탁·제3자 제공·국외 이전 여부
- 광고 발송 시 발신자 표시와 실제 수신 거부·철회 처리 경로

현재 약관은 초안이며 운영 정보 확정과 법률 검토가 남아 있습니다. 개인정보 동의 문서는 전체 개인정보 처리방침과 함께 검토해야 합니다. 현재 마케팅 기능은 동의 기록 저장까지 구현돼 있습니다.

초안 작성 시 참고한 자료는 [개인정보 보호법 제15조](https://law.go.kr/lsLinkCommonInfo.do?chrClsCd=010202&lsJoLnkSeq=1034516315)와 [정보통신망법 제50조](https://www.law.go.kr/LSW/lsLinkCommonInfo.do?chrClsCd=010202&lsJoLnkSeq=1034054981)입니다.
