-- [kny] 메인 페이지 기능용 데이터 (카테고리 바·필터링 / 전체 카테고리·최근 본 숙소 / 숙소 카드 뷰)
-- Notion 개발 현황 (1)(2)(3) 문서의 구성을 그대로 따른다.
-- 팀원이 만든 V1~V7, V100 파일은 고치지 않고, 이 파일에서 필요한 것만 "추가"한다. (Git 충돌 방지)
--
-- 1) 카테고리·지역에 code 컬럼 추가: 프론트 버튼(HOTEL, SEOUL 등)과 DB를 연결하는 고정 코드
-- 2) 카테고리 6개(모텔·호텔·독채·풀빌라·감성스테이·게스트하우스)에 코드 부여 (없는 4개는 새로 추가)
-- 3) 지역 5개(서울·강원도·대구·광주·제주도)에 코드 부여 (팀원 지역 데이터의 시·도 행을 그대로 사용)
-- 4) 팀원 예시 숙소 13건은 지우지 않고 숨김(HIDDEN) 처리 → DB에는 남고 화면에는 안 보인다
-- 5) 예시 숙소 36건(카테고리별 6건) + 대표 사진 + 기본 객실(1박 요금) + 날짜별 재고
--    지역·주소·요금은 예전 Java 초기화 코드(seed 20261117)와 똑같은 값이다.

-- 1) code 컬럼
ALTER TABLE accommodation_categories
    ADD COLUMN code VARCHAR(30) NULL COMMENT '프론트와 약속한 고정 코드 (예: HOTEL). 없으면 메인 화면에 안 보임' AFTER id,
    ADD CONSTRAINT uk_accommodation_categories_code UNIQUE (code);

ALTER TABLE regions
    ADD COLUMN code VARCHAR(30) NULL COMMENT '프론트와 약속한 고정 코드 (예: SEOUL). 없으면 메인 화면에 안 보임' AFTER id,
    ADD CONSTRAINT uk_regions_code UNIQUE (code);

-- 2) 카테고리 6개. sort_order 101~106은 "전체 숙소" 묶음의 표시 순서다.
--    이름이 같은 기존 행(모텔, 게스트하우스)은 새로 만들지 않고 코드만 붙인다.
UPDATE accommodation_categories SET code = 'MOTEL', sort_order = 101 WHERE name = '모텔';
UPDATE accommodation_categories SET code = 'GUESTHOUSE', sort_order = 106 WHERE name = '게스트하우스';
INSERT INTO accommodation_categories (code, name, sort_order)
VALUES ('HOTEL', '호텔', 102),
       ('PRIVATE_HOUSE', '독채', 103),
       ('POOL_VILLA', '풀빌라', 104),
       ('EMOTIONAL_STAY', '감성스테이', 105);

-- 3) 지역 5개 (시·도 단계)
UPDATE regions SET code = 'SEOUL' WHERE name = '서울' AND level = 1;
UPDATE regions SET code = 'GANGWON' WHERE name = '강원' AND level = 1;
UPDATE regions SET code = 'DAEGU' WHERE name = '대구' AND level = 1;
UPDATE regions SET code = 'GWANGJU' WHERE name = '광주' AND level = 1;
UPDATE regions SET code = 'JEJU' WHERE name = '제주' AND level = 1;

-- 4) 팀원 예시 숙소 13건 숨김 (삭제하지 않음. 다시 보이게 하려면 listing_status를 'LISTED'로 바꾸면 된다)
UPDATE accommodations
SET listing_status = 'HIDDEN'
WHERE name IN ('스테이 인 종로', '동성로 루미 모텔', '시온 시티 호텔', '시온 오션 호텔', '해담 호텔 제주',
               '솔숲 독채 평창', '무등 독채', '온유 풀빌라 제주', '숲결 풀빌라 양양', '온유 스테이 제주',
               '서가 스테이 대구', '여행자의 집 서촌', '양림 게스트하우스');

