-- Optional local extension: display metadata, never Spotify tokens/audio.
-- Apply once after the base schema, before the review API is started.
USE preen_local_moon_erd;
CREATE TABLE spotify_track_metadata (
 spotify_track_id VARCHAR(22) PRIMARY KEY,
 title VARCHAR(200) NOT NULL,
 artist VARCHAR(200) NOT NULL,
 album VARCHAR(200) NULL,
 CONSTRAINT chk_spotify_track_length CHECK(CHAR_LENGTH(spotify_track_id)=22)
);
