-- 개발용 예시 데이터: 호스트 사업자, 숙소 13건, 대표 사진, 객실, 날짜별 재고(2026-12-01 ~ 2027-03-31)
-- 이전에는 서버 시작 시 Java 코드가 넣었지만, 객실·재고가 숙소보다 먼저 만들어지도록 마이그레이션으로 옮겼다.
-- 모든 INSERT는 이미 있으면 건너뛰므로 여러 번 실행해도 중복되지 않는다.

-- 예시 호스트 사업자 (사업자등록번호 0000000000으로 식별)
INSERT INTO users (role, status, created_at, updated_at)
SELECT 'HOST', 'ACTIVE', NOW(6), NOW(6) FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM host_businesses WHERE business_registration_no = '0000000000');

INSERT INTO host_businesses (host_user_id, business_name, business_registration_no, approval_status, created_at)
SELECT (SELECT MAX(id) FROM users WHERE role = 'HOST'), '예시 숙소 사업자', '0000000000', 'APPROVED', NOW(6) FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM host_businesses WHERE business_registration_no = '0000000000');

-- 예시 숙소 13건 (지역별: 서울 3, 강원 3, 대구 2, 광주 2, 제주 3)
INSERT INTO accommodations (host_business_id, region_id, category_id, name, address, check_in_time, check_out_time, listing_status, created_at)
SELECT (SELECT id FROM host_businesses WHERE business_registration_no = '0000000000'),
       (SELECT id FROM regions WHERE name = '서울' AND level = 1),
       (SELECT id FROM accommodation_categories WHERE name = '모텔'),
       '스테이 인 종로', '서울 종로구 종로 120', '15:00:00', '11:00:00', 'LISTED', NOW(6) FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM accommodations WHERE name = '스테이 인 종로');

INSERT INTO accommodations (host_business_id, region_id, category_id, name, address, check_in_time, check_out_time, listing_status, created_at)
SELECT (SELECT id FROM host_businesses WHERE business_registration_no = '0000000000'),
       (SELECT id FROM regions WHERE name = '대구' AND level = 1),
       (SELECT id FROM accommodation_categories WHERE name = '모텔'),
       '동성로 루미 모텔', '대구 중구 동성로 45', '15:00:00', '11:00:00', 'LISTED', NOW(6) FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM accommodations WHERE name = '동성로 루미 모텔');

INSERT INTO accommodations (host_business_id, region_id, category_id, name, address, check_in_time, check_out_time, listing_status, created_at)
SELECT (SELECT id FROM host_businesses WHERE business_registration_no = '0000000000'),
       (SELECT id FROM regions WHERE name = '서울' AND level = 1),
       (SELECT id FROM accommodation_categories WHERE name = '호텔·리조트'),
       '시온 시티 호텔', '서울 중구 을지로 88', '15:00:00', '11:00:00', 'LISTED', NOW(6) FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM accommodations WHERE name = '시온 시티 호텔');

INSERT INTO accommodations (host_business_id, region_id, category_id, name, address, check_in_time, check_out_time, listing_status, created_at)
SELECT (SELECT id FROM host_businesses WHERE business_registration_no = '0000000000'),
       (SELECT id FROM regions WHERE name = '강원' AND level = 1),
       (SELECT id FROM accommodation_categories WHERE name = '호텔·리조트'),
       '시온 오션 호텔', '강원 강릉시 창해로 17', '15:00:00', '11:00:00', 'LISTED', NOW(6) FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM accommodations WHERE name = '시온 오션 호텔');

INSERT INTO accommodations (host_business_id, region_id, category_id, name, address, check_in_time, check_out_time, listing_status, created_at)
SELECT (SELECT id FROM host_businesses WHERE business_registration_no = '0000000000'),
       (SELECT id FROM regions WHERE name = '제주' AND level = 1),
       (SELECT id FROM accommodation_categories WHERE name = '호텔·리조트'),
       '해담 호텔 제주', '제주 서귀포시 중문관광로 72', '15:00:00', '11:00:00', 'LISTED', NOW(6) FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM accommodations WHERE name = '해담 호텔 제주');

INSERT INTO accommodations (host_business_id, region_id, category_id, name, address, check_in_time, check_out_time, listing_status, created_at)
SELECT (SELECT id FROM host_businesses WHERE business_registration_no = '0000000000'),
       (SELECT id FROM regions WHERE name = '강원' AND level = 1),
       (SELECT id FROM accommodation_categories WHERE name = '한옥'),
       '솔숲 독채 평창', '강원 평창군 봉평면 솔숲길 21', '15:00:00', '11:00:00', 'LISTED', NOW(6) FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM accommodations WHERE name = '솔숲 독채 평창');

INSERT INTO accommodations (host_business_id, region_id, category_id, name, address, check_in_time, check_out_time, listing_status, created_at)
SELECT (SELECT id FROM host_businesses WHERE business_registration_no = '0000000000'),
       (SELECT id FROM regions WHERE name = '광주' AND level = 1),
       (SELECT id FROM accommodation_categories WHERE name = '한옥'),
       '무등 독채', '광주 동구 무등로 310', '15:00:00', '11:00:00', 'LISTED', NOW(6) FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM accommodations WHERE name = '무등 독채');

INSERT INTO accommodations (host_business_id, region_id, category_id, name, address, check_in_time, check_out_time, listing_status, created_at)
SELECT (SELECT id FROM host_businesses WHERE business_registration_no = '0000000000'),
       (SELECT id FROM regions WHERE name = '제주' AND level = 1),
       (SELECT id FROM accommodation_categories WHERE name = '독채·풀빌라'),
       '온유 풀빌라 제주', '제주 제주시 애월읍 애월로 55', '15:00:00', '11:00:00', 'LISTED', NOW(6) FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM accommodations WHERE name = '온유 풀빌라 제주');

