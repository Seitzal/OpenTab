DROP TABLE IF EXISTS tabs;
DROP TABLE IF EXISTS users;
DROP TABLE IF EXISTS instance;

CREATE TABLE instance (
  schemaVersion VARCHAR(10)
);

INSERT INTO instance (schemaVersion) VALUES ('0.1.0');

CREATE TABLE IF NOT EXISTS users (
  id        SERIAL UNIQUE PRIMARY KEY,
  name      VARCHAR(30) UNIQUE,
  password  CHAR(60),
  email     VARCHAR(60) UNIQUE,
  isAdmin   BOOLEAN
);

INSERT INTO users
  (name, password, email, isAdmin)
  VALUES (
    'admin', 
    '$2a$10$bNhtvTwVUSAo4zwTVY8Gu.fU7vYPKc.VhCm3C0VHYDhSWiYyaSQ7m',
    'admin@example.tld',
    TRUE
  );

CREATE TABLE IF NOT EXISTS tabs (
  handle    VARCHAR(15) UNIQUE PRIMARY KEY,
  name      VARCHAR(50),
  owner     VARCHAR(30) REFERENCES users(name),
  isPublic  BOOLEAN
);
