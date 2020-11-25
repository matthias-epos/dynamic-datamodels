# Dynamic Data Models

This project is used to test different approaches to storing information with changing models in a relational database.

We want to be able to add custom attribute-value pairs to entities during runtime of the program.

Currently, two different approaches are being investigated:

## EAV

The entity-attribute-value model disregards the intended usage of relations to create a dynamic model. 
It consists of three tables: **Entity, Attribute and Values**. In Entity and Attribute we save the definition of entities and attributes respectively. 
The **Values** table stores in each row one value of an attribute of an entity.

## JSON-Values

JSON enables us to store dynamic structures in a text format. 
Thanks to the increased usage of JSON in web environments database vendors improved the handling of this data type.

Currently, we inspect the capabilities of 2 databases:

#### PostgreSQL 

*Documentation*

[Data Type](https://www.postgresql.org/docs/13/datatype-json.html)  
[Functions](https://www.postgresql.org/docs/13/functions-json.html)  
[Indexing](https://www.postgresql.org/docs/13/datatype-json.html#JSON-INDEXING)  

*Setup*

[Download](https://www.postgresql.org/download/)  
[SQL Commands](src/main/resources/sql/postgre.sql)


#### Oracle

*Documentation*

[Data Type](https://docs.oracle.com/en/database/oracle/oracle-database/18/adjsn/json-in-oracle-database.html#GUID-F6282E67-CBDF-442E-946F-5F781BC14F33)  
[Functions](https://docs.oracle.com/en/database/oracle/oracle-database/18/adjsn/query-json-data.html#GUID-119E5069-77F2-45DC-B6F0-A1B312945590)  
[Indexing](https://docs.oracle.com/en/database/oracle/oracle-database/18/adjsn/indexes-for-json-data.html#GUID-8A1B098E-D4FE-436E-A715-D8B465655C0D)  

*Setup*

[Download Test Version 18c XE](https://www.oracle.com/database/technologies/xe-downloads.html)
[Docker Container Test Version 18c XE Instructions](https://github.com/oracle/docker-images/tree/master/OracleDatabase/SingleInstance)  
[SQL Commands](src/main/resources/sql/oracle.sql)

<!-- 

## PostgreSQL HSTORE

Since indexing JSON values in PostgreSQL requires simple JSON Objects, we could also consider using the key-value store of PostreSQL.

-->
