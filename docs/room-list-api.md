# 객실 리스트 및 스펙 조회

GET /api/accommodations/{accommodationId}/rooms

- 공개(LISTED) 숙소의 활성(ACTIVE) 객실만 조회. local 프로필에서 활성화.
- 요청 ID는 양의 signed BIGINT 범위. 잘못된 ID는 400, 없는/비공개 숙소는 404.
- 기존 숙소에 활성 객실이 없으면 200 + rooms: [].
- accommodationId, accommodationName, description, address, checkInTime, checkOutTime, rooms 배열 반환. 숙소 공개 정보만 조회하며 DB 구조는 변경하지 않음.
- 각 객실: id, name, capacity, maxCapacity, nightlyPrice, extraPersonFee, images.
- images: id, imageUrl, sortOrder. sortOrder/id 순으로 정렬.
- BIGINT ID와 금액은 정밀도 유지를 위해 JSON 문자열로 전달.
- 기본 가격이며 날짜별 재고/판매가/할인/예약 가능 여부를 계산하지 않음.
- 계정/사업자 개인정보나 결제정보는 반환하지 않음. SQL 매개변수 바인딩 사용.
- 공개 상태 정책은 개발용 기본안으로, 관리자·숨김·판매중지 상세 정책은 담당자와 협의 필요.

## 로컬 실행과 확인
DB 실행 후 local 프로필로 백엔드 실행. 프론트 npm run dev는 /api를 localhost:8080으로 프록시.
VITE_API_BASE_URL을 별도로 설정했다면 프록시 대신 그 주소를 사용하므로 로컬에서는 비워 둠.
실제 배포 시 API 주소/인증/CORS 계약은 별도 설정해야 함.
기존 홈페이지 카드는 목업 데이터이므로 실제 DB ID와 연결하지 않음.
학습용 경로: /accommodations/900001/rooms. 학습용 실제 DB fixture를 사용하며 외부 서비스나 실회원 데이터 없음.
기본정보 화면은 익명 공개 조회이며 로그인/예약 API를 추가하지 않음.

## 검증
컨트롤러 3개(잘못된 ID 묶음,404,빈 목록), 로컬 DB 통합 3개(활성 객실/사진 순서,숨김 차단,없음과 빈 목록 구분) 통과.
DB 통합 테스트는 PREEN_VERIFY_LOCAL_DB=true로 명시 실행하며 fixture는 트랜잭션 롤백.
기존 시작/DB연결 테스트 포함 8개 실패0·건너뜀0. 프론트 빌드 및 브라우저 사진 전환/잘못된 주소 안내 확인.

## 프론트 통합 지점
features/rooms/public에서 RoomListSection, Room, RoomList 공개.
숙소 상세 페이지에서 <RoomListSection rooms={객실조회결과.rooms} /> 사용.
객실 목록 부분만 재사용 가능하며 페이지 헤더/푸터/홈 링크는 RoomListPage에만 있음.
RoomListSection은 API를 직접 호출하지 않아 팀의 기존 조회 로직과도 조합 가능.
부가 기능 정렬/상세 펼치기/사진 확대는 프론트 내부 동작이며 API/DB 변경 없음.
공통 스타일의 text/muted/border/paper/surface 변수 사용. 앱 인프라 병합 시 React Query Provider/공통 HTTP 클라이언트와 API 주소 확인 필요.

## 로컬 확장안: 면적·침대·편의시설
사용자가 팀 협의 전 로컬 구현을 승인하여 추가한 제안이며 기존 팀 ERD 확정 사항이 아님.
기존 30개 테이블은 유지하고 room_specs, room_beds, room_facilities, room_facility_links 4개를 추가. 총 34개.
새 환경은 02 스키마 → 03 카테고리 → 04 예시 객실 → 08 확장 스키마 → 09 확장 예시 순으로 각1회 적용. 기존 실행 환경에서 02~04를 재실행하지 말 것. 05~07은 과거 로컬 갱신 기록으로 새 환경에서는 실행하지 않음.
08/09는 local-setup/database에 보관. 자동 migration으로 등록하지 않았으며 배포·팀 병합 전에 검토 필요.
응답에 roomType(문자열 또는 null), areaSqm(㎡ 단위 십진 문자열 또는 null), beds:[{type,count}], facilities:[{code,name}] 추가.
침대 type: SINGLE, SUPER_SINGLE, DOUBLE, QUEEN, KING, BUNK, FLOOR_BEDDING. 마지막은 침대가 아닌 침구 세트.
기존 객실에 확장 정보가 없으면 null/빈 배열. 면적 0 초과, 침대 수 0 초과 DB 제약 적용.
목록 전체를 5회 조회하여 객실 수에 비례하는 쿼리를 만들지 않음. 미노출 객실의 확장 정보도 제외.
정보 누락 및 복수 침대/편의시설 혼합 상황 테스트 2개 추가, 기존 포함 총10개 통과.
이전 문서의 '스펙 항목 미구현/총30개'는 확장 전 기록이며 이 단락을 우선 참조.

