-- Local fictional examples only; all inserts transactional; do not rerun.
USE preen_local_moon_erd;
START TRANSACTION;
INSERT INTO rooms(id,accommodation_id,name,capacity,max_capacity,extra_person_fee,nightly_price,status) VALUES
(900004,900001,'디럭스 더블',2,3,25000,185000,'ACTIVE'),
(900005,900001,'테라스 스위트',2,4,30000,280000,'ACTIVE'),
(900006,900001,'프라이빗 풀 스위트',2,4,35000,390000,'ACTIVE'),
(900007,900001,'패밀리 온돌',4,6,15000,240000,'ACTIVE');
INSERT INTO room_specs(room_id,room_type,area_sqm) VALUES
(900001,'스탠다드 더블',28),(900002,'패밀리 트윈',45),(900004,'디럭스 더블',36),
(900005,'테라스 스위트',56),(900006,'풀 스위트',72),(900007,'온돌',52);
INSERT INTO room_beds(room_id,bed_type,bed_count,sort_order) VALUES
(900001,'DOUBLE',1,1),(900002,'DOUBLE',1,1),(900002,'SINGLE',2,2),(900004,'QUEEN',1,1),
(900005,'KING',1,1),(900005,'SINGLE',1,2),(900006,'KING',1,1),(900007,'FLOOR_BEDDING',4,1);
INSERT INTO room_facilities(id,code,name) VALUES
(900001,'WIFI','무료 Wi-Fi'),(900002,'AIR_CONDITIONER','에어컨'),(900003,'BATHTUB','욕조'),
(900004,'TERRACE','전용 테라스'),(900005,'PRIVATE_POOL','개별 수영장'),(900006,'KITCHEN','취사 시설'),
(900007,'COFFEE_MACHINE','커피 머신'),(900008,'FRIDGE','냉장고'),(900009,'TV','TV');
INSERT INTO room_facility_links(room_id,facility_id)
SELECT id,900001 FROM rooms WHERE id IN(900001,900002,900004,900005,900006,900007);
INSERT INTO room_facility_links(room_id,facility_id)
SELECT id,900002 FROM rooms WHERE id IN(900001,900002,900004,900005,900006,900007);
INSERT INTO room_facility_links(room_id,facility_id) VALUES
(900001,900008),(900002,900008),(900002,900009),(900004,900003),(900004,900007),
(900005,900004),(900005,900003),(900005,900007),(900006,900005),(900006,900004),
(900006,900007),(900007,900006),(900007,900008),(900007,900009);
INSERT INTO room_images(id,room_id,image_url,sort_order) VALUES
(900004,900004,'/images/generated-1.png',1),(900005,900005,'/images/generated-3.png',1),
(900006,900006,'/images/generated.png',1),(900007,900007,'/images/generated-2.png',1);
COMMIT;
