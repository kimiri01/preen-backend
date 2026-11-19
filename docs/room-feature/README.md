# STAY-004 숙소 상세 보완 — Moon

브랜치 feature/stay-detail-policy_Moon. 기존 개인 기능 커밋8b1c70a를 기준으로 상세 조회 보완만 포함합니다.

GET /api/accommodations/{id}/rooms에 기존 DB의 latitude/longitude, 숙소 amenities, 활성 refundPolicy(id/version/policyText/rules)를 추가했습니다.
좌표가 없으면null, 활성 환불정책이 없으면null입니다. 규칙은 daysBefore 내림차순이며 refundRate는0~1입니다.
공개 숙소/활성 객실 검사 및 기존 응답 필드는 유지합니다. 객실별 facilities와 숙소 전체 amenities를 구분합니다.
기존 DB를 읽으며 새 스키마/예시 데이터/팀 공통 코드 변경은 없습니다.

검증: 실제DB 롤백 테스트로 좌표, 숙소편의시설, 활성정책 선택, 환불규칙 정렬, 정책 부재 및 기존 객실 조회를 확인했습니다.
동일명의 프론트 브랜치와 함께 확인하세요. 팀 상세 API의 최종 화면 조합/실시간 빈방·요금/정식 로그인은 별도 연결 범위입니다.
