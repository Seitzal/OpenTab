DROP TABLE IF EXISTS instance;

CREATE TABLE instance (
  schemaVersion VARCHAR(10)
);

INSERT INTO instance (schemaVersion) VALUES ('0.1.0');

CREATE TABLE IF NOT EXISTS users (
  name      VARCHAR(30) UNIQUE PRIMARY KEY,
  password  CHAR(60),
  email     VARCHAR(60) UNIQUE,
  isAdmin   BOOLEAN
);

CREATE TABLE IF NOT EXISTS tabs (
  handle    VARCHAR(15) UNIQUE PRIMARY KEY,
  name      VARCHAR(50),
  owner     VARCHAR(30) REFERENCES users(name),
  isPublic  BOOLEAN
);
