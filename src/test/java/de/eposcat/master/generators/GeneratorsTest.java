package de.eposcat.master.generators;

import de.eposcat.master.approachImpl.EAV_DatabaseAdapter;
import de.eposcat.master.approachImpl.IDatabaseAdapter;
import de.eposcat.master.approachImpl.JSON_Postgres_DatabaseAdapter;
import de.eposcat.master.connection.PostgresConnectionManager;
import de.eposcat.master.connection.RelationalApproach;
import de.eposcat.master.generators.data.FillerAttributesStats;
import de.eposcat.master.generators.data.PerformanceTestAttribute;
import de.eposcat.master.generators.data.StartData;
import de.eposcat.master.model.Attribute;
import de.eposcat.master.model.AttributeBuilder;
import de.eposcat.master.model.AttributeType;
import de.eposcat.master.model.Page;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;


import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.fail;

@Testcontainers
public class GeneratorsTest {
    static IDatabaseAdapter dbAdapter;
    static Attribute defaultAttribute;

    private static final RelationalApproach approach = RelationalApproach.EAV;
    private static String database;

    private static final Logger log = LoggerFactory.getLogger(GeneratorsTest.class);

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
        initPostgresDB();
    }

    static void initPostgresDB(){
        PostgresConnectionManager connectionManager = new PostgresConnectionManager(approach, "localhost", postgres.getMappedPort(5432), "postgres", "admin");
        database = "PostgreSQL";

        switch (approach){
            case EAV: dbAdapter = new EAV_DatabaseAdapter(connectionManager);
                break;
            case JSON: dbAdapter = new JSON_Postgres_DatabaseAdapter(connectionManager);
                break;
        }

        defaultAttribute = new AttributeBuilder().setType(AttributeType.String).setValue("A test value").createAttribute();
    }

    @Test
    public void testGenerators() {
        try {
            String path = "test.txt";
            StartDataGenerator startDataGenerator = new StartDataGenerator(1);

            log.info("Testing Approach: {}, Database: {}", approach, database);
            log.info("Started Generating initial pages");
            Instant start = Instant.now();

            int numberOfStartAttributes = 10000;
            int meanNumberOfAttributes = 20;
            int maxNumberOfAttributes = 7500;

            //TODO Put arguments into builder?
            FillerAttributesStats filler = new FillerAttributesStats(numberOfStartAttributes, meanNumberOfAttributes, maxNumberOfAttributes);

            int numberOfStartEntities = 1000;

            StartData startData = startDataGenerator.generateData(numberOfStartEntities, filler, getExampleAtt(new Random(1)));

            for (Page page : startData.pages) {
                dbAdapter.createPageWithAttributes(page.getTypeName(), page.getAttributes());
            }

            Instant end = Instant.now();
            log.info("Finished Generating {} initial pages, duration: {}",
                    numberOfStartEntities, Duration.between(start,end));
            log.info("Filler Attribute stats: numberOfStartAttributes: {}, meanNumberOfAttributes: {}, maxNumberOfAttributes, {}",
                    numberOfStartAttributes, meanNumberOfAttributes, maxNumberOfAttributes);

//            ChangesGenerator generateChanges = new ChangesGenerator(startData.entityNames, startData.attributeNames, path, 1);
//            generateChanges.generateChangeSets(100);
//            ChangeRunner runner = new ChangeRunner(dbAdapter);
//            runner.applyChanges(Paths.get(path));

            log.info("Started finding fifty percent of pages");
            Instant startF = Instant.now();

            int count = dbAdapter.findPagesByAttributeName("fiftyFifty").size();

            Instant endF = Instant.now();
            log.info("Finished finding fifty percent of pages, duration: {}, count: {}", Duration.between(startF, endF), count);


            log.info("Started finding 25% of pages per attValue");
            Instant startVal = Instant.now();

            //Returns count 0 atm. -> probably bugged function, will switch to fixing tests in other branch now..
            int countVal = dbAdapter.findPagesByAttributeValue("fiftyFifty", new AttributeBuilder().setValue("true").setType(AttributeType.String).createAttribute()).size();

            Instant endVal = Instant.now();
            log.info("Finished finding 25% of pages per attValue, duration: {}, count: {}", Duration.between(startVal,endVal), countVal);

        } catch (SQLException e) {
            e.printStackTrace();
            fail();
        }
    }

    private ArrayList<PerformanceTestAttribute> getExampleAtt(Random r) {
        return new ArrayList<>(Arrays.asList(
                new PerformanceTestAttribute("fiftyFifty", 50d, () -> String.valueOf(r.nextBoolean())),
                new PerformanceTestAttribute("tenPercent", 10d, () -> String.valueOf(r.nextBoolean()))
        ));
    }
}
