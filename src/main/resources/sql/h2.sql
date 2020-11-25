CREATE TABLE entities (
	id IDENTITY NOT NULL PRIMARY KEY ,
	typename varchar(255)
);


CREATE TABLE attributes (
	id IDENTITY NOT NULL PRIMARY KEY ,
	datatype varchar(255),
	name varchar(255)
);

CREATE TABLE eav_values (
	ent_id bigint,
	att_id bigint,
	value varchar(255),
	PRIMARY KEY (ent_id, att_id)
);