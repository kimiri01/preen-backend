-- [kny] 숙소 상세페이지 예시 데이터: "숙소 소개"(description)와 "시설/서비스"(accommodation_amenities)
-- 기존 테이블만 사용하고 테이블 구조는 바꾸지 않습니다.
-- 대상: 메인 화면 숙소(코드가 있는 카테고리 + 공개 LISTED). 이미 소개가 있는 숙소는 덮어쓰지 않습니다.

-- 1) 시설/서비스 항목 추가 (V2의 기본 10개 + 상세페이지용 6개). 이미 있으면 넣지 않습니다.
INSERT INTO amenities (name)
SELECT s.name
FROM (
      SELECT '금연객실' AS name
      UNION ALL SELECT '24시간 데스크'
      UNION ALL SELECT '어메니티'
      UNION ALL SELECT '수하물 보관'
      UNION ALL SELECT '테라스/발코니'
      UNION ALL SELECT '공용 라운지'
) s
WHERE NOT EXISTS (SELECT 1 FROM amenities m WHERE m.name = s.name);

-- 2) 숙소 소개. 첫 줄은 화면에서 강조 문구로, 나머지 줄은 소개 문단으로 보여 줍니다. (CHAR(10) = 줄바꿈)
UPDATE accommodations a
JOIN accommodation_categories c ON c.id = a.category_id
JOIN regions g ON g.id = a.region_id
SET a.description = CASE c.code
    WHEN 'MOTEL' THEN CONCAT(
        '★ 깔끔하고 편안한 ', g.name, ' 모텔 ★', CHAR(10),
        a.name, '에 오신 것을 환영합니다.', CHAR(10),
        '합리적인 가격으로 깨끗한 객실과 편안한 휴식을 제공합니다.', CHAR(10),
        '늦은 시간에도 부담 없이 머물 수 있는 숙소입니다.')
    WHEN 'HOTEL' THEN CONCAT(
        '★ 도심 속 여유, ', g.name, ' 호텔 ★', CHAR(10),
        a.name, '에 오신 것을 환영합니다.', CHAR(10),
        '세련된 객실과 친절한 서비스로 여행의 시작과 끝을 함께합니다.', CHAR(10),
        '주요 관광지와 가까워 어디로든 이동이 편리합니다.')
    WHEN 'PRIVATE_HOUSE' THEN CONCAT(
        '★ 우리끼리 머무는 ', g.name, ' 독채 ★', CHAR(10),
        a.name, '은 한 채를 온전히 사용하는 프라이빗한 공간입니다.', CHAR(10),
        '마당과 주방을 갖추고 있어 가족, 친구와 함께 머물기 좋습니다.', CHAR(10),
        '조용하고 여유로운 시간을 보내보세요.')
    WHEN 'POOL_VILLA' THEN CONCAT(
        '★ 프라이빗 수영장이 있는 ', g.name, ' 풀빌라 ★', CHAR(10),
        a.name, '에서는 객실 전용 수영장을 누구의 방해도 없이 즐길 수 있습니다.', CHAR(10),
        '바비큐와 스파까지, 특별한 하루를 위한 모든 것이 준비되어 있습니다.')
    WHEN 'EMOTIONAL_STAY' THEN CONCAT(
        '★ 머무는 것만으로 여행이 되는 ', g.name, ' 감성스테이 ★', CHAR(10),
        a.name, '은 감각적인 인테리어와 따뜻한 조명으로 꾸며진 작은 숙소입니다.', CHAR(10),
        '좋아하는 음악을 틀어 두고 천천히 쉬어가세요.')
    WHEN 'GUESTHOUSE' THEN CONCAT(
        '★ 가볍게 떠나는 ', g.name, ' 게스트하우스 ★', CHAR(10),
        a.name, '은 여행자를 위한 아늑한 개인실과 공용 라운지를 갖추고 있습니다.', CHAR(10),
        '합리적인 가격으로 머물며 새로운 여행자들과 이야기를 나눠보세요.')
