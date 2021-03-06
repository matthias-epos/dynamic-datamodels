-- Currently the Connection URL for the connector is jdbc:oracle:thin:@//localhost:33333/xepdb1
-- We should add a properties or config file to change it on the fly

-- EAV

CREATE TABLE entities (
	id NUMBER GENERATED BY DEFAULT ON NULL AS IDENTITY,
	typename varchar2(255),

    CONSTRAINT "ENTITIES_ID_PK" PRIMARY KEY ("ID")
);


CREATE TABLE attributes (
	id NUMBER GENERATED BY DEFAULT ON NULL AS IDENTITY,
	datatype varchar2(255),
	name varchar2(255),

	CONSTRAINT "ATTRIBUTES_ID_PK" PRIMARY KEY ("ID")
);

CREATE TABLE eav_values (
	ent_id NUMBER,
	att_id Number,
	value varchar2(255),

	CONSTRAINT "EAV_VALUES_PK_D" PRIMARY KEY (ent_id, att_id)
);

-- JSON

CREATE TABLE pages (
    id NUMBER GENERATED BY DEFAULT ON NULL AS IDENTITY,
    type varchar2(255),
    -- Oracle Docs use clob in examples in version 12 and varchar2(23767) in version 18 examples
    -- Not sure why exactly 23767 bytes used, doesnt work in default config of database
    attributes varchar2(4000),

    CONSTRAINT "PAGES_ID_PK" PRIMARY KEY ("ID"),
    CONSTRAINT "ENSURE_JSON_ATTRIBUTE" CHECK (attributes IS JSON)
);