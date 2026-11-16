-- User-approved fictional local stays/reviews for Must 13. Run once only.
-- IDs 901100-901104; fail on collision. Do not run on a team's existing DB.
-- Track IDs verified against official Spotify oEmbed on 2026-11-16.
USE preen_local_moon_erd;
START TRANSACTION;
INSERT INTO spotify_track_metadata(spotify_track_id,title,artist,album) VALUES
('4Dr2hJ3EnVh2Aaot6fRwDO','Blueming','아이유','Love poem'),
('7HrE6HtYNBbGqp5GmHbFV0','Love poem','아이유','Love poem'),
('3P3UA61WRQqwCXaoFOTENd','밤편지','아이유','Palette'),
('0i2XXofKXlXbZ2sjnh06Jj','우주를 건너','백예린','FRANK');
INSERT INTO users(id,role) VALUES(901101,'GUEST'),(901102,'GUEST'),(901103,'GUEST'),(901104,'GUEST');
INSERT INTO refund_policies(id,accommodation_id,version,policy_text,active)
VALUES(901100,900001,'MUST13-DEMO','예시 리뷰 재현을 위한 가상 이용 완료 예약 정책',FALSE);
INSERT INTO reservations(id,reservation_no,user_id,room_id,refund_policy_id,check_in,check_out,guest_count,guest_name,total_price,status,refund_policy_snapshot) VALUES
(901101,'MUST13-DEMO-01',901101,900001,901100,'2026-10-17','2026-10-18',2,'최예시',180000,'COMPLETED','{"demo":true}'),
(901102,'MUST13-DEMO-02',901102,900001,901100,'2026-10-14','2026-10-15',2,'박예시',180000,'COMPLETED','{"demo":true}'),
(901103,'MUST13-DEMO-03',901103,900001,901100,'2026-10-10','2026-10-11',2,'김예시',180000,'COMPLETED','{"demo":true}'),
(901104,'MUST13-DEMO-04',901104,900002,901100,'2026-10-07','2026-10-08',2,'이예시',210000,'COMPLETED','{"demo":true}');
INSERT INTO reviews(id,reservation_id,accommodation_id,user_id,spotify_track_id,rating,status,created_at) VALUES
(901101,901101,900001,901101,'4Dr2hJ3EnVh2Aaot6fRwDO',5,'VISIBLE','2026-10-18 12:00:00'),
(901102,901102,900001,901102,'7HrE6HtYNBbGqp5GmHbFV0',3,'VISIBLE','2026-10-15 12:00:00'),
(901103,901103,900001,901103,'3P3UA61WRQqwCXaoFOTENd',5,'VISIBLE','2026-10-11 12:00:00'),
(901104,901104,900001,901104,'0i2XXofKXlXbZ2sjnh06Jj',4,'VISIBLE','2026-10-08 12:00:00');
COMMIT;
