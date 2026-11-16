-- Local room Spotify recommendation; track ID only, no token/audio storage.
USE preen_local_moon_erd;
ALTER TABLE room_audio ADD COLUMN spotify_track_id VARCHAR(22) NULL;
UPDATE room_audio SET spotify_track_id='11dFghVXANMlKmJXsNCbNl',connection_guide='이 페이지의 Spotify 플레이어를 열고, 재생 기기를 객실 오디오에 Bluetooth로 연결해주세요.' WHERE room_id=900005;
UPDATE room_audio SET spotify_track_id='11dFghVXANMlKmJXsNCbNl',connection_guide='이 페이지의 Spotify 플레이어를 열고, 재생 기기를 객실 오디오에 Bluetooth 또는 AUX로 연결해주세요.' WHERE room_id=900006;
