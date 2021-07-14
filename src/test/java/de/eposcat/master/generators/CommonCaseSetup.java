package de.eposcat.master.generators;

import ch.qos.logback.classic.Level;
import de.eposcat.master.UseCasePerformanceHolder;
import de.eposcat.master.approachImpl.IDatabaseAdapter;
import de.eposcat.master.generators.data.FillerAttributesStats;
import de.eposcat.master.generators.data.PerformanceTestAttribute;
import de.eposcat.master.model.Attribute;
import de.eposcat.master.model.AttributeBuilder;
import de.eposcat.master.model.AttributeType;
import de.eposcat.master.model.Page;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.shaded.com.google.common.collect.Lists;

import java.sql.SQLException;
import java.util.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.fail;

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

    private static final UseCasePerformanceHolder holder = new UseCasePerformanceHolder();

    private static final Logger log = LoggerFactory.getLogger(CommonCaseSetup.class);
    private static final Logger performanceLog = LoggerFactory.getLogger("resultLogger");

    private static final Database databaseApplication = Database.DA_ORACLE;

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


        getContainer(databaseApplication).start();
        assertThat(getContainer(databaseApplication).isRunning(), is(true));
        log.info("Container is running. Generating initial data for JSON and EAV approach.");

        initAdapters(databaseApplication);

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
            MDC.put("approach", "" + databaseApplication + approachName + "TenThousand");
            holder.clear();
            log.info("!!TestCase: {}, Approach: {}", TEST_CASE_NAME, approachName);
            for (int i = 0; i < 6; i++) {
                if (i == 5) {
                    log.info("Warm-up is done. Lets do this!!!");
                }
                testChanges(ADAPTERS_MAP.get(approachName), approachName);
                testAttributeNameQuery(ADAPTERS_MAP.get(approachName), approachName);
                testAttributeValue(ADAPTERS_MAP.get(approachName), approachName, new AttributeBuilder().setValue("true").setType(AttributeType.String).createAttribute());

                // This test does not work for the 1.000.000 postgres eav with index case
                // There's no easy way to test for that so we have to skip it manually
//              if(!approachName.equals("postgresEav") ){
                    changeSingleBigAttributeTest(ADAPTERS_MAP.get(approachName), approachName);
//              }
            }

            log.info("+++++++++++++++");
            log.info(holder.toString());
            log.info("+++++++++++++++");

            performanceLog.warn(holder.toCSVEntry());
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
            holder.setCreatePage(endTime - startTime);

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
            holder.setUpdatePage(endTime - startTime);

            startTime = System.currentTimeMillis();
            adapter.deletePage(page.getId());
            endTime = System.currentTimeMillis();
            log.info("Finished deleting the test page, duration: {} ms", endTime - startTime);
            holder.setDeletePage(endTime - startTime);
        } catch (SQLException e) {
            log.error("Failed to execute changes");
            log.error(e.getMessage());
            e.printStackTrace();
            fail();
        }
    }

    public void testAttributeNameQuery(IDatabaseAdapter dbAdapter, String dbName) {
        long startTime;
        long endTime;

        try {
            startTime = System.currentTimeMillis();
            dbAdapter.findPagesByAttributeName("fiftyPercent");
            endTime = System.currentTimeMillis();
            log.info("Finished finding 100 pages with with attribute 'fiftyPercent', duration: {} ms", endTime - startTime);
            holder.setFindByAttribute(endTime - startTime);


        } catch (SQLException e) {
            log.error("Failed to execute changes");
            log.error(e.getMessage());
            e.printStackTrace();
            fail();
        }
    }

    public void testAttributeValue(IDatabaseAdapter dbAdapter, String approach, Attribute value) {
        log.info("Starting Attribute Value Lookup, database/approach: {}", approach);

        long startTime;
        long endTime;

        try {

            startTime = System.currentTimeMillis();
            dbAdapter.findPagesByAttributeValue("fiftyPercent", value);
            endTime = System.currentTimeMillis();

            log.info("Finished finding 100 pages with with attribute 'fiftyPercent' and value {}, duration: {} ms", value, endTime - startTime);
            holder.setFindByValue(endTime - startTime);
        } catch (SQLException e) {
            log.error("Failed to execute changes");
            log.error(e.getMessage());
            e.printStackTrace();
            fail();
        }
    }

    public void changeSingleBigAttributeTest(IDatabaseAdapter adapter, String approach) {
        log.info("Testing running attribute changes, approach: {}", approach);
        Random randomGen = new Random();
        long startTime;
        long endTime;
        try {
            startTime = System.currentTimeMillis();
            Page page = adapter.createPage("bigAttributeChangeTestPage");
            endTime = System.currentTimeMillis();
            log.debug("Finished creating big empty test page, duration: {} ms", endTime - startTime);

            Map<String, Attribute> newAttributes = new HashMap<>();
            for (int i = 0; i < MEAN_NUMBER_OF_ATTRIBUTES; i++) {
                newAttributes.put(randomAttributeName(randomGen), randomAttributeValue(randomGen));
            }
            newAttributes.put("bigAttribute", new AttributeBuilder().setValue(generateBigString(1000)).setType(AttributeType.String).createAttribute());
            page.setAttributes(newAttributes);

            startTime = System.currentTimeMillis();
            adapter.updatePage(page);
            endTime = System.currentTimeMillis();
            log.debug("Finished saving {} attributes, including big attribute, duration: {} ms", MEAN_NUMBER_OF_ATTRIBUTES + 1, endTime - startTime);

            //change big attribute(concat)

            Attribute bigAttribute = page.getAttribute("bigAttribute");
            bigAttribute.setValue(bigAttribute.getValue() + "concatString");
            page.addAttribute("bigAttribute", bigAttribute);

            startTime = System.currentTimeMillis();
            adapter.updatePage(page);
            endTime = System.currentTimeMillis();
            log.info("++Finished changing big attribute value, approach: {}, duration: {} ms", approach, endTime - startTime);
            holder.setChangeBigAttribute(endTime - startTime);

            startTime = System.currentTimeMillis();
            adapter.deletePage(page.getId());
            endTime = System.currentTimeMillis();
            log.debug("Finished deleting the test page, duration: {} ms", endTime - startTime);
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

    private static String generateBigString(int sizeInKB) {
        byte[] bytes = new byte[sizeInKB * 1000];
        RANDOM_GEN.nextBytes(bytes);
        return Arrays.toString(bytes);
    }

}
