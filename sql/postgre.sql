-- Currently the Connection URL for the connector is jdbc:postgresql://localhost/ + database name (based on approach used)
-- We should add a properties or config file to change it on the fly

---- EAV

CREATE DATABASE eav_test
    WITH
    OWNER = postgres
    ENCODING = 'UTF8'
    CONNECTION LIMIT = -1;

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
	value varchar(255),
	PRIMARY KEY (ent_id, att_id)
);

-- EAV Index

---- JSON
CREATE DATABASE json_test
    WITH
    OWNER = postgres
    ENCODING = 'UTF8'
    CONNECTION LIMIT = -1;

CREATE TABLE pages
(
    id Serial,
    type varchar,
    attributes jsonb
)

-- JSON Index

---- HSTORE