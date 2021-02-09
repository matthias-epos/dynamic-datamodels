package de.eposcat.master.generators;

import de.eposcat.master.generators.data.PerformanceTestAttribute;
import de.eposcat.master.model.Attribute;
import de.eposcat.master.model.Page;
import org.hamcrest.core.Is;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
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
public class BigAttributesCaseSetup extends PerformanceTestContainerStartup{

    private static final Logger log = LoggerFactory.getLogger(BigAttributesCaseSetup.class);

    static {
        setupContainers("cornerCase");
        startup();
    }

    static public void setupCornerCaseScenario(){
        numberOfStartEntities = 1000;

        maxNumberOfAttributes = 10;
        meanNumberOfAttributes = 5;
        numberOfStartAttributes = 30;

        queryAttributes = new ArrayList<>(Arrays.asList(
                new PerformanceTestAttribute("bigAttribute", 5d, () -> generateBigString(1000)),
                new PerformanceTestAttribute("otherAttribute", 50d, () -> String.valueOf(r.nextBoolean()))
        ));
    }

    static public String generateBigString(int sizeInKB){
        byte[] bytes = new byte[sizeInKB * 1000];
        r.nextBytes(bytes);
        return Arrays.toString(bytes);
    }

    static void startup(){
        setupCornerCaseScenario();
        setupData();
    }

    @Test
    public void changeBigAttributeTest(){
        try {
            log.info("Started finding pages with big attribute");
            Instant startq = Instant.now();

            List<Page> pages = postgresJsonDBAdapter.findPagesByAttributeName("bigAttribute");
            assertThat(pages.size(), not(0));

            Instant endq = Instant.now();
            log.info("Finished finding pages with big attribute, duration: {}", Duration.between(startq,endq));

            Page page = pages.get(0);

            log.info("Started changing value of single page with big attribute");
            Instant startChange = Instant.now();

            Attribute bigAttribute = page.getAttribute("bigAttribute");
            bigAttribute.setValue(bigAttribute.getValue() + "concatString");
            page.addAttribute("bigAttribute", bigAttribute);
            postgresJsonDBAdapter.updatePage(page);


            Instant endChange = Instant.now();
            log.info("Finished changing value of single page with big attribute, duration: {}", Duration.between(startChange,endChange));



            assertThat(postgresJsonDBAdapter.findPagesByAttributeName("bigAttribute").size(), not(0));
        } catch (SQLException throwables) {
            fail();
        }
    }

}
