INSERT INTO users
  (name, password, email, isadmin)
  VALUES (
    'admin', 
    '$2a$10$bNhtvTwVUSAo4zwTVY8Gu.fU7vYPKc.VhCm3C0VHYDhSWiYyaSQ7m',
    'admin@example.tld',
    TRUE
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
