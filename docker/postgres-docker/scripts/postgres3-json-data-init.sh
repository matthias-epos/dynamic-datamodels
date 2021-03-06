#!/bin/bash

set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "json_test" <<-EOSQL
INSERT INTO pages (type, attributes) VALUES
( 'single', '[{"name":"singleAttribute","values":[{"String":"test"}]}]' ),
( 'ten', '[{"name":"tenAttribute","values":[{"String":"firstHalf"}]}]' ),
( 'ten', '[{"name":"tenAttribute","values":[{"String":"firstHalf"}]}]' ),
( 'ten', '[{"name":"tenAttribute","values":[{"String":"firstHalf"}]}]' ),
( 'ten', '[{"name":"tenAttribute","values":[{"String":"firstHalf"}]}]' ),
( 'ten', '[{"name":"tenAttribute","values":[{"String":"firstHalf"}]}]' ),
( 'ten', '[{"name":"tenAttribute","values":[{"String":"secondHalf"}]}]' ),
( 'ten', '[{"name":"tenAttribute","values":[{"String":"secondHalf"}]}]' ),
( 'ten', '[{"name":"tenAttribute","values":[{"String":"secondHalf"}]}]' ),
( 'ten', '[{"name":"tenAttribute","values":[{"String":"secondHalf"}]}]' ),
( 'ten', '[{"name":"tenAttribute","values":[{"String":"secondHalf"}]}]' ),
( 'rand', '[]' ),
( 'rand', '[]' ),
( 'rand', '[]' ),
( 'complex', '[{"name":"moreAttribute3","values":[{"String":"3"}]},{"name":"moreAttribute4","values":[{"String":"4"}]},{"name":"moreAttribute1","values":[{"String":"1"}]},{"name":"moreAttribute2","values":[{"String":"2"}]}]' );

EOSQL