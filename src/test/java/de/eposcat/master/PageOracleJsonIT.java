package de.eposcat.master;

import de.eposcat.master.approachImpl.EAV_DatabaseAdapter;
import de.eposcat.master.approachImpl.JSON_Oracle_DatabaseAdapter;
import de.eposcat.master.connection.CustomOracleConnectionManager;
import de.eposcat.master.connection.RelationalApproach;
import de.eposcat.master.model.AttributeBuilder;
import de.eposcat.master.model.AttributeType;
import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

@Testcontainers
public class PageOracleJsonIT extends PageTest {

    @Container
    public static GenericContainer oracle = new GenericContainer(DockerImageName.parse("mstrepos1/dynamic_datamodels:oracle")).withExposedPorts(1521).withEnv("ORACLE_PWD", "admin").waitingFor(Wait.forLogMessage(".*DATABASE IS READY TO USE!\\s*",2).withStartupTimeout(Duration.ofMinutes(2)));

    @BeforeAll
    static void initDataBase(){
        CustomOracleConnectionManager connectionManager = new CustomOracleConnectionManager(RelationalApproach.JSON, "localhost", oracle.getMappedPort(5432), "json", "json");

        dbAdapter = new JSON_Oracle_DatabaseAdapter(connectionManager);
        defaultAttribute = new AttributeBuilder().setType(AttributeType.String).setValue("A test value").createAttribute();
    }
}
