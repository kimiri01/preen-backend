-- Optional seed. Run ONCE, after 02, against the local database only.
-- This is not the team's V6 Flyway migration.
USE preen_local_moon_erd;
INSERT INTO accommodation_categories (name, sort_order) VALUES
('호텔·리조트',1), ('펜션',2), ('한옥',3), ('독채·풀빌라',4),
('게스트하우스',5), ('모텔',6), ('캠핑·글램핑',7), ('기타',8);
