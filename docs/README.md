# 백엔드 문서

회원가입 구현을 이해하려면 회원가입 가이드를 먼저 읽고, 약관 본문과 관리 방법을 이어서 확인합니다.

```text
docs/
├─ README.md → 문서 안내
├─ signup-guide.md → 가입 순서·테이블 연결·API·보안 역할
└─ terms/
   ├─ README.md → 약관 버전 관리·수정 위치·확정할 운영 정보
   ├─ service-terms.md → 필수 서비스 이용약관 초안
   ├─ privacy-consent.md → 필수 개인정보 수집·이용 동의 초안
   └─ marketing-consent.md → 선택 이메일 수신 동의 초안
```

- [회원가입 구현 가이드](signup-guide.md)
- [약관 관리 가이드](terms/README.md)

문서에는 실제 환경변수 비밀값, 비밀번호, 발급 토큰을 기록하지 않습니다.
