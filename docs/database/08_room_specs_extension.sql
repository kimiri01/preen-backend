-- Local proposal only. Apply once after the 30-table MVP schema. DDL may commit partially.
-- Original tables are preserved. No DROP/ALTER or changes to team shared databases.
USE preen_local_moon_erd;
CREATE TABLE room_specs (
 room_id BIGINT PRIMARY KEY,
 room_type VARCHAR(50) NULL,
 area_sqm DECIMAL(7,2) NULL,
 CONSTRAINT fk_room_specs_room FOREIGN KEY(room_id) REFERENCES rooms(id),
 CONSTRAINT chk_room_area CHECK(area_sqm IS NULL OR area_sqm > 0)
);
CREATE TABLE room_beds (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 room_id BIGINT NOT NULL,
 bed_type VARCHAR(30) NOT NULL,
 bed_count SMALLINT UNSIGNED NOT NULL,
 sort_order INT NOT NULL DEFAULT 0,
 CONSTRAINT fk_room_beds_room FOREIGN KEY(room_id) REFERENCES rooms(id),
 CONSTRAINT uq_room_bed_type UNIQUE(room_id,bed_type),
 CONSTRAINT chk_room_bed_type CHECK(bed_type IN ('SINGLE','SUPER_SINGLE','DOUBLE','QUEEN','KING','BUNK','FLOOR_BEDDING')),
 CONSTRAINT chk_room_bed_count CHECK(bed_count > 0)
);
CREATE TABLE room_facilities (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 code VARCHAR(50) NOT NULL UNIQUE,
 name VARCHAR(100) NOT NULL
);
CREATE TABLE room_facility_links (
 room_id BIGINT NOT NULL,
 facility_id BIGINT NOT NULL,
 PRIMARY KEY(room_id,facility_id),
 CONSTRAINT fk_room_facility_room FOREIGN KEY(room_id) REFERENCES rooms(id),
 CONSTRAINT fk_room_facility_master FOREIGN KEY(facility_id) REFERENCES room_facilities(id)
);
