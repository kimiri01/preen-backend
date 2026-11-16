-- LOCAL learning fixture only. Run once, no real users or bookings.
-- Fails on ID collision; no existing data is updated/deleted. All rows are transactional.
USE preen_local_moon_erd;
START TRANSACTION;
INSERT INTO users(id,role) VALUES(900001,'HOST');
INSERT INTO host_businesses(id,host_user_id,business_name,business_registration_no,approval_status)
VALUES(900001,900001,'객실 조회 가상 사업자','DEMO-ROOM-MOON','APPROVED');
INSERT INTO regions(id,name,level) VALUES(900001,'예시 지역',1);
INSERT INTO accommodations(id,host_business_id,region_id,category_id,name,address,check_in_time,check_out_time,listing_status)
SELECT 900001,900001,900001,id,'프린 스테이','예시 주소','15:00','11:00','LISTED'
FROM accommodation_categories ORDER BY id LIMIT 1;
INSERT INTO rooms(id,accommodation_id,name,capacity,max_capacity,extra_person_fee,nightly_price,status) VALUES
(900001,900001,'스탠다드 룸',2,2,0,150000,'ACTIVE'),
(900002,900001,'패밀리 룸',2,4,20000,220000,'ACTIVE'),
(900003,900001,'미노출 객실',2,2,0,100000,'INACTIVE');
INSERT INTO room_images(id,room_id,image_url,sort_order) VALUES
(900001,900001,'/images/generated-1.png',1),
(900002,900001,'/images/generated-3.png',2),
(900003,900002,'/images/generated-3.png',1);
COMMIT;