## 오디오 기본 구비 객실 (로컬 제안)
10_room_audio_extension.sql로 room_audio 추가. 총35테이블.
RoomView.audio는 미구비 시 null, 구비 시 equipmentName/connectionGuide/availableFrom/availableUntil/volumeGuide 반환.
해당 레코드는 기본 구비 및 객실 요금 포함을 의미. 대여/추가결제/사전신청/재고 모델이 아니며 별도 요금은 없음.
로컬 예시2객실(테라스·풀 스위트), 이용09:00–22:00, 개인 기기 연결·적정 음량·음원 이용권 미제공 안내. 실제 숙소의 장비 제공 사실을 확인한 데이터가 아니라 컨셉 검증용임.
미구비/비공개/비활성 객실의 오디오 처리 테스트 포함 총11개 통과. 객실수와 무관하게 총6회 조회.
프론트는 구비 객실 필터/배지/기본 구비 안내를 표시. 음악 재생·음원 전송·리뷰 작성 기능은 추가하지 않았음.

PREEN Must12 원본 대조 보완: 객실 행 가격/예약 안내 위치 추가. 원본의 실제 잔여객실·선택날짜 총액은 재고/예약 기능에 연결한 뒤 주입해야 하므로 아직 미표시. 예약 버튼은 standalone에서 비활성, onSelectRoom 콜백을 전달한 경우 객실 선택 이벤트만 발생. 예약 생성/확정은 수행하지 않음. 예시명/가격 갱신은 로컬 SQL11.

## 객실 Spotify 감상 연결
12_room_spotify.sql로 room_audio.spotify_track_id nullable 추가(테이블 수는35 유지). 트랙 ID만 저장하며 음악파일/제공자 토큰 저장 안 함.
audio.spotifyTrackId를 공식 Spotify Embed에 사용. 현재 예시 추천곡은 Spotify 공식 Embed 튜토리얼의 트랙 ID로, 숙소 추천 데이터 확정 전 교체 필요.
RoomSpotifyPlayer는 사용자 클릭 후 iframe을 로드하고 Spotify 공식 링크로도 이동 가능. 자동재생 안 함. 입력은 22자리 트랙ID 또는 https://open.spotify.com/(intl-xx/)track/{id}만 수용하며 iframe호스트는 고정.
기존 개인 음악서비스 안내 제거. PREEN Spotify 플레이어→기기의 Bluetooth/AUX→객실오디오 흐름으로 변경.
Spotify 곡 검색 API, 로그인 OAuth, Web Playback SDK/Spotify Connect 제어는 아직 구현하지 않음. 링크로 곡 변경과 공식 Embed를 통한 감상 범위이며 전체곡 재생권/자동 스피커 연결을 보장하지 않음. 실제 장비 출력은 사용자 기기 연결이 필요.
공식 문서: https://developer.spotify.com/documentation/embeds/tutorials/creating-an-embed

브라우저 검증: 첫 공식 예시 곡은 지역제한 안내가 표시돼 Web API 공식문서 예시 트랙11dFghVXANMlKmJXsNCbNl로 교체. 플레이어는 Cut To The Feeling/Carly Rae Jepsen과 미리듣기/재생버튼 표시. 실제 오디오 재생 및 장비 출력·계정 로그인은 검증하지 않음. Spotify 외 URL 차단 및 유효 곡 링크 전환 확인.

## Must 13 음악 리뷰 조회 (2026-11-16 추가)
GET /api/accommodations/{accommodationId}/music-reviews?rating=0&sort=latest&page=0
- rating: 0(전체), 1~5. sort: latest/rating-desc/rating-asc. page: 0~10000, 페이지 크기 10.
- 공개 숙소·VISIBLE 리뷰·COMPLETED 예약·동일 숙소 객실·동일 이용자의 예약만 조회. 잘못된 값 400, 없는/비공개 숙소 404.
- accommodationId,total,filteredTotal,page,pageSize,reviews 반환.
- 리뷰: id,roomName,spotifyTrackId,rating,createdAt,authorLabel(성 첫 글자+**),stayedOn,title,artist,album.
- 사용자ID·예약번호·전체 이름·전화번호·Spotify 토큰은 반환하지 않음. Spotify 곡 메타데이터가 없으면 title/artist/album=null.
- 14_music_review_metadata.sql 적용 필요. 15_must13_review_demo.sql은 사용자 승인된 가상 예약·리뷰 4개와 곡 메타데이터만 추가.
- Spotify 재생은 공식 Embed 사용. 다른 리뷰의 플레이어를 열면 기존 iframe 제거. 전체곡/계정 로그인/Spotify Connect·커스텀 진행바 SDK는 미구현.
- 메타데이터 출처: https://open.spotify.com/oembed?url=https://open.spotify.com/track/{trackId}
- Blueming: 4Dr2hJ3EnVh2Aaot6fRwDO / Love poem: 7HrE6HtYNBbGqp5GmHbFV0 / 밤편지: 3P3UA61WRQqwCXaoFOTENd / 우주를 건너: 0i2XXofKXlXbZ2sjnh06Jj.
