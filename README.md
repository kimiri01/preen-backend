# PREEN 객실 및 음악 리뷰 API

Java21 / Spring Boot4.1.2 / Gradle. 로컬 MySQL을 사용하는 JDBC 구현입니다.
현재 패키지는 com.preen.backend이며 팀 공통 코드·DB·응답 규칙과의 통합은 별도 확인이 필요합니다.

- GET /api/accommodations/{accommodationId}/rooms
- GET /api/accommodations/{accommodationId}/music-reviews?rating=0&sort=latest&page=0
- 상세 명세: docs/room-list-api.md
- 로컬 SQL과 적용 순서: docs/database/README.md

## 로컬 실행
1. 별도 로컬 DB를 준비합니다. 참고 SQL을 기존 팀 DB에 일괄 실행하지 마세요.
2. application-local.example.properties를 application-local.properties로 복사하고 본인 접속 정보를 입력합니다.
3. ./gradlew bootRun --args='--spring.profiles.active=local'
4. 서버 http://127.0.0.1:8080. local 프로필에서만 API가 활성화됩니다.

비밀번호 파일은 Git에서 제외합니다. SQL 자동 실행과 Hibernate 자동 DDL은 비활성화했습니다.
별도 DB 없이 ./gradlew test로 기본 테스트를 실행합니다.
DB 통합 테스트는 격리된 로컬 DB를 준비한 뒤 PREEN_VERIFY_LOCAL_DB=true로 실행합니다. 테스트 INSERT는 트랜잭션 종료 시 롤백합니다.

## 기능 범위
공개 숙소의 활성 객실·사진·스펙·시설·오디오 조회, 공개 음악 리뷰의 별점필터·정렬·페이지 조회입니다.
리뷰는 이용 완료 예약의 작성자/숙소/객실 관계가 일치하는 경우만 공개합니다. 원본 이름·연락처·계정 정보는 반환하지 않습니다.
날짜별 판매 재고·총액·예약 생성·리뷰 작성·인증·Spotify 로그인/재생 SDK는 구현하지 않았습니다.
프론트는 공식 Spotify Embed로 곡을 표시하며 서버는 곡 ID와 표시 메타데이터만 제공합니다.

팀 통합 시 패키지, 단수/복수 테이블명, ApiResponse 래퍼 및 예약 상태 계약을 함께 맞춰주세요.
