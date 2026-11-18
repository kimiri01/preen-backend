# 별점 등록·음악 리뷰 작성/조회 (Moon)

## 범위와 현재 상태
로컬 feature/music-review-create_Moon. 기존 develop 통합본 위에 추가했습니다.
팀 MVP 명세 REVIEW-001(별점+음악 작성), REVIEW-003(공개 조회)을 기준으로 합니다.
글/사진/호스트 답글, 수정·삭제(REVIEW-004), 음악 검색(REVIEW-002)은 이번 범위에서 제외합니다.
팀 로그인 사용자 식별 방식은 미정이며 사용자 확인에 따라 연결을 남겼습니다.
실제 익명 브라우저 등록은 차단되며, 성공 저장과 목록 반영은 테스트 인증 주체·DB 통합 테스트로 검증합니다.

## API
- GET /api/review-reservations?accommodationId=32 : 해당 숙소의 본인 이용 완료·미작성 예약(최대50건).
- GET /api/review-reservations/{reservationId} : 본인 예약의 작성 가능 여부와 이미 작성한 리뷰ID.
- GET /api/reviews/csrf : 팀 CSRF 보호를 유지하기 위한 인증된 요청의 헤더명/토큰.
- POST /api/reviews : reservationId, rating(정수1~5), spotifyTrackId(영문숫자22자). 성공201 + ApiResponse.data.
- GET /api/accommodations/{id}/reviews : 기존 /music-reviews와 같은 공개 조회의 명세 호환 경로.

예약/사용자/숙소 ID는 서버의 예약 관계에서 확인합니다. body의 userId나 accommodationId는 저장 근거로 사용하지 않습니다.
곡 제목/아티스트/커버는 DB에 저장하지 않습니다. 기존 reviews 테이블을 사용하며 새 스키마는 없습니다.
서버가 활성 사용자, 예약 소유자, COMPLETED 상태와 지난 체크아웃 날짜를 확인합니다.
타인 예약404, 이용 전·중복409, 입력 오류400. 예약 행 잠금과 기존 UNIQUE(reservation_id)로 중복을 막습니다.
숨김 리뷰도 기존 리뷰로 간주합니다. 공개 조회와 달리 미공개 숙소의 본인 이용 이력은 작성 검사에서 공개 여부와 분리합니다.

## 로그인 연결 지점
ReviewUserPrincipal을 팀 로그인 인증 주체에 구현하고 Spring Security Authentication.principal에 설치해야 합니다.
또는 로그인 계약 확정 후 ReviewUserResolver만 해당 계약에 맞게 수정합니다.
일반 모드에서는 신뢰된 해당 타입의 principal만 허용합니다. 숫자 username/헤더/body의 userId는 허용하지 않습니다.
예약 담당 브랜치 22eac81의 임시 인증 계약도 호환합니다. app.temp-auth.enabled=true인 경우에만
서버가 만든 UsernamePasswordAuthenticationToken의 Long principal 및 ROLE_USER를 받습니다.
이 설정은 기본 false이며 이번 작업에서 켜지 않았습니다. 임시 사용자 ID를 리뷰 코드에 고정하지 않습니다.
팀 임시 모드에서 CSRF가 꺼져 토큰이 없으면 /csrf는 null 필드를 반환하고 프론트는 해당 헤더를 생략합니다.
일반 모드의 토큰 발급·전달 및 공통 SecurityConfig는 그대로입니다.
이는 정식 로그인이 아니며 팀 임시 인증 코드가 실제 실행되는 환경에서만 시험할 수 있습니다.
JWT를 쓰더라도 서명 검증과 서버 인증 컨텍스트 구성이 선행돼야 합니다.

## 검증
신규 컨트롤러3개·DB 서비스5개 테스트: 정상 저장 및 기존 별점 조회 반영,
타인·미완료·미래 완료·탈퇴 계정·중복(숨김 포함)·잘못된 별점/곡/사용자 위조 시도 확인.
DB 테스트는 승인된 로컬 검증 DB에서 트랜잭션 롤백합니다. 기존 예시 리뷰4개는 유지됩니다.
전체 백엔드33개+기존 회원가입 통합5개 통과 후, 최종 입력 검증 변경에 해당하는 신규8개 재검증.
실제 서버 익명 GET401, 익명 POST403, 공개 조회200 확인.
실제 로그인 후 브라우저 등록/CSRF 왕복은 로그인 기능 연결 후 추가 검증이 필요합니다.

팀 통합 전 프론트 작성 경로, 예약 상세 링크, 로그인 principal, 곡 검색 선택값 계약을 합의해야 합니다.

## 2026-11-18 팀 최신 코드 재확인
원격 develop: backend 24545a4 / frontend 00e5b93. 예약 API 최신은 별도 feature/reservation-api-yjhb 22eac81이며 develop에는 아직 없습니다.
팀 예약 생성 입력은 roomId, checkIn, checkOut, guestCount입니다. 현재 날짜·인원 선택 값과 호환됩니다.
예약 생성 버튼 활성화·재고 선점·결제 진행은 이번 변경에 포함하지 않습니다. 리뷰는 COMPLETED 예약만 허용합니다.
팀 예약 브랜치에는 새 Flyway 변경이 없고, 현재 reviews/예약 관계에 대한 스키마 수정도 필요하지 않습니다.
공통 코드/팀 브랜치/원격 저장소/실제 비밀값/DB를 변경하지 않았습니다.

## 별도 예약 통합 시험 완료
팀 22eac81과 별도 복사본에서 실제 보안 필터/DB를 통한 자동 HTTP 통합 시험 2개 통과.
본인 완료 예약 리뷰201/공개 조회200/중복409/타인404 확인, 데이터 롤백.
팀 JPA 저장소와 충돌하는 Moon RoomRepository bean 이름을 moonRoomQueryRepository로 명시.
정식 로그인 및 수동 브라우저 왕복은 별도 확인 대상. 신규 통합본은 아직 원격에 올리지 않았습니다.
