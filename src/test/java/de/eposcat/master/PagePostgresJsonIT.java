package de.eposcat.master;

import de.eposcat.master.connection.PostgresConnectionManager;
import de.eposcat.master.model.AttributeBuilder;
import de.eposcat.master.approachImpl.JSON_Postgres_DatabaseAdapter;
import de.eposcat.master.connection.RelationalApproach;
import de.eposcat.master.model.AttributeType;
import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

@Testcontainers
public class PagePostgresJsonIT extends PageTest {

    @Container
    public static GenericContainer postgres = new GenericContainer(DockerImageName.parse("mstrepos1/dynamic_datamodels:postgres"))
            .withExposedPorts(5432).withEnv("POSTGRES_PASSWORD", "admin")
            .waitingFor(Wait.forLogMessage(".*database system is ready to accept connections\\s*",2)
            .withStartupTimeout(Duration.ofMinutes(2)));

//    @Container
//    public GenericContainer oracle = new GenericContainer(DockerImageName.parse("mstrepos1/dynamic_datamodels:oracle")).withExposedPorts(55555);

    @BeforeAll
    static void initDataBase(){
        PostgresConnectionManager connectionManager = new PostgresConnectionManager(RelationalApproach.JSON, "localhost", postgres.getMappedPort(5432), "postgres", "admin");

        dbAdapter = new JSON_Postgres_DatabaseAdapter(connectionManager);
        defaultAttribute = new AttributeBuilder().setType(AttributeType.String).setValue("A test value").createAttribute();
    }
}
