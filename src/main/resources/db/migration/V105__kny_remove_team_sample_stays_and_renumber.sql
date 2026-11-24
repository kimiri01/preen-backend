-- [kny] 팀원 예시 숙소(V100, 13건) 삭제 + 남은 숙소 ID를 1번부터 다시 매기기
--
-- 왜 새 마이그레이션인가?
--   이미 실행된 V100·V101을 고치면 Flyway 체크섬이 달라져 모든 팀원의 서버가 실행되지 않는다.
--   그래서 기존 파일은 그대로 두고, "지우고 번호를 다시 매기는" 작업을 이 파일에서 한다.
--   새 DB에서도 V100(1~13) → V101(14~49) → V105(삭제 후 1~36) 순서로 실행되어 결과가 같다.
--
-- 대상: V101에서 숨김(HIDDEN) 처리한 V100 예시 숙소 13건 (이름 + 숨김 + 예시 사업자로 정확히 지정)
--   메인용 숙소 중 이름이 같은 '온유 풀빌라 제주'는 공개(LISTED)라 대상이 아니다.

-- =====================================================================
-- 1) 삭제할 숙소와, 그 숙소에 딸린 객실·재고·예약·결제 ID를 임시 표에 모은다.
-- =====================================================================
CREATE TEMPORARY TABLE kny_del_accommodations AS
SELECT a.id
FROM accommodations a
JOIN host_businesses hb ON hb.id = a.host_business_id
WHERE a.listing_status = 'HIDDEN'
  AND hb.business_registration_no = '0000000000'
  AND a.name IN ('스테이 인 종로', '동성로 루미 모텔', '시온 시티 호텔', '시온 오션 호텔', '해담 호텔 제주',
                 '솔숲 독채 평창', '무등 독채', '온유 풀빌라 제주', '숲결 풀빌라 양양', '온유 스테이 제주',
                 '서가 스테이 대구', '여행자의 집 서촌', '양림 게스트하우스');

CREATE TEMPORARY TABLE kny_del_rooms AS
SELECT r.id FROM rooms r WHERE r.accommodation_id IN (SELECT id FROM kny_del_accommodations);

CREATE TEMPORARY TABLE kny_del_inventories AS
SELECT i.id FROM room_inventories i WHERE i.room_id IN (SELECT id FROM kny_del_rooms);

CREATE TEMPORARY TABLE kny_del_reservations AS
SELECT b.id FROM reservations b WHERE b.room_id IN (SELECT id FROM kny_del_rooms);

CREATE TEMPORARY TABLE kny_del_payments AS
SELECT p.id FROM payments p WHERE p.reservation_id IN (SELECT id FROM kny_del_reservations);

CREATE TEMPORARY TABLE kny_del_refund_policies AS
SELECT rp.id FROM refund_policies rp WHERE rp.accommodation_id IN (SELECT id FROM kny_del_accommodations);

-- =====================================================================
-- 2) 참조하는 쪽(자식)부터 지운다. (외래키 순서: 결제 → 예약 → 재고 → 객실 → 숙소)
--    예시 숙소에는 보통 예약·결제가 없지만, 팀원 로컬 DB에 테스트 데이터가 있어도 깨지지 않게 함께 지운다.
-- =====================================================================
DELETE FROM refunds WHERE payment_id IN (SELECT id FROM kny_del_payments);
DELETE FROM payment_logs WHERE payment_id IN (SELECT id FROM kny_del_payments);
DELETE FROM payments WHERE id IN (SELECT id FROM kny_del_payments);

DELETE FROM reservation_price_items WHERE reservation_id IN (SELECT id FROM kny_del_reservations);
DELETE FROM reservation_status_history WHERE reservation_id IN (SELECT id FROM kny_del_reservations);
DELETE FROM reviews WHERE reservation_id IN (SELECT id FROM kny_del_reservations);
DELETE FROM reviews WHERE accommodation_id IN (SELECT id FROM kny_del_accommodations);
DELETE FROM inventory_holds WHERE reservation_id IN (SELECT id FROM kny_del_reservations);
DELETE FROM inventory_holds WHERE room_inventory_id IN (SELECT id FROM kny_del_inventories);
DELETE FROM reservations WHERE id IN (SELECT id FROM kny_del_reservations);

DELETE FROM room_inventories WHERE id IN (SELECT id FROM kny_del_inventories);
DELETE FROM room_images WHERE room_id IN (SELECT id FROM kny_del_rooms);
DELETE FROM rooms WHERE id IN (SELECT id FROM kny_del_rooms);

