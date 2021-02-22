package de.eposcat.master.generators;

import de.eposcat.master.approachImpl.IDatabaseAdapter;
import de.eposcat.master.generators.data.PerformanceTestAttribute;
import de.eposcat.master.model.Attribute;
import de.eposcat.master.model.AttributeBuilder;
import de.eposcat.master.model.AttributeType;
import de.eposcat.master.model.Page;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

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
    }

    @Test
    public void simplePerformanceTest(){
        log.info("Started SimplePerformanceTest, Setup: {}", setupName);
        Instant startPt = Instant.now();

        for(String key: adapters.keySet()){
            log.info("++++++++++++++++++++++ " + key + " Warmup start +++++++++++++++++++++++++");
            for (int i = 0 ; i<5; i++){
                log.info("Started one test runthrough");
                Instant startR = Instant.now();

                testAttributeName(adapters.get(key), key);
                testAttributeValue(adapters.get(key), key, new AttributeBuilder().setValue("true").setType(AttributeType.String).createAttribute());
                testChanges(adapters.get(key), key);

                Instant endR = Instant.now();
                log.info("* WARMUP TIMER *");
                log.info("Run: {}; Time: {}", i, Duration.between(startR,endR));
                log.info("* WARMUP TIMER *");
            }

            log.info("++++++++++++++++++++++ " + key + " Warmup end +++++++++++++++++++++++++");

            log.info("Lets do this!!!");
            log.info("********************** Started real run **********************");
            Instant startRR = Instant.now();

            testAttributeName(adapters.get(key), key);
            testAttributeValue(adapters.get(key), key, new AttributeBuilder().setValue("true").setType(AttributeType.String).createAttribute());
            testChanges(adapters.get(key), key);

            Instant endRR = Instant.now();
            log.info("********************** Finished real run **********************");
            log.info("Duration: {}", Duration.between(startRR,endRR));
        }

        Instant endPt = Instant.now();
        log.info("Finished SimplePerformanceTest, duration: {}", Duration.between(startPt,endPt));
    }

    private void testChanges(IDatabaseAdapter adapter, String dbName) {
        //Here we test the performance of all parts off the entity lifecycle
        // Create -> Add attributes -> Change single attribute -> Delete page
        log.info("Testing running attribute changes, database/approach: {}", dbName);
        try {
            log.info("Started Creating page which will be changed");
            Instant start = Instant.now();

            Page page = adapter.createPage("attributeChangeTestPage");

            Instant end = Instant.now();
            Duration duration = Duration.between(start,end);
            log.info("Finished Creating page which will be changed, duration: {}", duration);

            Map<String, Attribute> newAttributes = new HashMap<>();

            for (int i = 0; i<meanNumberOfAttributes; i++){
                newAttributes.put(randomAttributeName(), randomAttributeValue());
            }

            newAttributes.put("changeAttribute", randomAttributeValue());
            page.setAttributes(newAttributes);

            log.info("Started saving {} entity attributes", meanNumberOfAttributes + 1);
            Instant starts = Instant.now();

            adapter.updatePage(page);

            Instant ends = Instant.now();
            log.info("Finished saving {} entity attributes, duration: {}", meanNumberOfAttributes + 1, Duration.between(starts,ends));

            page.addAttribute("changeAttribute", randomAttributeValue());

            log.info("Started changing single attribute value");
            Instant startC1 = Instant.now();

            adapter.updatePage(page);

            Instant endC1 = Instant.now();
            log.info("Finished changing single attribute value, duration: {}", Duration.between(startC1,endC1));

            log.info("Started deleting page");
            Instant startD = Instant.now();

            adapter.deletePage(page.getId());

            Instant endD = Instant.now();
            log.info("Finished deleting page, duration: {}", Duration.between(startD,endD));

        } catch (SQLException throwables) {
            log.error("Failed to execute changes");
            log.error(throwables.getMessage());
            throwables.printStackTrace();
            fail();
        }
    }

    private Attribute randomAttributeValue() {
        Random r = new Random();
        return new AttributeBuilder().setType(AttributeType.String).setValue(r.nextInt(400)).createAttribute();
    }

    private String randomAttributeName() {
        Random r = new Random();
        StartDataGenerator stg = new StartDataGenerator(r.nextInt());

        return  stg.getRandomEntityName();
    }


    public void testAttributeName(IDatabaseAdapter dbAdapter, String dbName){
        try {
            log.info("Starting with Attributename lookup, database/approach: {}",  dbName);
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
        log.info("Starting Attribute Value Lookup, database/approach: {}", dbName);


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