END
WHERE a.listing_status = 'LISTED'
  AND c.code IN ('MOTEL', 'HOTEL', 'PRIVATE_HOUSE', 'POOL_VILLA', 'EMOTIONAL_STAY', 'GUESTHOUSE')
  AND (a.description IS NULL OR TRIM(a.description) = '');

-- 3) 숙박 유형별 시설/서비스 연결. 이미 연결된 항목은 건너뜁니다.
INSERT INTO accommodation_amenities (accommodation_id, amenity_id)
SELECT a.id, m.id
FROM accommodations a
JOIN accommodation_categories c ON c.id = a.category_id
JOIN (
      SELECT 'MOTEL' AS category_code, '와이파이' AS amenity_name
      UNION ALL SELECT 'MOTEL', '주차'
      UNION ALL SELECT 'MOTEL', '에어컨'
      UNION ALL SELECT 'MOTEL', '금연객실'
      UNION ALL SELECT 'MOTEL', '24시간 데스크'
      UNION ALL SELECT 'MOTEL', '어메니티'
      UNION ALL SELECT 'HOTEL', '와이파이'
      UNION ALL SELECT 'HOTEL', '주차'
      UNION ALL SELECT 'HOTEL', '조식'
      UNION ALL SELECT 'HOTEL', '에어컨'
      UNION ALL SELECT 'HOTEL', '금연객실'
      UNION ALL SELECT 'HOTEL', '24시간 데스크'
      UNION ALL SELECT 'HOTEL', '어메니티'
      UNION ALL SELECT 'HOTEL', '수하물 보관'
      UNION ALL SELECT 'PRIVATE_HOUSE', '와이파이'
      UNION ALL SELECT 'PRIVATE_HOUSE', '주차'
      UNION ALL SELECT 'PRIVATE_HOUSE', '주방'
      UNION ALL SELECT 'PRIVATE_HOUSE', '세탁기'
      UNION ALL SELECT 'PRIVATE_HOUSE', '에어컨'
      UNION ALL SELECT 'PRIVATE_HOUSE', '바비큐'
      UNION ALL SELECT 'PRIVATE_HOUSE', '테라스/발코니'
      UNION ALL SELECT 'POOL_VILLA', '와이파이'
      UNION ALL SELECT 'POOL_VILLA', '주차'
      UNION ALL SELECT 'POOL_VILLA', '수영장'
      UNION ALL SELECT 'POOL_VILLA', '바비큐'
      UNION ALL SELECT 'POOL_VILLA', '주방'
      UNION ALL SELECT 'POOL_VILLA', '에어컨'
      UNION ALL SELECT 'POOL_VILLA', '스파'
      UNION ALL SELECT 'POOL_VILLA', '테라스/발코니'
      UNION ALL SELECT 'EMOTIONAL_STAY', '와이파이'
      UNION ALL SELECT 'EMOTIONAL_STAY', '주차'
      UNION ALL SELECT 'EMOTIONAL_STAY', '에어컨'
      UNION ALL SELECT 'EMOTIONAL_STAY', '금연객실'
      UNION ALL SELECT 'EMOTIONAL_STAY', '어메니티'
      UNION ALL SELECT 'EMOTIONAL_STAY', '테라스/발코니'
      UNION ALL SELECT 'GUESTHOUSE', '와이파이'
      UNION ALL SELECT 'GUESTHOUSE', '조식'
      UNION ALL SELECT 'GUESTHOUSE', '세탁기'
      UNION ALL SELECT 'GUESTHOUSE', '에어컨'
      UNION ALL SELECT 'GUESTHOUSE', '공용 라운지'
      UNION ALL SELECT 'GUESTHOUSE', '수하물 보관'
) s ON s.category_code = c.code
JOIN amenities m ON m.name = s.amenity_name
WHERE a.listing_status = 'LISTED'
  AND NOT EXISTS (SELECT 1 FROM accommodation_amenities x WHERE x.accommodation_id = a.id AND x.amenity_id = m.id);
