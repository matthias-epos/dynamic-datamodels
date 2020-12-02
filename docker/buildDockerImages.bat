echo OFF

echo Building postgres image
docker build -t mstrepos1/dynamic_datamodels:postgres --rm ./postgres-docker/

echo Building vanilla oracle image which ours is based on (not available in repo)
docker build -t oracle/database:18.4.0-xe -f Dockerfile.xe --rm ./vanilla-oracle-docker/18.4.0/

echo Building oracle image
docker build -t mstrepos1/dynamic_datamodels:oracle --rm ./oracle-docker/

