ALTER TABLE whiskey ADD COLUMN avg_rating double DEFAULT 0.0;
ALTER TABLE whiskey ADD COLUMN review_count int DEFAULT 0;
ALTER TABLE whiskey ADD COLUMN version bigint DEFAULT 0;

ALTER TABLE review MODIFY COLUMN star_rate int NOT NULL CHECK (star_rate >= 1 AND star_rate <= 5);
ALTER TABLE review ADD COLUMN content text;
ALTER TABLE review ADD CONSTRAINT uk_review_whiskey_member_id UNIQUE (whiskey_id, member_id);