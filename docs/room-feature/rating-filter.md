# REVIEW-003 표준 페이지·트랙 모음 — Moon

브랜치 feature/review-track-list_Moon. 기존 개인 기능 커밋8b1c70a 위에 리뷰 조회 보완만 포함합니다.

- GET /api/accommodations/{id}/reviews?rating=&sort=&page=&size=
- 공통 PageResponse: content/page/size/totalElements/totalPages/hasNext. 기본20/최대50, page0부터.
- GET /api/accommodations/{id}/tracks: 곡별 중복 제거, 최근 첨부순 최대50곡. spotifyTrackId/reviewCount/latestCreatedAt 반환.
- 공개숙소·VISIBLE 리뷰·소유 관계가 일치하는 COMPLETED 예약만 반환. 작성자는 첫글자** 마스킹.
- 기존 /music-reviews는 10개 및 total/filteredTotal/reviews 응답을 그대로 유지합니다.

검증: 크기·페이지 경계/입력 거절, 숨김·미완료 제외, 곡 중복집계·최근순·50곡 제한을 테스트했습니다.
DB 변경은 롤백합니다. 동일명의 프론트 브랜치가 표준페이지 API를 사용하며 신규 등록 시 리뷰·트랙 캐시를 갱신합니다.
정식 로그인·Spotify 검색은 별도 연결 대기이며 팀 공통 PageResponse를 수정하지 않았습니다.
