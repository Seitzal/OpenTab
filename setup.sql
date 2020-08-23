DROP TABLE IF EXISTS judge_clashes;
DROP TABLE IF EXISTS judges;
DROP TABLE IF EXISTS speakers;
DROP TABLE IF EXISTS teams;
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
  owner     INT REFERENCES users(id) ON DELETE CASCADE,
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

CREATE TABLE teams (
  id          SERIAL UNIQUE PRIMARY KEY,
  tabid       INT REFERENCES tabs(id) ON DELETE CASCADE,
  name        VARCHAR(50),
  delegation  VARCHAR(50),
  status      INT CHECK(status > 0 AND status < 4),
  isactive    BOOLEAN,
  CONSTRAINT unique_team_name_per_tab UNIQUE(tabid, name)
);

CREATE TABLE speakers (
  id        SERIAL UNIQUE PRIMARY KEY,
  tabid     INT REFERENCES tabs(id) ON DELETE CASCADE,
  teamid    INT REFERENCES teams(id) ON DELETE CASCADE,
  firstname VARCHAR(100),
  lastname  VARCHAR(100),
  status    INT CHECK(status > 0 AND status < 4),
  CONSTRAINT unique_speaker_name_per_team UNIQUE(teamid, firstname, lastname)
);

CREATE TABLE judges (
  id        SERIAL UNIQUE PRIMARY KEY,
  tabid     INT REFERENCES tabs(id)  ON DELETE CASCADE,
  firstname VARCHAR(100),
  lastname  VARCHAR(100),
  rating    INT CHECK(rating > 0 AND rating < 11),
  isactive  BOOLEAN,
  CONSTRAINT unique_judge_name_per_tab UNIQUE(tabid, firstname, lastname)
);

CREATE TABLE judge_clashes (
  judgeid INT REFERENCES judges(id) ON DELETE CASCADE,
  teamid  INT REFERENCES teams(id) ON DELETE CASCADE,
  level   INT CHECK(level > -1 AND level < 11),
  PRIMARY KEY(judgeid, teamid)
);
