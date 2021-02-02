package de.eposcat.master.generators;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

public class ContainterInfo {
    static GenericContainer getOracleContainer(String label){
        return new GenericContainer(DockerImageName.parse("mstrepos1/dynamic_datamodels:oracle"))
                .withExposedPorts(1521).withEnv("ORACLE_PWD", "admin")
                .waitingFor(Wait.forLogMessage(".*DATABASE IS READY TO USE!\\s*",1)
                        .withStartupTimeout(Duration.ofMinutes(15)))
                .withLabel("de.eposcat.testCase", label)
                .withReuse(true);
    }

    static GenericContainer getPostgresContainer(String label){
        return new GenericContainer(DockerImageName.parse("mstrepos1/dynamic_datamodels:postgres"))
                .withExposedPorts(5432).withEnv("POSTGRES_PASSWORD", "admin")
                .waitingFor(Wait.forLogMessage(".*database system is ready to accept connections\\s*",2)
                        .withStartupTimeout(Duration.ofMinutes(2)))
                .withLabel("de.eposcat.testCase", label)
                .withReuse(true);
    }
}
