-- [kny] 홈 검색 바 날짜 검색용 재고 보충
-- V101의 재고는 2026-12-01 ~ 2027-03-31이라, 2026년 11월 날짜로 검색하면 결과가 0건이었다.
-- 메인 화면 숙소(코드가 있는 카테고리 + 공개 LISTED)의 판매 중 객실에
-- 2026-11-01 ~ 2026-11-30 재고를 V101과 같은 규칙(객실 1개, 가격 = 객실 기본가)으로 "추가"만 한다.
-- 기존 행·V100·V101은 고치지 않으며, 이미 같은 날짜 재고가 있으면 건너뛴다.
INSERT INTO room_inventories (room_id, stay_date, total_count, booked_count, held_count, price, is_closed)
WITH RECURSIVE d AS (
    SELECT CAST('2026-11-01' AS DATE) AS dt
    UNION ALL
    SELECT dt + INTERVAL 1 DAY FROM d WHERE dt < CAST('2026-11-30' AS DATE)
)
SELECT r.id, d.dt, 1, 0, 0, r.nightly_price, 0
FROM rooms r
JOIN accommodations a ON a.id = r.accommodation_id
JOIN accommodation_categories c ON c.id = a.category_id
CROSS JOIN d
WHERE r.status = 'ACTIVE'
  AND a.listing_status = 'LISTED'
  AND c.code IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM room_inventories i WHERE i.room_id = r.id AND i.stay_date = d.dt);
