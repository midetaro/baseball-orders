CREATE TABLE IF NOT EXISTS players (
    player_id BIGINT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    hit_average REAL NOT NULL,
    sluggish REAL NOT NULL,
    bunt_success_rate REAL NOT NULL,
    steal_success_rate REAL NOT NULL
);
