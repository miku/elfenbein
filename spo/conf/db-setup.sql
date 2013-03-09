CREATE TABLE IF NOT EXISTS triples (
    id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    s VARCHAR(4096) NOT NULL,
    p VARCHAR(4096) NOT NULL,
    o VARCHAR(4096) NOT NULL,
    last_modified TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    UNIQUE KEY `spo` (s(255), p(255), o(255))
);

CREATE INDEX idx_triples_s ON triples (s(255));
CREATE INDEX idx_triples_p ON triples (p(255));
CREATE INDEX idx_triples_o ON triples (o(255));
CREATE INDEX idx_triples_last_modified ON triples (last_modified);