-- 5-1) 예시 숙소 36건 (호스트는 V100의 예시 사업자를 같이 쓴다)
INSERT INTO accommodations (host_business_id, region_id, category_id, name, address, check_in_time, check_out_time, listing_status, created_at)
SELECT (SELECT id FROM host_businesses WHERE business_registration_no = '0000000000'),
       (SELECT id FROM regions WHERE code = s.region_code),
       (SELECT id FROM accommodation_categories WHERE code = s.category_code),
       s.name, s.address, '15:00:00', '11:00:00', 'LISTED', NOW(6)
FROM (
      SELECT 'MOTEL' AS category_code, 'GANGWON' AS region_code, '루나 모텔 강원' AS name, '강원 속초시 해오름로 331' AS address, '/images/stays/motel-1.jpg' AS image_url, 106000 AS price
      UNION ALL SELECT 'MOTEL', 'GANGWON', '모먼트 모텔 강원', '강원 속초시 해오름로 41', '/images/stays/motel-2.jpg', 59000
      UNION ALL SELECT 'MOTEL', 'JEJU', '블루문 모텔 제주', '제주 제주시 구좌읍 해맞이해안로 191', '/images/stays/motel-3.jpg', 105000
      UNION ALL SELECT 'MOTEL', 'GWANGJU', '어반 모텔 광주', '광주 북구 첨단과기로 333', '/images/stays/motel-4.jpg', 111000
      UNION ALL SELECT 'MOTEL', 'GANGWON', '라온 모텔 강원', '강원 양양군 현남면 해변길 52', '/images/stays/motel-5.jpg', 141000
      UNION ALL SELECT 'MOTEL', 'SEOUL', '시그니처 모텔 서울', '서울 용산구 이태원로 30', '/images/stays/motel-6.jpg', 68000
      UNION ALL SELECT 'HOTEL', 'GANGWON', '시온 시티 호텔 강원', '강원 양양군 현남면 해변길 164', '/images/stays/hotel-1.jpg', 149000
      UNION ALL SELECT 'HOTEL', 'SEOUL', '해담 호텔 서울', '서울 용산구 이태원로 196', '/images/stays/hotel-2.jpg', 91000
      UNION ALL SELECT 'HOTEL', 'GWANGJU', '라벤더 호텔 광주', '광주 남구 양림로 284', '/images/stays/hotel-3.jpg', 127000
      UNION ALL SELECT 'HOTEL', 'GANGWON', '더 스카이 호텔 강원', '강원 속초시 해오름로 374', '/images/stays/hotel-4.jpg', 145000
      UNION ALL SELECT 'HOTEL', 'SEOUL', '오로라 호텔 서울', '서울 중구 을지로 80', '/images/stays/hotel-5.jpg', 102000
      UNION ALL SELECT 'HOTEL', 'JEJU', '메리안 호텔 제주', '제주 서귀포시 성산읍 일출로 353', '/images/stays/hotel-6.jpg', 60000
      UNION ALL SELECT 'PRIVATE_HOUSE', 'JEJU', '솔숲 독채 제주', '제주 제주시 구좌읍 해맞이해안로 365', '/images/stays/private-house-1.jpg', 95000
      UNION ALL SELECT 'PRIVATE_HOUSE', 'JEJU', '돌담 독채 제주', '제주 제주시 애월읍 애월로 71', '/images/stays/private-house-2.jpg', 104000
      UNION ALL SELECT 'PRIVATE_HOUSE', 'SEOUL', '하늘채 독채 서울', '서울 마포구 와우산로 326', '/images/stays/private-house-3.jpg', 58000
      UNION ALL SELECT 'PRIVATE_HOUSE', 'DAEGU', '고요 독채 대구', '대구 수성구 들안로 201', '/images/stays/private-house-4.jpg', 128000
      UNION ALL SELECT 'PRIVATE_HOUSE', 'JEJU', '느린별 독채 제주', '제주 제주시 애월읍 애월로 280', '/images/stays/private-house-5.jpg', 123000
      UNION ALL SELECT 'PRIVATE_HOUSE', 'SEOUL', '마루 독채 서울', '서울 강남구 테헤란로 357', '/images/stays/private-house-6.jpg', 77000
      UNION ALL SELECT 'POOL_VILLA', 'JEJU', '온유 풀빌라 제주', '제주 서귀포시 중문관광로 382', '/images/stays/pool-villa-1.jpg', 78000
      UNION ALL SELECT 'POOL_VILLA', 'GWANGJU', '숲결 풀빌라 광주', '광주 동구 무등로 331', '/images/stays/pool-villa-2.jpg', 55000
      UNION ALL SELECT 'POOL_VILLA', 'DAEGU', '아쿠아 풀빌라 대구', '대구 중구 동성로 172', '/images/stays/pool-villa-3.jpg', 69000
      UNION ALL SELECT 'POOL_VILLA', 'JEJU', '윤슬 풀빌라 제주', '제주 제주시 구좌읍 해맞이해안로 134', '/images/stays/pool-villa-4.jpg', 99000
      UNION ALL SELECT 'POOL_VILLA', 'DAEGU', '파란 풀빌라 대구', '대구 수성구 들안로 61', '/images/stays/pool-villa-5.jpg', 123000
      UNION ALL SELECT 'POOL_VILLA', 'JEJU', '썸머 풀빌라 제주', '제주 서귀포시 성산읍 일출로 381', '/images/stays/pool-villa-6.jpg', 75000
      UNION ALL SELECT 'EMOTIONAL_STAY', 'DAEGU', '온유 스테이 대구', '대구 중구 달구벌대로 112', '/images/stays/emotional-stay-1.jpg', 85000
      UNION ALL SELECT 'EMOTIONAL_STAY', 'SEOUL', '서가 스테이 서울', '서울 마포구 와우산로 128', '/images/stays/emotional-stay-2.jpg', 142000
      UNION ALL SELECT 'EMOTIONAL_STAY', 'JEJU', '오후 스테이 제주', '제주 서귀포시 성산읍 일출로 26', '/images/stays/emotional-stay-3.jpg', 86000
      UNION ALL SELECT 'EMOTIONAL_STAY', 'JEJU', '결 스테이 제주', '제주 제주시 애월읍 애월로 399', '/images/stays/emotional-stay-4.jpg', 76000
      UNION ALL SELECT 'EMOTIONAL_STAY', 'JEJU', '머묾 스테이 제주', '제주 제주시 애월읍 애월로 105', '/images/stays/emotional-stay-5.jpg', 60000
      UNION ALL SELECT 'EMOTIONAL_STAY', 'JEJU', '포근 스테이 제주', '제주 제주시 애월읍 애월로 84', '/images/stays/emotional-stay-6.jpg', 119000
      UNION ALL SELECT 'GUESTHOUSE', 'SEOUL', '여행자의 집 서울', '서울 마포구 와우산로 23', '/images/stays/guesthouse-1.jpg', 62000
      UNION ALL SELECT 'GUESTHOUSE', 'SEOUL', '양림 게스트하우스 서울', '서울 용산구 이태원로 300', '/images/stays/guesthouse-2.jpg', 86000
      UNION ALL SELECT 'GUESTHOUSE', 'GANGWON', '바람 게스트하우스 강원', '강원 춘천시 호반로 201', '/images/stays/guesthouse-3.jpg', 70000
      UNION ALL SELECT 'GUESTHOUSE', 'SEOUL', '골목 게스트하우스 서울', '서울 강남구 테헤란로 29', '/images/stays/guesthouse-4.jpg', 74000
      UNION ALL SELECT 'GUESTHOUSE', 'GWANGJU', '별빛 게스트하우스 광주', '광주 남구 양림로 130', '/images/stays/guesthouse-5.jpg', 56000
      UNION ALL SELECT 'GUESTHOUSE', 'SEOUL', '쉼표 게스트하우스 서울', '서울 종로구 종로 39', '/images/stays/guesthouse-6.jpg', 108000
) s
-- 팀원 예시와 이름이 같은 숙소(온유 풀빌라 제주)가 있어서, 숨김이 아닌(LISTED) 숙소만 중복으로 본다.
WHERE NOT EXISTS (SELECT 1 FROM accommodations a WHERE a.name = s.name AND a.listing_status = 'LISTED');

