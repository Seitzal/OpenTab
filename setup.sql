DROP TABLE IF EXISTS scores;
DROP TABLE IF EXISTS ballots;
DROP TABLE IF EXISTS debates;
DROP TABLE IF EXISTS rounds;
DROP TABLE IF EXISTS judge_clashes;
DROP TABLE IF EXISTS judges;
DROP TABLE IF EXISTS speakers;
DROP TABLE IF EXISTS teams;
DROP TABLE IF EXISTS permissions;
DROP TABLE IF EXISTS tabsettings;
DROP TABLE IF EXISTS tabs;
DROP TABLE IF EXISTS users;
DROP TABLE IF EXISTS instance;

CREATE TABLE instance (
  schemaversion VARCHAR(10)
);

INSERT INTO instance (schemaversion) VALUES ('0.1.0');

CREATE TABLE users (
  id        SERIAL      UNIQUE PRIMARY KEY,
  name      VARCHAR(30) UNIQUE,
  password  CHAR(60)    ,
  email     VARCHAR(60) UNIQUE,
  isadmin   BOOLEAN
);

CREATE TABLE tabs (
  id        SERIAL      UNIQUE PRIMARY KEY,
  name      VARCHAR(50) ,
  owner     INT         REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE tabsettings (
  tabid     INT         REFERENCES tabs(id) ON DELETE CASCADE,
  key       VARCHAR(40) ,
  value     VARCHAR(100),
  PRIMARY KEY (tabid, key)
);

CREATE TABLE permissions (
  userid  INT     REFERENCES users(id) ON DELETE CASCADE,
  tabid   INT     REFERENCES tabs(id) ON DELETE CASCADE,
  view    BOOLEAN ,
  results BOOLEAN ,
  setup   BOOLEAN ,
  own     BOOLEAN ,

  PRIMARY KEY(userid, tabid)
);

CREATE TABLE teams (
  id          SERIAL      UNIQUE PRIMARY KEY,
  tabid       INT          REFERENCES tabs(id) ON DELETE CASCADE,
  name        VARCHAR(50) ,
  delegation  VARCHAR(50) ,
  status      INT         CHECK(status > 0 AND status < 4),
  isactive    BOOLEAN     ,

  CONSTRAINT unique_team_name_per_tab UNIQUE(tabid, name)
);

CREATE TABLE speakers (
  id        SERIAL        UNIQUE PRIMARY KEY,
  tabid     INT           REFERENCES tabs(id) ON DELETE CASCADE,
  teamid    INT           REFERENCES teams(id) ON DELETE CASCADE,
  firstname VARCHAR(100)  ,
  lastname  VARCHAR(100)  ,
  status    INT           CHECK(status > 0 AND status < 4),

  CONSTRAINT unique_speaker_name_per_team UNIQUE(teamid, firstname, lastname)
);

CREATE TABLE judges (
  id        SERIAL        UNIQUE PRIMARY KEY,
  tabid     INT           REFERENCES tabs(id) ON DELETE CASCADE,
  firstname VARCHAR(100)  ,
  lastname  VARCHAR(100)  ,
  rating    INT           CHECK(rating > 0 AND rating < 11),
  urlkey    INT           ,
  isactive  BOOLEAN       ,

  CONSTRAINT unique_judge_name_per_tab UNIQUE(tabid, firstname, lastname)
);

CREATE TABLE judge_clashes (
  judgeid INT REFERENCES judges(id) ON DELETE CASCADE,
  teamid  INT REFERENCES teams(id) ON DELETE CASCADE,
  level   INT CHECK(level > -1 AND level < 11),

  PRIMARY KEY(judgeid, teamid)
);

CREATE TABLE rounds (
  tabid       INT     REFERENCES tabs(id) ON DELETE CASCADE,
  roundno     INT     ,
  isprepared  BOOLEAN ,
  islocked    BOOLEAN ,

  PRIMARY KEY(tabid, roundno)
);

CREATE TABLE debates (
  id        SERIAL    UNIQUE PRIMARY KEY,
  tabid     INT       ,
  roundno   INT       ,
  pro       INT       REFERENCES teams(id) ON DELETE CASCADE,
  pro_swing BOOLEAN   ,
  pro_adhoc BOOLEAN   ,
  pro_bye   BOOLEAN   ,
  opp       INT       REFERENCES teams(id) ON DELETE CASCADE,
  opp_swing BOOLEAN   ,
  opp_adhoc BOOLEAN   ,
  opp_bye   BOOLEAN   ,

  FOREIGN KEY (tabid, roundno) REFERENCES rounds (tabid, roundno) ON DELETE CASCADE
);

CREATE TABLE ballots (
  id            SERIAL  UNIQUE PRIMARY KEY,
  debateid      INT     REFERENCES debates(id) ON DELETE CASCADE,
  judgeid       INT     REFERENCES judges(id) ON DELETE CASCADE,
  is_chair      BOOLEAN ,
  is_confirmed  BOOLEAN ,
  winner        INT     REFERENCES teams(id) ON DELETE CASCADE
);

CREATE TABLE scores (
  speakerid         INT       REFERENCES speakers(id),
  ballotid          INT       REFERENCES ballots(id),
  is_reply          BOOLEAN   ,
  position          INT       CHECK(position > 0 AND position < 5), /* 4 = Reply */
  is_swing_or_adhoc BOOLEAN   ,

  PRIMARY KEY (speakerid, ballotid, is_reply)
);
