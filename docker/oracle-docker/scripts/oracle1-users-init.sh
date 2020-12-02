#!/bin/bash
set -e

/opt/oracle/setPassword.sh admin

sqlplus system/admin@//localhost:1521/XEPDB1 <<-EOSQL

CREATE USER eav IDENTIFIED BY eav;
GRANT CONNECT, RESOURCE, DBA TO eav;

CREATE USER json IDENTIFIED BY json;
GRANT CONNECT, RESOURCE, DBA TO json;

exit;
EOSQL
