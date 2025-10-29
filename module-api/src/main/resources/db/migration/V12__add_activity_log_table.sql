CREATE TABLE activity_logs (
    id bigint PRIMARY KEY AUTO_INCREMENT,
    member_id bigint NOT NULL,
    activity_type ENUM('VIEW', 'SEARCH', 'LOGIN'),
    target_type ENUM('WHISKEY', 'REVIEW', 'MEMBER'),
    target_id bigint NOT NULL,
    ip_address varchar(45),
    create_at datetime,
    update_at datetime
);

CREATE INDEX idx_target ON activity_logs(target_type, target_id);
CREATE INDEX idx_activity_type ON activity_logs(activity_type);
CREATE INDEX idx_member_id ON activity_logs(member_id);