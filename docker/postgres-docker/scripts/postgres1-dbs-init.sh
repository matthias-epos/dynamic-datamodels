#!/bin/bash
set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    -- Create database
    CREATE DATABASE eav_test
      WITH
      OWNER = postgres
      ENCODING = 'UTF8'
      CONNECTION LIMIT = -1;

    \c eav_test

    CREATE TABLE entities (
      id SERIAL,
      typename varchar(255)
    );

    ALTER TABLE public.entities ADD CONSTRAINT entities_pkey PRIMARY KEY (ID);

    CREATE TABLE attributes (
      id SERIAL,
      datatype varchar(255),
      name varchar(255)
    );

    ALTER TABLE public.attributes ADD CONSTRAINT attributes_pkey PRIMARY KEY (id);

    CREATE TABLE eav_values (
      ent_id int,
      att_id int,
      value text,
      PRIMARY KEY (ent_id, att_id)
    );

    \c postgres

    CREATE DATABASE json_test
    WITH
    OWNER = postgres
    ENCODING = 'UTF8'
    CONNECTION LIMIT = -1;

    \c json_test

    CREATE TABLE pages
    (
        id Serial,
        type varchar,
        attributes jsonb
    );
EOSQL