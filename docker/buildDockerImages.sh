echo OFF

echo If creating/starting the images/containers fails, make sure to use linux-style line endings in the shell scripts!

if [ "$1" = "mvn" ]
then
	path=$PWD/docker
else
	path=.
fi

echo $path

echo Building postgres image
docker build -t mstrepos1/dynamic_datamodels:postgres --rm $path/postgres-docker/

echo "Building vanilla oracle image if it does not exist yet; can't just pull because oracle; if this step does not work follow the instructions at https://github.com/oracle/docker-images/tree/master/OracleDatabase/SingleInstance to get the base image"
docker image inspect oracle/database:18.4.0-xe  > /dev/null 2>&1 && echo "Vanilla OracleDB exists, skipping building new image" || docker build -t oracle/database:18.4.0-xe -f $path/vanilla-oracle-docker/18.4.0/Dockerfile.xe --rm $path/vanilla-oracle-docker/18.4.0/

echo Building oracle image
docker build -t mstrepos1/dynamic_datamodels:oracle --rm $path/oracle-docker/

