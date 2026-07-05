ALTER TABLE member ADD COLUMN role enum('USER','ADMIN') NOT NULL DEFAULT 'USER';

-- 부하 테스트/개발용 관리자 시드: 기존 회원 id=3을 ADMIN으로 승격 (대상 없으면 no-op)
UPDATE member SET role = 'ADMIN' WHERE id = 3;
