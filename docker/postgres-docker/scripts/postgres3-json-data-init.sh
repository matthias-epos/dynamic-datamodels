#!/bin/bash
set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "json_test" <<-EOSQL
INSERT INTO pages (type, attributes) VALUES
( 'single', '{"singleAttribute":{"type":"String","value":"test"}}' ),
( 'ten', '{"tenAttribute":{"type":"String","value":"firstHalf"}}' ),
( 'ten', '{"tenAttribute":{"type":"String","value":"firstHalf"}}' ),
( 'ten', '{"tenAttribute":{"type":"String","value":"firstHalf"}}' ),
( 'ten', '{"tenAttribute":{"type":"String","value":"firstHalf"}}' ),
( 'ten', '{"tenAttribute":{"type":"String","value":"firstHalf"}}' ),
( 'ten', '{"tenAttribute":{"type":"String","value":"secondHalf"}}' ),
( 'ten', '{"tenAttribute":{"type":"String","value":"secondHalf"}}' ),
( 'ten', '{"tenAttribute":{"type":"String","value":"secondHalf"}}' ),
( 'ten', '{"tenAttribute":{"type":"String","value":"secondHalf"}}' ),
( 'ten', '{"tenAttribute":{"type":"String","value":"secondHalf"}}' ),
( 'rand', '{}' ),
( 'rand', '{}' ),
( 'rand', '{}' );
EOSQL