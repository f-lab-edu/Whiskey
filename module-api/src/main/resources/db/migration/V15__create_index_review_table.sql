CREATE INDEX idx_review_latest ON review(whiskey_id, deleted_at, id);
CREATE INDEX idx_review_rating ON review(whiskey_id, deleted_at, star_rate, id);