-- 5-2) 대표 사진 (프론트 public/images/stays 폴더의 파일. DB에는 경로만 저장)
INSERT INTO accommodation_images (accommodation_id, image_url, sort_order)
SELECT a.id, s.image_url, 0
FROM accommodations a
JOIN (
      SELECT 'MOTEL' AS category_code, 'GANGWON' AS region_code, '루나 모텔 강원' AS name, '강원 속초시 해오름로 331' AS address, '/images/stays/motel-1.jpg' AS image_url, 106000 AS price
      UNION ALL SELECT 'MOTEL', 'GANGWON', '모먼트 모텔 강원', '강원 속초시 해오름로 41', '/images/stays/motel-2.jpg', 59000
      UNION ALL SELECT 'MOTEL', 'JEJU', '블루문 모텔 제주', '제주 제주시 구좌읍 해맞이해안로 191', '/images/stays/motel-3.jpg', 105000
      UNION ALL SELECT 'MOTEL', 'GWANGJU', '어반 모텔 광주', '광주 북구 첨단과기로 333', '/images/stays/motel-4.jpg', 111000
      UNION ALL SELECT 'MOTEL', 'GANGWON', '라온 모텔 강원', '강원 양양군 현남면 해변길 52', '/images/stays/motel-5.jpg', 141000
      UNION ALL SELECT 'MOTEL', 'SEOUL', '시그니처 모텔 서울', '서울 용산구 이태원로 30', '/images/stays/motel-6.jpg', 68000
      UNION ALL SELECT 'HOTEL', 'GANGWON', '시온 시티 호텔 강원', '강원 양양군 현남면 해변길 164', '/images/stays/hotel-1.jpg', 149000
      UNION ALL SELECT 'HOTEL', 'SEOUL', '해담 호텔 서울', '서울 용산구 이태원로 196', '/images/stays/hotel-2.jpg', 91000
      UNION ALL SELECT 'HOTEL', 'GWANGJU', '라벤더 호텔 광주', '광주 남구 양림로 284', '/images/stays/hotel-3.jpg', 127000
      UNION ALL SELECT 'HOTEL', 'GANGWON', '더 스카이 호텔 강원', '강원 속초시 해오름로 374', '/images/stays/hotel-4.jpg', 145000
      UNION ALL SELECT 'HOTEL', 'SEOUL', '오로라 호텔 서울', '서울 중구 을지로 80', '/images/stays/hotel-5.jpg', 102000
      UNION ALL SELECT 'HOTEL', 'JEJU', '메리안 호텔 제주', '제주 서귀포시 성산읍 일출로 353', '/images/stays/hotel-6.jpg', 60000
      UNION ALL SELECT 'PRIVATE_HOUSE', 'JEJU', '솔숲 독채 제주', '제주 제주시 구좌읍 해맞이해안로 365', '/images/stays/private-house-1.jpg', 95000
      UNION ALL SELECT 'PRIVATE_HOUSE', 'JEJU', '돌담 독채 제주', '제주 제주시 애월읍 애월로 71', '/images/stays/private-house-2.jpg', 104000
      UNION ALL SELECT 'PRIVATE_HOUSE', 'SEOUL', '하늘채 독채 서울', '서울 마포구 와우산로 326', '/images/stays/private-house-3.jpg', 58000
      UNION ALL SELECT 'PRIVATE_HOUSE', 'DAEGU', '고요 독채 대구', '대구 수성구 들안로 201', '/images/stays/private-house-4.jpg', 128000
      UNION ALL SELECT 'PRIVATE_HOUSE', 'JEJU', '느린별 독채 제주', '제주 제주시 애월읍 애월로 280', '/images/stays/private-house-5.jpg', 123000
      UNION ALL SELECT 'PRIVATE_HOUSE', 'SEOUL', '마루 독채 서울', '서울 강남구 테헤란로 357', '/images/stays/private-house-6.jpg', 77000
      UNION ALL SELECT 'POOL_VILLA', 'JEJU', '온유 풀빌라 제주', '제주 서귀포시 중문관광로 382', '/images/stays/pool-villa-1.jpg', 78000
      UNION ALL SELECT 'POOL_VILLA', 'GWANGJU', '숲결 풀빌라 광주', '광주 동구 무등로 331', '/images/stays/pool-villa-2.jpg', 55000
      UNION ALL SELECT 'POOL_VILLA', 'DAEGU', '아쿠아 풀빌라 대구', '대구 중구 동성로 172', '/images/stays/pool-villa-3.jpg', 69000
      UNION ALL SELECT 'POOL_VILLA', 'JEJU', '윤슬 풀빌라 제주', '제주 제주시 구좌읍 해맞이해안로 134', '/images/stays/pool-villa-4.jpg', 99000
      UNION ALL SELECT 'POOL_VILLA', 'DAEGU', '파란 풀빌라 대구', '대구 수성구 들안로 61', '/images/stays/pool-villa-5.jpg', 123000
      UNION ALL SELECT 'POOL_VILLA', 'JEJU', '썸머 풀빌라 제주', '제주 서귀포시 성산읍 일출로 381', '/images/stays/pool-villa-6.jpg', 75000
      UNION ALL SELECT 'EMOTIONAL_STAY', 'DAEGU', '온유 스테이 대구', '대구 중구 달구벌대로 112', '/images/stays/emotional-stay-1.jpg', 85000
      UNION ALL SELECT 'EMOTIONAL_STAY', 'SEOUL', '서가 스테이 서울', '서울 마포구 와우산로 128', '/images/stays/emotional-stay-2.jpg', 142000
      UNION ALL SELECT 'EMOTIONAL_STAY', 'JEJU', '오후 스테이 제주', '제주 서귀포시 성산읍 일출로 26', '/images/stays/emotional-stay-3.jpg', 86000
      UNION ALL SELECT 'EMOTIONAL_STAY', 'JEJU', '결 스테이 제주', '제주 제주시 애월읍 애월로 399', '/images/stays/emotional-stay-4.jpg', 76000
      UNION ALL SELECT 'EMOTIONAL_STAY', 'JEJU', '머묾 스테이 제주', '제주 제주시 애월읍 애월로 105', '/images/stays/emotional-stay-5.jpg', 60000
      UNION ALL SELECT 'EMOTIONAL_STAY', 'JEJU', '포근 스테이 제주', '제주 제주시 애월읍 애월로 84', '/images/stays/emotional-stay-6.jpg', 119000
      UNION ALL SELECT 'GUESTHOUSE', 'SEOUL', '여행자의 집 서울', '서울 마포구 와우산로 23', '/images/stays/guesthouse-1.jpg', 62000
      UNION ALL SELECT 'GUESTHOUSE', 'SEOUL', '양림 게스트하우스 서울', '서울 용산구 이태원로 300', '/images/stays/guesthouse-2.jpg', 86000
      UNION ALL SELECT 'GUESTHOUSE', 'GANGWON', '바람 게스트하우스 강원', '강원 춘천시 호반로 201', '/images/stays/guesthouse-3.jpg', 70000
      UNION ALL SELECT 'GUESTHOUSE', 'SEOUL', '골목 게스트하우스 서울', '서울 강남구 테헤란로 29', '/images/stays/guesthouse-4.jpg', 74000
      UNION ALL SELECT 'GUESTHOUSE', 'GWANGJU', '별빛 게스트하우스 광주', '광주 남구 양림로 130', '/images/stays/guesthouse-5.jpg', 56000
      UNION ALL SELECT 'GUESTHOUSE', 'SEOUL', '쉼표 게스트하우스 서울', '서울 종로구 종로 39', '/images/stays/guesthouse-6.jpg', 108000
) s ON s.name = a.name
WHERE NOT EXISTS (SELECT 1 FROM accommodation_images i WHERE i.accommodation_id = a.id);

