CREATE TABLE IF NOT EXISTS team_default_batter (
    team VARCHAR(16) NOT NULL,
    batting_order INT NOT NULL,
    hit_average DECIMAL(4, 3) NOT NULL,
    personality VARCHAR(32) NOT NULL,
    steal_forced BOOLEAN NOT NULL,
    bunt_forced BOOLEAN NOT NULL,
    PRIMARY KEY (team, batting_order)
);