DELETE FROM refund_policy_rules WHERE refund_policy_id IN (SELECT id FROM kny_del_refund_policies);
DELETE FROM refund_policies WHERE id IN (SELECT id FROM kny_del_refund_policies);

DELETE FROM favorites WHERE accommodation_id IN (SELECT id FROM kny_del_accommodations);
DELETE FROM accommodation_amenities WHERE accommodation_id IN (SELECT id FROM kny_del_accommodations);
DELETE FROM accommodation_images WHERE accommodation_id IN (SELECT id FROM kny_del_accommodations);
DELETE FROM accommodations WHERE id IN (SELECT id FROM kny_del_accommodations);

-- =====================================================================
-- 3) 남은 숙소 ID를 1번부터 다시 매긴다.
--    예: 14 → 1, 15 → 2, ... 49 → 36 (기존 ID 순서 그대로)
--    숙소 ID를 바꾸면 그 ID를 저장한 표(사진·편의시설·객실·환불정책·리뷰·찜)도 같이 바꿔야 한다.
-- =====================================================================
CREATE TEMPORARY TABLE kny_accommodation_id_map AS
SELECT id AS old_id, ROW_NUMBER() OVER (ORDER BY id) AS new_id
FROM accommodations;

-- 번호를 바꾸는 동안에는 숙소와 자식 표의 ID가 잠깐 어긋나므로 외래키 검사를 잠시 끈다. (마지막에 다시 켬)
SET FOREIGN_KEY_CHECKS = 0;

-- 3-1) 먼저 모든 ID에 100만을 더해 "임시 번호"로 옮긴다.
--      바로 1~36으로 바꾸면, 아직 안 바뀐 기존 ID(14~36)와 겹쳐서 중복 키 오류가 날 수 있기 때문이다.
UPDATE accommodations SET id = id + 1000000;
UPDATE accommodation_images SET accommodation_id = accommodation_id + 1000000;
UPDATE accommodation_amenities SET accommodation_id = accommodation_id + 1000000;
UPDATE rooms SET accommodation_id = accommodation_id + 1000000;
UPDATE refund_policies SET accommodation_id = accommodation_id + 1000000;
UPDATE reviews SET accommodation_id = accommodation_id + 1000000;
UPDATE favorites SET accommodation_id = accommodation_id + 1000000;

-- 3-2) 임시 번호를 새 번호(1~)로 바꾼다.
UPDATE accommodations t JOIN kny_accommodation_id_map m ON t.id = m.old_id + 1000000
SET t.id = m.new_id;
UPDATE accommodation_images t JOIN kny_accommodation_id_map m ON t.accommodation_id = m.old_id + 1000000
SET t.accommodation_id = m.new_id;
UPDATE accommodation_amenities t JOIN kny_accommodation_id_map m ON t.accommodation_id = m.old_id + 1000000
SET t.accommodation_id = m.new_id;
UPDATE rooms t JOIN kny_accommodation_id_map m ON t.accommodation_id = m.old_id + 1000000
SET t.accommodation_id = m.new_id;
UPDATE refund_policies t JOIN kny_accommodation_id_map m ON t.accommodation_id = m.old_id + 1000000
SET t.accommodation_id = m.new_id;
UPDATE reviews t JOIN kny_accommodation_id_map m ON t.accommodation_id = m.old_id + 1000000
SET t.accommodation_id = m.new_id;
UPDATE favorites t JOIN kny_accommodation_id_map m ON t.accommodation_id = m.old_id + 1000000
SET t.accommodation_id = m.new_id;

SET FOREIGN_KEY_CHECKS = 1;

-- 4) 다음에 등록되는 숙소가 37번(마지막 번호 + 1)부터 이어지도록 자동 증가값을 맞춘다.
--    (1로 지정하면 MySQL이 "현재 최대 ID + 1"로 자동 조정한다.)
ALTER TABLE accommodations AUTO_INCREMENT = 1;

DROP TEMPORARY TABLE kny_accommodation_id_map;
DROP TEMPORARY TABLE kny_del_refund_policies;
DROP TEMPORARY TABLE kny_del_payments;
DROP TEMPORARY TABLE kny_del_reservations;
DROP TEMPORARY TABLE kny_del_inventories;
DROP TEMPORARY TABLE kny_del_rooms;
DROP TEMPORARY TABLE kny_del_accommodations;
