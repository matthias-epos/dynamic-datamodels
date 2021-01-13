package de.eposcat.master;

import org.junit.jupiter.api.Disabled;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

@Testcontainers
//Shouldn't be picked up by test runners anyway, but to make sure:
@Disabled
public class PageOracleReusableDockerContainer extends PageTest{

    static final GenericContainer oracle;

    static {
        oracle = new GenericContainer(DockerImageName.parse("mstrepos1/dynamic_datamodels:oracle"))
                .withExposedPorts(1521).withEnv("ORACLE_PWD", "admin")
                .waitingFor(Wait.forLogMessage(".*DATABASE IS READY TO USE!\\s*",1)
                        .withStartupTimeout(Duration.ofMinutes(15)))
                .withReuse(true);

        oracle.start();
    }

}
