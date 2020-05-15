DROP TABLE IF EXISTS permissions;
DROP TABLE IF EXISTS tabs;
DROP TABLE IF EXISTS users;
DROP TABLE IF EXISTS instance;

CREATE TABLE instance (
  schemaversion VARCHAR(10)
);

INSERT INTO instance (schemaversion) VALUES ('0.1.0');

CREATE TABLE users (
  id        SERIAL UNIQUE PRIMARY KEY,
  name      VARCHAR(30) UNIQUE,
  password  CHAR(60),
  email     VARCHAR(60) UNIQUE,
  isadmin   BOOLEAN
);

INSERT INTO users
  (name, password, email, isadmin)
  VALUES (
    'admin', 
    '$2a$10$bNhtvTwVUSAo4zwTVY8Gu.fU7vYPKc.VhCm3C0VHYDhSWiYyaSQ7m',
    'admin@example.tld',
    TRUE
  );

CREATE TABLE tabs (
  id        SERIAL UNIQUE PRIMARY KEY,
  name      VARCHAR(50),
  owner     INT REFERENCES users(id),
  ispublic  BOOLEAN
);

INSERT INTO tabs
  (name, owner, ispublic)
  VALUES (
    'Public Test',
    1,
    TRUE
  );

INSERT INTO tabs
  (name, owner, ispublic)
  VALUES (
    'Private Test',
    1,
    FALSE
  );

CREATE TABLE permissions (
  userid  INT REFERENCES users(id),
  tabid   INT REFERENCES tabs(id),
  view    BOOLEAN,
  results BOOLEAN,
  setup   BOOLEAN,
  own     BOOLEAN,
  PRIMARY KEY(userid, tabid)
);
