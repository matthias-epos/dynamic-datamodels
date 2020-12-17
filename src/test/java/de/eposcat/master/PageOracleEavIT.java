package de.eposcat.master;

import de.eposcat.master.approachImpl.EAV_DatabaseAdapter;
import de.eposcat.master.connection.AbstractConnectionManager;
import de.eposcat.master.connection.CustomOracleConnectionManager;
import de.eposcat.master.connection.PostgresConnectionManager;
import de.eposcat.master.connection.RelationalApproach;
import de.eposcat.master.model.AttributeBuilder;
import de.eposcat.master.model.AttributeType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

@Testcontainers
@Disabled
public class PageOracleEavIT extends PageTest {

    @Container
    public static GenericContainer oracle = new GenericContainer(DockerImageName.parse("mstrepos1/dynamic_datamodels:oracle"))
            .withExposedPorts(1521).withEnv("ORACLE_PWD", "admin")
            .waitingFor(Wait.forLogMessage(".*DATABASE IS READY TO USE!\\s*",1)
            .withStartupTimeout(Duration.ofMinutes(15)));

    @BeforeAll
    static void initDataBase(){
        CustomOracleConnectionManager connectionManager = new CustomOracleConnectionManager(RelationalApproach.EAV, "localhost", oracle.getMappedPort(1521), "eav", "eav");

        dbAdapter = new EAV_DatabaseAdapter(connectionManager);
        defaultAttribute = new AttributeBuilder().setType(AttributeType.String).setValue("A test value").createAttribute();
    }
}
