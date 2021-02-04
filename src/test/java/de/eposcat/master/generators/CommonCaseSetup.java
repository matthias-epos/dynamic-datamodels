package de.eposcat.master.generators;

import de.eposcat.master.approachImpl.IDatabaseAdapter;
import de.eposcat.master.generators.data.PerformanceTestAttribute;
import de.eposcat.master.model.Attribute;
import de.eposcat.master.model.AttributeBuilder;
import de.eposcat.master.model.AttributeType;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * The databases of this class get filled with data when first run and then will be reused for the tests
 * To generate new start data, delete the running containers (it seems like you cannot name the containers with testcontainers, so look for containers based on the images)
 *
 * !!!!!!!
 * Currently there can be only one running oracle container, so you might need to stop the one from the integration tests or the other Test cases
 * There is probably an issue with the ports and this behavior might be fixed in a future update.
 * It is possible though to pause and resume the containers manually and save yourself some time!
 * !!!!!!!
 *
 * This class should not be run as part of the Unit and Integration Tests
 */
@Testcontainers
public class CommonCaseSetup extends PerformanceTestContainerStartup{

    private static final Logger log = LoggerFactory.getLogger(CommonCaseSetup.class);

    static int numberOfChanges = 5000;

    static {
        setupContainers("commonCase");
        startup();
    }

    public static void setupDatabaseValuesParameters(){
        setupName = "Best Case Scenario";

        numberOfStartEntities = 100000;

        maxNumberOfAttributes = 200;
        meanNumberOfAttributes = 100;
        numberOfStartAttributes = 1000;

        queryAttributes = new ArrayList<>(Arrays.asList(
                new PerformanceTestAttribute("twentyPercent", 20d, () -> String.valueOf(r.nextBoolean())),
                new PerformanceTestAttribute("fiftyPercent", 50d, () -> String.valueOf(r.nextBoolean())),
                new PerformanceTestAttribute("seventyPercent", 70d, () -> String.valueOf(r.nextBoolean()))
        ));
    }

    static void startup(){
        setupDatabaseValuesParameters();
        setupData();

        //Currently we only remember entityNames and attributeNames when generating the startData
        //To be more flexible with creating changeSets we might have to persist them somewhere (db or text file?)
        if(!Files.exists(Paths.get(getChangesFileName())) && entityNames != null && attributeNames != null){
            setupChanges(numberOfChanges);
        }
    }

    @Test
    public void simplePerformanceTest(){
        log.info("Started SimplePerformanceTest, Setup: {}", setupName);
        Instant startPt = Instant.now();

        for(String key: adapters.keySet()){
            testAttributeName(adapters.get(key), key);
            testAttributeValue(adapters.get(key), key, new AttributeBuilder().setValue("true").setType(AttributeType.String).createAttribute());
            testChanges(adapters.get(key), key);

        }

        Instant endPt = Instant.now();
        log.info("Finished SimplePerformanceTest, duration: {}", Duration.between(startPt,endPt));
    }

    private void testChanges(IDatabaseAdapter adapter, String dbName) {
        //Changes only affect filler attributes, the tests for our query attributes should not be affected?
        //We are adding and removing attributes, so it might have some effect...
        log.info("Testing running multiple attribute changes, database: {}", dbName);
        ChangeRunner runner = new ChangeRunner(adapter);

        try {
            log.info("Started Running changes, count: {}", numberOfChanges);
            Instant start = Instant.now();

            runner.applyChanges(Paths.get(getChangesFileName()));

            Instant end = Instant.now();
            Duration duration = Duration.between(start,end);
            log.info("Finished Running changes, duration: {}", duration);
            log.info("Average time per transaction: {}ms", "" + duration.toMillis()/numberOfChanges);
        } catch (IOException e) {
            log.error("Failed to read changes file");
            log.error(e.getMessage());
            e.printStackTrace();
            fail();
        } catch (SQLException throwables) {
            log.error("Failed to execute changes");
            log.error(throwables.getMessage());
            throwables.printStackTrace();
            fail();
        }
    }


    public void testAttributeName(IDatabaseAdapter dbAdapter, String dbName){
        try {
            log.info("Starting with Attributename lookup, DB: {}",  dbName);
            log.info("Started Checking for name, 20%");
            Instant startN2 = Instant.now();

            dbAdapter.findPagesByAttributeName("twentyPercent");

            Instant endN2 = Instant.now();
            log.info("Finished Checking for name, 20%, duration: {}", Duration.between(startN2,endN2));

            log.info("Started checking for name, 50%");
            Instant startN5 = Instant.now();

            dbAdapter.findPagesByAttributeName("fiftyPercent");

            Instant endN5 = Instant.now();
            log.info("Finished checking for name, 50%, duration: {}", Duration.between(startN5,endN5));

            log.info("Started checking for name, 70%");
            Instant startN7 = Instant.now();

            dbAdapter.findPagesByAttributeName("seventyPercent");

            Instant endN7 = Instant.now();
            log.info("Finished checking for name, 70%, duration: {}", Duration.between(startN7,endN7));
        } catch (SQLException throwables) {
            log.info("XXXXXXXXXXXXXXXXXXXXXXXXXX");
            log.info("Encountered an error while looking for attribute by name!!!!");
            log.info("XXXXXXXXXXXXXXXXXXXXXXXXXX");
            fail();
            throwables.printStackTrace();
        }

    }

    public void testAttributeValue(IDatabaseAdapter dbAdapter, String dbName, Attribute value){
        log.info("Starting Attribute Value Lookup, DB: {}", dbName);


        try {
            log.info("Started Checking for value, 10%");
            Instant startN2 = Instant.now();

            dbAdapter.findPagesByAttributeValue("twentyPercent", value);

            Instant endN2 = Instant.now();
            log.info("Finished Checking for value, 10%, duration: {}", Duration.between(startN2,endN2));

            log.info("Started checking for value, 25%");
            Instant startN5 = Instant.now();

            dbAdapter.findPagesByAttributeValue("fiftyPercent", value);

            Instant endN5 = Instant.now();
            log.info("Finished checking for value, 25%, duration: {}", Duration.between(startN5,endN5));

            log.info("Started checking for value, 35%");
            Instant startN7 = Instant.now();

            dbAdapter.findPagesByAttributeValue("seventyPercent", value);

            Instant endN7 = Instant.now();
            log.info("Finished checking for value, 35%, duration: {}", Duration.between(startN7,endN7));
        } catch (SQLException throwables) {
            log.info("XXXXXXXXXXXXXXXXXXXXXXXXXX");
            log.info("Encountered an error while looking for attribute by value!!!!");
            log.info("XXXXXXXXXXXXXXXXXXXXXXXXXX");
            log.error(throwables.getMessage());
            throwables.printStackTrace();
            fail();
        }
    }

}