-- 5-3) 기본 객실 1개. nightly_price가 카드에 보이는 "최저 1박 요금"이 된다. (50,000 ~ 150,000원)
INSERT INTO rooms (accommodation_id, name, capacity, max_capacity, extra_person_fee, nightly_price, status)
SELECT a.id, '기본 객실', 2, 4, 10000.00, s.price, 'ACTIVE'
FROM accommodations a
JOIN (
      SELECT 'MOTEL' AS category_code, 'GANGWON' AS region_code, '루나 모텔 강원' AS name, '강원 속초시 해오름로 331' AS address, '/images/stays/motel-1.jpg' AS image_url, 106000 AS price
      UNION ALL SELECT 'MOTEL', 'GANGWON', '모먼트 모텔 강원', '강원 속초시 해오름로 41', '/images/stays/motel-2.jpg', 59000
      UNION ALL SELECT 'MOTEL', 'JEJU', '블루문 모텔 제주', '제주 제주시 구좌읍 해맞이해안로 191', '/images/stays/motel-3.jpg', 105000
      UNION ALL SELECT 'MOTEL', 'GWANGJU', '어반 모텔 광주', '광주 북구 첨단과기로 333', '/images/stays/motel-4.jpg', 111000
      UNION ALL SELECT 'MOTEL', 'GANGWON', '라온 모텔 강원', '강원 양양군 현남면 해변길 52', '/images/stays/motel-5.jpg', 141000
      UNION ALL SELECT 'MOTEL', 'SEOUL', '시그니처 모텔 서울', '서울 용산구 이태원로 30', '/images/stays/motel-6.jpg', 68000
      UNION ALL SELECT 'HOTEL', 'GANGWON', '시온 시티 호텔 강원', '강원 양양군 현남면 해변길 164', '/images/stays/hotel-1.jpg', 149000
      UNION ALL SELECT 'HOTEL', 'SEOUL', '해담 호텔 서울', '서울 용산구 이태원로 196', '/images/stays/hotel-2.jpg', 91000
      UNION ALL SELECT 'HOTEL', 'GWANGJU', '라벤더 호텔 광주', '광주 남구 양림로 284', '/images/stays/hotel-3.jpg', 127000
      UNION ALL SELECT 'HOTEL', 'GANGWON', '더 스카이 호텔 강원', '강원 속초시 해오름로 374', '/images/stays/hotel-4.jpg', 145000
      UNION ALL SELECT 'HOTEL', 'SEOUL', '오로라 호텔 서울', '서울 중구 을지로 80', '/images/stays/hotel-5.jpg', 102000
      UNION ALL SELECT 'HOTEL', 'JEJU', '메리안 호텔 제주', '제주 서귀포시 성산읍 일출로 353', '/images/stays/hotel-6.jpg', 60000
      UNION ALL SELECT 'PRIVATE_HOUSE', 'JEJU', '솔숲 독채 제주', '제주 제주시 구좌읍 해맞이해안로 365', '/images/stays/private-house-1.jpg', 95000
      UNION ALL SELECT 'PRIVATE_HOUSE', 'JEJU', '돌담 독채 제주', '제주 제주시 애월읍 애월로 71', '/images/stays/private-house-2.jpg', 104000
      UNION ALL SELECT 'PRIVATE_HOUSE', 'SEOUL', '하늘채 독채 서울', '서울 마포구 와우산로 326', '/images/stays/private-house-3.jpg', 58000
      UNION ALL SELECT 'PRIVATE_HOUSE', 'DAEGU', '고요 독채 대구', '대구 수성구 들안로 201', '/images/stays/private-house-4.jpg', 128000
      UNION ALL SELECT 'PRIVATE_HOUSE', 'JEJU', '느린별 독채 제주', '제주 제주시 애월읍 애월로 280', '/images/stays/private-house-5.jpg', 123000
      UNION ALL SELECT 'PRIVATE_HOUSE', 'SEOUL', '마루 독채 서울', '서울 강남구 테헤란로 357', '/images/stays/private-house-6.jpg', 77000
      UNION ALL SELECT 'POOL_VILLA', 'JEJU', '온유 풀빌라 제주', '제주 서귀포시 중문관광로 382', '/images/stays/pool-villa-1.jpg', 78000
      UNION ALL SELECT 'POOL_VILLA', 'GWANGJU', '숲결 풀빌라 광주', '광주 동구 무등로 331', '/images/stays/pool-villa-2.jpg', 55000
      UNION ALL SELECT 'POOL_VILLA', 'DAEGU', '아쿠아 풀빌라 대구', '대구 중구 동성로 172', '/images/stays/pool-villa-3.jpg', 69000
      UNION ALL SELECT 'POOL_VILLA', 'JEJU', '윤슬 풀빌라 제주', '제주 제주시 구좌읍 해맞이해안로 134', '/images/stays/pool-villa-4.jpg', 99000
      UNION ALL SELECT 'POOL_VILLA', 'DAEGU', '파란 풀빌라 대구', '대구 수성구 들안로 61', '/images/stays/pool-villa-5.jpg', 123000
      UNION ALL SELECT 'POOL_VILLA', 'JEJU', '썸머 풀빌라 제주', '제주 서귀포시 성산읍 일출로 381', '/images/stays/pool-villa-6.jpg', 75000
      UNION ALL SELECT 'EMOTIONAL_STAY', 'DAEGU', '온유 스테이 대구', '대구 중구 달구벌대로 112', '/images/stays/emotional-stay-1.jpg', 85000
      UNION ALL SELECT 'EMOTIONAL_STAY', 'SEOUL', '서가 스테이 서울', '서울 마포구 와우산로 128', '/images/stays/emotional-stay-2.jpg', 142000
      UNION ALL SELECT 'EMOTIONAL_STAY', 'JEJU', '오후 스테이 제주', '제주 서귀포시 성산읍 일출로 26', '/images/stays/emotional-stay-3.jpg', 86000
      UNION ALL SELECT 'EMOTIONAL_STAY', 'JEJU', '결 스테이 제주', '제주 제주시 애월읍 애월로 399', '/images/stays/emotional-stay-4.jpg', 76000
      UNION ALL SELECT 'EMOTIONAL_STAY', 'JEJU', '머묾 스테이 제주', '제주 제주시 애월읍 애월로 105', '/images/stays/emotional-stay-5.jpg', 60000
      UNION ALL SELECT 'EMOTIONAL_STAY', 'JEJU', '포근 스테이 제주', '제주 제주시 애월읍 애월로 84', '/images/stays/emotional-stay-6.jpg', 119000
      UNION ALL SELECT 'GUESTHOUSE', 'SEOUL', '여행자의 집 서울', '서울 마포구 와우산로 23', '/images/stays/guesthouse-1.jpg', 62000
      UNION ALL SELECT 'GUESTHOUSE', 'SEOUL', '양림 게스트하우스 서울', '서울 용산구 이태원로 300', '/images/stays/guesthouse-2.jpg', 86000
      UNION ALL SELECT 'GUESTHOUSE', 'GANGWON', '바람 게스트하우스 강원', '강원 춘천시 호반로 201', '/images/stays/guesthouse-3.jpg', 70000
      UNION ALL SELECT 'GUESTHOUSE', 'SEOUL', '골목 게스트하우스 서울', '서울 강남구 테헤란로 29', '/images/stays/guesthouse-4.jpg', 74000
      UNION ALL SELECT 'GUESTHOUSE', 'GWANGJU', '별빛 게스트하우스 광주', '광주 남구 양림로 130', '/images/stays/guesthouse-5.jpg', 56000
      UNION ALL SELECT 'GUESTHOUSE', 'SEOUL', '쉼표 게스트하우스 서울', '서울 종로구 종로 39', '/images/stays/guesthouse-6.jpg', 108000
) s ON s.name = a.name
WHERE NOT EXISTS (SELECT 1 FROM rooms r WHERE r.accommodation_id = a.id);

-- 5-4) 날짜별 재고: V100과 같은 기간(2026-12-01 ~ 2027-03-31). 팀원 검색의 날짜·인원 조건에도 걸리게 한다.
INSERT INTO room_inventories (room_id, stay_date, total_count, booked_count, held_count, price, is_closed)
WITH RECURSIVE d AS (
    SELECT CAST('2026-12-01' AS DATE) AS dt
    UNION ALL
    SELECT dt + INTERVAL 1 DAY FROM d WHERE dt < CAST('2027-03-31' AS DATE)
)
SELECT r.id, d.dt, 1, 0, 0, r.nightly_price, 0
FROM rooms r
CROSS JOIN d
WHERE NOT EXISTS (SELECT 1 FROM room_inventories i WHERE i.room_id = r.id);
