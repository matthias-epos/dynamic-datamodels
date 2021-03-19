package de.eposcat.master.generators;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.fail;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.shaded.com.google.common.collect.Lists;

import de.eposcat.master.approachImpl.IDatabaseAdapter;
import de.eposcat.master.generators.data.FillerAttributesStats;
import de.eposcat.master.generators.data.PerformanceTestAttribute;
import de.eposcat.master.model.Attribute;
import de.eposcat.master.model.Page;

/**
 * The databases of this class get filled with data when first run and then will be reused for the tests
 * To generate new start data, delete the running containers (it seems like you cannot name the containers with testcontainers, so look for containers based on the images)
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
public class BigAttributesCaseSetup extends PerformanceTestContainerStartup {

    private static final Logger log = LoggerFactory.getLogger(BigAttributesCaseSetup.class);

    private static final String TEST_CASE_NAME = "Big Attribute";
    private static final int NUMBER_OF_START_ENTITIES = 100;
    private static final int NUMBER_OF_START_ATTRIBUTES = 30;
    private static final int MEAN_NUMBER_OF_ATTRIBUTES = 5;
    private static final int MAX_NUMBER_OF_ATTRIBUTES = 10;
    private static final List<PerformanceTestAttribute> QUERY_ATTRIBUTES = Arrays.asList(
            new PerformanceTestAttribute("bigAttribute", 5d, () -> generateBigString(1000)),
            new PerformanceTestAttribute("otherAttribute", 50d, () -> String.valueOf(RANDOM_GEN.nextBoolean()))
    );

    @BeforeAll
    static void setupData() {
        assertThat(POSTGRES.isRunning(), is(true));
        log.info("Postgres container is running. Generating initial data for JSON and EAV approach.");

        initAdapters();
        StartDataGenerator startDataGenerator = new StartDataGenerator(RNG_SEED);
        FillerAttributesStats filler = new FillerAttributesStats(NUMBER_OF_START_ATTRIBUTES, MEAN_NUMBER_OF_ATTRIBUTES, MAX_NUMBER_OF_ATTRIBUTES);
        startDataGenerator.generateStartData(NUMBER_OF_START_ENTITIES, filler, QUERY_ATTRIBUTES, Lists.newArrayList(ADAPTERS_MAP.values()));

        log.info("Finished generating start data.");
    }

    @Test
    public void changeBigAttributeTest() {
        IDatabaseAdapter postgresJsonAdapter = ADAPTERS_MAP.get("postgresJson");

        try {
            List<Page> pages = postgresJsonAdapter.findPagesByAttributeName("bigAttribute");
            assertThat(pages.size(), is(not(0)));
            Page page = pages.get(0);

            Attribute bigAttribute = page.getAttribute("bigAttribute");
            bigAttribute.setValue(bigAttribute.getValue() + "concatString");
            page.addAttribute("bigAttribute", bigAttribute);

            long startTime = System.currentTimeMillis();
            postgresJsonAdapter.updatePage(page);
            long endTime = System.currentTimeMillis();
            log.info("Finished changing value of single page with big attribute, duration: {} ms", endTime - startTime);

            assertThat(postgresJsonAdapter.findPagesByAttributeName("bigAttribute").size(), is(not(0)));
        } catch (SQLException e) {
            fail();
        }
    }

    private static String generateBigString(int sizeInKB) {
        byte[] bytes = new byte[sizeInKB * 1000];
        RANDOM_GEN.nextBytes(bytes);
        return Arrays.toString(bytes);
    }
}
