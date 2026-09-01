CREATE TABLE IF NOT EXISTS players (
    player_id VARCHAR(255) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    hit_average REAL NOT NULL,
    sluggish REAL NOT NULL
);
