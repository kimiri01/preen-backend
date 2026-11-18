# 객실·음악 리뷰 팀 통합 (Moon)

## 기준과 범위
- 기준: origin/test/accmmo-serch-test (668fba7). develop에는 아직 검색 통합 코드가 없어 해당 검증 브랜치를 기준으로 분리했습니다.
- 작업 브랜치: feature/room-list-integration_Moon. 기존 feature/room-list-spec_Moon은 보존합니다.
- 팀 JPA 테이블, 공통 ApiResponse, 기존 Flyway 기록을 유지합니다. 기존 마이그레이션은 수정하지 않습니다.
- V20261118.1500__room_specs_and_audio.sql은 객실 면적·침대·편의시설·오디오 확장 테이블만 추가합니다. 팀 인증 V102 및 검색 V103과 겹치지 않도록 날짜·시분 번호를 사용합니다.

## API
모든 응답은 팀 ApiResponse의 data에 담깁니다.
- GET /api/accommodations/{id}/rooms: 공개 숙소·활성 객실, 숙소 이미지, 객실 스펙/이미지/시설/오디오.
- GET /api/accommodations/{id}/music-reviews: 공개된 이용 완료 예약 리뷰. rating(0=전체,1~5), sort(latest/rating-desc/rating-asc), page(0부터), 페이지당10개.
- GET /api/spotify/tracks/{id}/metadata: 공개 리뷰 또는 활성 객실 추천에 등록된 곡만 조회합니다.
- 잘못된 입력400, 미공개/없는 숙소404, 외부 서비스 실패503. 내부 오류나 DB 인증정보는 응답에 노출하지 않습니다.

Spotify 제목은 공식 oEmbed에서 가져와 서버 메모리에 최대5분 보관하며 DB에 저장하지 않습니다. 응답 HTML은 사용하지 않습니다. 아티스트/앨범 정보는 공식 플레이어에서 확인합니다. 클라이언트 비밀키나 액세스 토큰은 필요하지 않습니다. OAuth·Web Playback SDK·Spotify Connect 구현은 포함하지 않습니다.

## 실행
Java21과 팀 DB 스키마를 사용합니다. application-moon.example.properties의 환경변수 PREEN_DB_URL, PREEN_DB_USER, PREEN_DB_PASSWORD를 개인 실행 설정에 넣고 --spring.config.location=file:application-moon.example.properties로 실행합니다. 비밀번호 파일은 커밋하지 않습니다. PREEN_FRONTEND_ORIGIN은 기본 http://127.0.0.1:5173입니다.

Flyway 실행 전에 DB를 백업하고 팀의 마이그레이션 번호 충돌 여부를 확인하세요. 기존 SQL을 덮어쓰거나 팀 DB를 초기화하지 않습니다. demo-data.example.sql은 별도 로컬 확인 DB에서만 한 번 사용하는 선택 예시이며 자동 실행되지 않습니다.

## 검증과 남은 연결
2026-11-18: 격리된 MySQL8.4.11 스키마에서 Flyway V1~V7/V100/V101/V102, JPA validate 및 테스트17개 통과. 객실 API, 별점5 필터, 실제 Spotify 제목 조회 확인.
DB 없이 컨트롤러/Spotify 단위 테스트만 실행할 때: ./gradlew test --tests '*RoomControllerTests' --tests '*MusicReviewControllerTests' --tests '*SpotifyMetadataServiceTests'
DB 통합 테스트는 PREEN_VERIFY_LOCAL_DB=true와 개인 DB 실행 설정이 필요합니다.

실시간 재고, 날짜별 총액, 예약 생성/결제, 리뷰 작성, 인증 연결은 이번 변경에 포함하지 않습니다. 예약 버튼은 해당 연결 전까지 비활성화합니다. 실제 재생 범위는 Spotify 로그인·계정·이용 조건에 따릅니다. 팀 상세 페이지 담당자와 라우트/공통 응답/마이그레이션을 리뷰한 뒤 병합해야 합니다.
