package de.eposcat.master.generators;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.fail;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.shaded.com.google.common.collect.Lists;

import ch.qos.logback.classic.Level;
import de.eposcat.master.approachImpl.IDatabaseAdapter;
import de.eposcat.master.generators.data.FillerAttributesStats;
import de.eposcat.master.generators.data.PerformanceTestAttribute;
import de.eposcat.master.model.Attribute;
import de.eposcat.master.model.AttributeBuilder;
import de.eposcat.master.model.AttributeType;
import de.eposcat.master.model.Page;

/**
 * The databases of this class gets filled with data when first run and then will be reused for the tests
 * To generate new start data, delete the running containers (it seems like you cannot name the containers with testContainers, so look for containers based on the images)
 * <p>
 * !!!!!!!
 * Currently there can be only one running oracle container, so you might need to stop the one from the integration tests or the other Test cases
 * There is probably an issue with the ports and this behavior might be fixed in a future update.
 * It is possible though to pause and resume the containers manually and save yourself some time!
 * !!!!!!!
 * <p>
 * This class should not be run as part of the Unit and Integration Tests
 */
@Testcontainers
public class CommonCaseSetup extends PerformanceTestContainerStartup {

    private static final Logger log = LoggerFactory.getLogger(CommonCaseSetup.class);

    private static final String TEST_CASE_NAME = "Best Case Scenario";
    private static final int NUMBER_OF_START_ENTITIES = 100000;
    private static final int NUMBER_OF_START_ATTRIBUTES = 1000;
    private static final int MEAN_NUMBER_OF_ATTRIBUTES = 100;
    private static final int MAX_NUMBER_OF_ATTRIBUTES = 200;
    private static final List<PerformanceTestAttribute> QUERY_ATTRIBUTES = Arrays.asList(
            new PerformanceTestAttribute("twentyPercent", 20d, () -> String.valueOf(RANDOM_GEN.nextBoolean())),
            new PerformanceTestAttribute("fiftyPercent", 50d, () -> String.valueOf(RANDOM_GEN.nextBoolean())),
            new PerformanceTestAttribute("seventyPercent", 70d, () -> String.valueOf(RANDOM_GEN.nextBoolean()))
    );

    @BeforeAll
    static void setupData() throws SQLException {
        POSTGRES.start();
        assertThat(POSTGRES.isRunning(), is(true));
        log.info("Postgres container is running. Generating initial data for JSON and EAV approach.");

        initAdapters();

        // generate test data only for empty DBs
        List<IDatabaseAdapter> emptyDbs = Lists.newArrayList();
        for (String approach : ADAPTERS_MAP.keySet()) {
            IDatabaseAdapter dbAdapter = ADAPTERS_MAP.get(approach);
            List<Page> pages = dbAdapter.findPagesByAttributeName(QUERY_ATTRIBUTES.get(0).getAttributeName());
            if (pages.isEmpty()) {
                emptyDbs.add(dbAdapter);
            }
        }

        StartDataGenerator startDataGenerator = new StartDataGenerator(RNG_SEED);
        FillerAttributesStats filler = new FillerAttributesStats(NUMBER_OF_START_ATTRIBUTES, MEAN_NUMBER_OF_ATTRIBUTES, MAX_NUMBER_OF_ATTRIBUTES);
        startDataGenerator.generateStartData(NUMBER_OF_START_ENTITIES, filler, QUERY_ATTRIBUTES, emptyDbs);

        log.info("Finished generating start data for empty DBs: {}, with params entitiesNum: {}, maxAttrNum: {}, startAttrNum: {}, meanAttrNum: {}",
                emptyDbs,
                NUMBER_OF_START_ENTITIES, MAX_NUMBER_OF_ATTRIBUTES,
                NUMBER_OF_START_ATTRIBUTES, MEAN_NUMBER_OF_ATTRIBUTES);

        ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger("de.eposcat.master.approachImpl")).setLevel(Level.INFO);
    }

    @Test
    public void simplePerformanceTest() {
        for (String approachName : ADAPTERS_MAP.keySet()) {
            log.info("!!TestCase: {}, Approach: {}", TEST_CASE_NAME, approachName);
            for (int i = 0; i < 6; i++) {
                if (i == 5) {
                    log.info("Warm-up is done. Lets do this!!!");
                }
                testChanges(ADAPTERS_MAP.get(approachName), approachName);
            }
        }
    }

    /**
     * Here we test the performance of all parts off the entity lifecycle
     * Create -> Add attributes -> Change single attribute -> Delete page
     */
    private void testChanges(IDatabaseAdapter adapter, String approach) {
        log.info("Testing running attribute changes, approach: {}", approach);
        Random randomGen = new Random();
        long startTime;
        long endTime;
        try {
            startTime = System.currentTimeMillis();
            Page page = adapter.createPage("attributeChangeTestPage");
            endTime = System.currentTimeMillis();
            log.info("Finished creating empty test page, duration: {} ms", endTime - startTime);

            Map<String, Attribute> newAttributes = new HashMap<>();
            for (int i = 0; i < MEAN_NUMBER_OF_ATTRIBUTES; i++) {
                newAttributes.put(randomAttributeName(randomGen), randomAttributeValue(randomGen));
            }
            newAttributes.put("changeAttribute", randomAttributeValue(randomGen));
            page.setAttributes(newAttributes);

            startTime = System.currentTimeMillis();
            adapter.updatePage(page);
            endTime = System.currentTimeMillis();
            log.info("Finished saving {} attributes, duration: {} ms", MEAN_NUMBER_OF_ATTRIBUTES + 1, endTime - startTime);

            page.addAttribute("changeAttribute", randomAttributeValue(randomGen));
            startTime = System.currentTimeMillis();
            adapter.updatePage(page);
            endTime = System.currentTimeMillis();
            log.info("++Finished changing single attribute value, approach: {}, duration: {} ms", approach, endTime - startTime);

            startTime = System.currentTimeMillis();
            adapter.deletePage(page.getId());
            endTime = System.currentTimeMillis();
            log.info("Finished deleting the test page, duration: {} ms", endTime - startTime);
        } catch (SQLException e) {
            log.error("Failed to execute changes");
            log.error(e.getMessage());
            e.printStackTrace();
            fail();
        }
    }

    private Attribute randomAttributeValue(Random randomGen) {
        return new AttributeBuilder().setType(AttributeType.String).setValue(randomGen.nextInt(400)).createAttribute();
    }

    private String randomAttributeName(Random randomGen) {
        StartDataGenerator stg = new StartDataGenerator(randomGen.nextInt());
        return stg.getRandomEntityName();
    }

}
