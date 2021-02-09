echo OFF

echo If creating/starting the images/containers fails, make sure to use linux-style line endings in the shell scripts!

echo Building postgres image
docker build -t mstrepos1/dynamic_datamodels:postgres --rm ./postgres-docker/

echo "Building vanilla oracle image if it does not exist yet; can't just pull because oracle; if this step does not work follow the instructions at https://github.com/oracle/docker-images/tree/master/OracleDatabase/SingleInstance to get the base image"
docker image inspect oracle/database:18.4.0-xe  >NUL 2>&1 && docker build -t oracle/database:18.4.0-xe -f vanilla-oracle-docker/18.4.0/Dockerfile.xe --rm ./vanilla-oracle-docker/18.4.0/ || echo "Vanilla OracleDB exists, skipping building new image"

echo Building oracle image
docker build -t mstrepos1/dynamic_datamodels:oracle --rm ./oracle-docker/

