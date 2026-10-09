DROP TABLE IF EXISTS `users`;
DROP TABLE IF EXISTS `matches`;

-- Bảng users
CREATE TABLE users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL, 
    total_score INT DEFAULT 0,
    total_matches INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Bảng matches
CREATE TABLE matches (
    match_id INT AUTO_INCREMENT PRIMARY KEY,
    player_red_id INT,
    player_blue_id INT,
    red_score INT DEFAULT 0,
    blue_score INT DEFAULT 0,
    finished_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (player_red_id) REFERENCES users(id),
    FOREIGN KEY (player_blue_id) REFERENCES users(id)
);

-- Insert dữ liệu mẫu để test Đăng nhập và Bảng xếp hạng
INSERT INTO users (username, password, total_score, total_matches) VALUES 
('admin', '123', 1500, 25),
('duc', '123', 1800, 40),
('huy', '123', 1500, 30),
('kien', '123', 1200, 15);