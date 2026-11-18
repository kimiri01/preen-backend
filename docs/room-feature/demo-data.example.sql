-- OPTIONAL: fictional verification data for a NEW isolated local DB only.
-- Never run against a team/shared/production DB. Run once after Flyway V1-V101.
-- Existing records are not deleted or updated. Fail on ID collision.
START TRANSACTION;
SET @moon_stay = (SELECT id FROM accommodations WHERE name='온유 풀빌라 제주' AND listing_status='LISTED' ORDER BY id DESC LIMIT 1);
SET @moon_room = (SELECT id FROM rooms WHERE accommodation_id=@moon_stay AND status='ACTIVE' ORDER BY id LIMIT 1);
INSERT INTO room_specs(room_id,room_type,area_sqm) VALUES(@moon_room,'풀빌라',56);
INSERT INTO room_beds(room_id,bed_type,bed_count,sort_order) VALUES(@moon_room,'DOUBLE',1,1);
INSERT INTO room_facilities(id,code,name) VALUES(901100,'MOON_DEMO_WIFI','Wi-Fi');
INSERT INTO room_facility_links(room_id,facility_id) VALUES(@moon_room,901100);
INSERT INTO room_audio(room_id,equipment_name,connection_guide,available_from,available_until,volume_guide,spotify_track_id)
VALUES(@moon_room,'예시 스테레오','Spotify 플레이어 재생 기기를 Bluetooth로 연결하세요.','09:00','22:00','적정 음량으로 이용해주세요.','4Dr2hJ3EnVh2Aaot6fRwDO');
INSERT INTO users(id,role,status,created_at,updated_at) VALUES
(901101,'GUEST','ACTIVE',NOW(),NOW()),(901102,'GUEST','ACTIVE',NOW(),NOW()),(901103,'GUEST','ACTIVE',NOW(),NOW()),(901104,'GUEST','ACTIVE',NOW(),NOW());
INSERT INTO refund_policies(id,accommodation_id,version,policy_text,active) VALUES(901100,@moon_stay,'MOON-DEMO','가상 리뷰 검증 정책',FALSE);
INSERT INTO reservations(id,reservation_no,user_id,room_id,refund_policy_id,check_in,check_out,guest_count,guest_name,total_price,status,refund_policy_snapshot,hold_expires_at,created_at) VALUES
(901101,'MOON-INTEGRATION-DEMO-1',901101,@moon_room,901100,'2026-10-17','2026-10-18',2,'최예시',100000,'COMPLETED','{"demo":true}','2026-10-17 18:00:00',NOW()),
(901102,'MOON-INTEGRATION-DEMO-2',901102,@moon_room,901100,'2026-10-14','2026-10-15',2,'박예시',100000,'COMPLETED','{"demo":true}','2026-10-14 18:00:00',NOW()),
(901103,'MOON-INTEGRATION-DEMO-3',901103,@moon_room,901100,'2026-10-10','2026-10-11',2,'김예시',100000,'COMPLETED','{"demo":true}','2026-10-10 18:00:00',NOW()),
(901104,'MOON-INTEGRATION-DEMO-4',901104,@moon_room,901100,'2026-10-07','2026-10-08',2,'이예시',100000,'COMPLETED','{"demo":true}','2026-10-07 18:00:00',NOW());
INSERT INTO reviews(id,reservation_id,accommodation_id,user_id,spotify_track_id,rating,status,created_at) VALUES
(901101,901101,@moon_stay,901101,'4Dr2hJ3EnVh2Aaot6fRwDO',5,'VISIBLE','2026-10-18 12:00:00'),
(901102,901102,@moon_stay,901102,'7HrE6HtYNBbGqp5GmHbFV0',3,'VISIBLE','2026-10-15 12:00:00'),
(901103,901103,@moon_stay,901103,'3P3UA61WRQqwCXaoFOTENd',5,'VISIBLE','2026-10-11 12:00:00'),
(901104,901104,@moon_stay,901104,'0i2XXofKXlXbZ2sjnh06Jj',4,'VISIBLE','2026-10-08 12:00:00');
COMMIT;
