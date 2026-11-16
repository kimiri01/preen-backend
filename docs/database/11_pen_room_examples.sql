-- Local demo only: align first two rooms with Must12 reference.
USE preen_local_moon_erd;
START TRANSACTION;
UPDATE rooms SET name='오션뷰 독채',capacity=2,max_capacity=4,nightly_price=180000,extra_person_fee=20000 WHERE id=900001 AND accommodation_id=900001;
UPDATE rooms SET name='가든 스위트',capacity=2,max_capacity=4,nightly_price=210000,extra_person_fee=20000 WHERE id=900002 AND accommodation_id=900001;
UPDATE room_specs SET room_type='독채',area_sqm=56 WHERE room_id=900001;
UPDATE room_specs SET room_type='스위트',area_sqm=56 WHERE room_id=900002;
UPDATE room_beds SET bed_count=1,bed_type='DOUBLE' WHERE room_id=900001 AND bed_type='DOUBLE';
COMMIT;