INSERT INTO accommodations (host_business_id, region_id, category_id, name, address, check_in_time, check_out_time, listing_status, created_at)
SELECT (SELECT id FROM host_businesses WHERE business_registration_no = '0000000000'),
       (SELECT id FROM regions WHERE name = '강원' AND level = 1),
       (SELECT id FROM accommodation_categories WHERE name = '독채·풀빌라'),
       '숲결 풀빌라 양양', '강원 양양군 현남면 숲길 12', '15:00:00', '11:00:00', 'LISTED', NOW(6) FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM accommodations WHERE name = '숲결 풀빌라 양양');

INSERT INTO accommodations (host_business_id, region_id, category_id, name, address, check_in_time, check_out_time, listing_status, created_at)
SELECT (SELECT id FROM host_businesses WHERE business_registration_no = '0000000000'),
       (SELECT id FROM regions WHERE name = '제주' AND level = 1),
       (SELECT id FROM accommodation_categories WHERE name = '펜션'),
       '온유 스테이 제주', '제주 제주시 구좌읍 해맞이해안로 400', '15:00:00', '11:00:00', 'LISTED', NOW(6) FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM accommodations WHERE name = '온유 스테이 제주');

INSERT INTO accommodations (host_business_id, region_id, category_id, name, address, check_in_time, check_out_time, listing_status, created_at)
SELECT (SELECT id FROM host_businesses WHERE business_registration_no = '0000000000'),
       (SELECT id FROM regions WHERE name = '대구' AND level = 1),
       (SELECT id FROM accommodation_categories WHERE name = '펜션'),
       '서가 스테이 대구', '대구 중구 달구벌대로 2100', '15:00:00', '11:00:00', 'LISTED', NOW(6) FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM accommodations WHERE name = '서가 스테이 대구');

INSERT INTO accommodations (host_business_id, region_id, category_id, name, address, check_in_time, check_out_time, listing_status, created_at)
SELECT (SELECT id FROM host_businesses WHERE business_registration_no = '0000000000'),
       (SELECT id FROM regions WHERE name = '서울' AND level = 1),
       (SELECT id FROM accommodation_categories WHERE name = '게스트하우스'),
       '여행자의 집 서촌', '서울 종로구 자하문로 40', '15:00:00', '11:00:00', 'LISTED', NOW(6) FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM accommodations WHERE name = '여행자의 집 서촌');

INSERT INTO accommodations (host_business_id, region_id, category_id, name, address, check_in_time, check_out_time, listing_status, created_at)
SELECT (SELECT id FROM host_businesses WHERE business_registration_no = '0000000000'),
       (SELECT id FROM regions WHERE name = '광주' AND level = 1),
       (SELECT id FROM accommodation_categories WHERE name = '게스트하우스'),
       '양림 게스트하우스', '광주 남구 양림로 66', '15:00:00', '11:00:00', 'LISTED', NOW(6) FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM accommodations WHERE name = '양림 게스트하우스');

-- 대표 사진 (sort_order 0). 사진이 아직 없는 숙소에만 넣는다.
INSERT INTO accommodation_images (accommodation_id, image_url, sort_order)
SELECT a.id,
       CASE a.name
           WHEN '스테이 인 종로' THEN '/images/generated-3.png'
           WHEN '동성로 루미 모텔' THEN '/images/generated-3.png'
           WHEN '시온 시티 호텔' THEN '/images/generated-1.png'
           WHEN '시온 오션 호텔' THEN '/images/generated-1.png'
           WHEN '해담 호텔 제주' THEN '/images/generated-1.png'
           WHEN '솔숲 독채 평창' THEN '/images/generated-2.png'
           WHEN '무등 독채' THEN '/images/generated-2.png'
           WHEN '온유 풀빌라 제주' THEN '/images/generated.png'
           WHEN '숲결 풀빌라 양양' THEN '/images/generated.png'
           WHEN '온유 스테이 제주' THEN '/images/generated.png'
           WHEN '서가 스테이 대구' THEN '/images/generated-2.png'
           WHEN '여행자의 집 서촌' THEN '/images/generated-3.png'
           WHEN '양림 게스트하우스' THEN '/images/generated-3.png'
       END,
       0
FROM accommodations a
WHERE a.name IN ('스테이 인 종로', '동성로 루미 모텔', '시온 시티 호텔', '시온 오션 호텔', '해담 호텔 제주',
                 '솔숲 독채 평창', '무등 독채', '온유 풀빌라 제주', '숲결 풀빌라 양양', '온유 스테이 제주',
                 '서가 스테이 대구', '여행자의 집 서촌', '양림 게스트하우스')
  AND NOT EXISTS (SELECT 1 FROM accommodation_images i WHERE i.accommodation_id = a.id);

-- 객실: 숙소마다 기본 객실 1개. 기본가는 숙소 id에 따라 5만~9만 원.
INSERT INTO rooms (accommodation_id, name, capacity, max_capacity, extra_person_fee, nightly_price, status)
SELECT a.id, '기본 객실', 2, 4, 10000.00, 50000.00 + (a.id % 5) * 10000.00, 'ACTIVE'
FROM accommodations a
WHERE NOT EXISTS (SELECT 1 FROM rooms r WHERE r.accommodation_id = a.id);

-- 날짜별 재고: 2026-12-01 ~ 2027-03-31, 객실당 1실, 판매 중
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
