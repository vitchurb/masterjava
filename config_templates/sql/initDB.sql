DROP TABLE IF EXISTS users;
DROP TABLE IF EXISTS cities;
DROP TABLE IF EXISTS groups;
DROP TABLE IF EXISTS projects;

DROP SEQUENCE IF EXISTS user_seq;
DROP SEQUENCE IF EXISTS city_seq;
DROP SEQUENCE IF EXISTS group_seq;
DROP SEQUENCE IF EXISTS project_seq;
DROP TYPE IF EXISTS user_flag;
DROP TYPE IF EXISTS group_type;

CREATE SEQUENCE city_seq START 100000;

CREATE TABLE cities (
  id        INTEGER PRIMARY KEY DEFAULT nextval('city_seq'),
  code      TEXT NOT NULL,
  name      TEXT NOT NULL
);

CREATE UNIQUE INDEX cities_code_idx ON cities (code);


CREATE SEQUENCE project_seq START 100000;

CREATE TABLE projects (
  id          INTEGER PRIMARY KEY DEFAULT nextval('project_seq'),
  name        TEXT NOT NULL,
  description TEXT NOT NULL
);

CREATE UNIQUE INDEX projects_name_idx ON projects (name);

CREATE TYPE group_type AS ENUM ('REGISTERING', 'CURRENT', 'FINISHED');

CREATE SEQUENCE group_seq START 100000;

CREATE TABLE groups (
  id        INTEGER PRIMARY KEY DEFAULT nextval('group_seq'),
  name      TEXT NOT NULL,
  type      group_type NOT NULL,
  project_id   INTEGER REFERENCES projects NOT NULL
);

CREATE UNIQUE INDEX groups_name_idx ON groups (name);


CREATE TYPE user_flag AS ENUM ('active', 'deleted', 'superuser');

CREATE SEQUENCE user_seq START 100000;

CREATE TABLE users (
  id        INTEGER PRIMARY KEY DEFAULT nextval('user_seq'),
  full_name TEXT NOT NULL,
  email     TEXT NOT NULL,
  flag      user_flag NOT NULL,
  city_id   INTEGER REFERENCES cities NOT NULL
);

CREATE UNIQUE INDEX email_idx ON users (email);


