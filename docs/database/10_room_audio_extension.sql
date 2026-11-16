-- Local proposal for audio-included rooms. Apply once after 08. No fee or booking option.
USE preen_local_moon_erd;
CREATE TABLE room_audio (
 room_id BIGINT PRIMARY KEY,
 equipment_name VARCHAR(150) NOT NULL,
 connection_guide VARCHAR(500) NOT NULL,
 available_from TIME NOT NULL,
 available_until TIME NOT NULL,
 volume_guide VARCHAR(500) NOT NULL,
 CONSTRAINT fk_room_audio_room FOREIGN KEY(room_id) REFERENCES rooms(id),
 CONSTRAINT chk_room_audio_hours CHECK(available_until > available_from)
);
INSERT INTO room_audio(room_id,equipment_name,connection_guide,available_from,available_until,volume_guide) VALUES
(900005,'스테레오 앰프 · 북셀프 스피커 2대','개인 기기를 Bluetooth로 연결해 이용할 수 있습니다.','09:00','22:00','대화가 가능한 음량으로 이용해주세요. 스피커 위치 변경은 삼가주세요.'),
(900006,'인티앰프 · 플로어스탠딩 스피커 2대','개인 기기를 Bluetooth 또는 AUX로 연결해 이용할 수 있습니다.','09:00','22:00','창문과 문을 닫고 적정 음량으로 이용해주세요. 야외에서는 오디오를 이용할 수 없습니다.');
