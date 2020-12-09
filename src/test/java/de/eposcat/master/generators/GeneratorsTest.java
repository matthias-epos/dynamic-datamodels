package de.eposcat.master.generators;

import de.eposcat.master.approachImpl.IDatabaseAdapter;
import de.eposcat.master.approachImpl.JSON_Postgres_DatabaseAdapter;
import de.eposcat.master.connection.PostgresConnectionManager;
import de.eposcat.master.connection.RelationalApproach;
import de.eposcat.master.generators.data.StartData;
import de.eposcat.master.model.Attribute;
import de.eposcat.master.model.AttributeBuilder;
import de.eposcat.master.model.AttributeType;
import de.eposcat.master.model.Page;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.fail;

@Testcontainers
public class GeneratorsTest {
    static IDatabaseAdapter dbAdapter;
    static Attribute defaultAttribute;

    @Container
    public static GenericContainer postgres = new GenericContainer(DockerImageName.parse("mstrepos1/dynamic_datamodels:postgres")).withExposedPorts(5432).withEnv("POSTGRES_PASSWORD", "admin").waitingFor(Wait.forLogMessage(".*database system is ready to accept connections\\s*",2).withStartupTimeout(Duration.ofMinutes(2)));


    //    @BeforeAll
//    static void initDataBase() {
//        H2ConnectionManager connectionManager = new H2ConnectionManager(RelationalApproach.EAV);
//
//        dbAdapter = new EAV_DatabaseAdapter(connectionManager);
//        defaultAttribute = new AttributeBuilder().setType(AttributeType.String).setValue("A test value").createAttribute();
//    }

    @BeforeAll
    static void initDataBase(){
        PostgresConnectionManager connectionManager = new PostgresConnectionManager(RelationalApproach.JSON, "localhost", postgres.getMappedPort(5432), "postgres", "admin");

        dbAdapter = new JSON_Postgres_DatabaseAdapter(connectionManager);
        defaultAttribute = new AttributeBuilder().setType(AttributeType.String).setValue("A test value").createAttribute();
    }

    @Test
    public void testGenerators() {
        try {
            String path = "test.txt";
            StartDataGenerator startDataGenerator = new StartDataGenerator(1);
            //TODO Put arguments into builder?
            StartData startData = startDataGenerator.generateData(1000, 200, 30, 100);

            for (Page page : startData.pages) {
                dbAdapter.createPageWithAttributes(page.getTypeName(), page.getAttributes());
            }

            ChangesGenerator generateChanges= new ChangesGenerator(startData.entityNames, startData.attributeNames, path, 1);
            generateChanges.generateChangeSets(100);


            ChangeRunner runner = new ChangeRunner(dbAdapter);
            runner.applyChanges(Paths.get(path));

            System.out.println("test");

            dbAdapter.findPagesByType("test");
        } catch (SQLException | IOException e) {
            e.printStackTrace();
            fail();
        }
    }
}
