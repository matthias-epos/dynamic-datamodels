package de.eposcat.master.generators;

import de.eposcat.master.approachImpl.IDatabaseAdapter;
import de.eposcat.master.approachImpl.JSON_Oracle_DatabaseAdapter;
import de.eposcat.master.approachImpl.JSON_Postgres_DatabaseAdapter;
import de.eposcat.master.connection.CustomOracleConnectionManager;
import de.eposcat.master.connection.PostgresConnectionManager;
import de.eposcat.master.connection.RelationalApproach;
import de.eposcat.master.generators.data.FillerAttributesStats;
import de.eposcat.master.generators.data.PerformanceTestAttribute;
import de.eposcat.master.generators.data.StartData;
import de.eposcat.master.model.Page;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * The databases of this class get filled with data when first run and then will be reused for the tests
 * To generate new start data, delete the running containers (it seems like ou cannot name the containers to your liking)
 *
 * !!!!!!!
 * Currently there can be only one running oracle container, so you might need to stop the one from the integration tests.
 * There is probably an issue with the ports and this behavior might be fixed in a future update.
 * It is possible though to pause and resume the containers manually and save yourself some time!
 * !!!!!!!
 *
 * This class should not be run as part of the Unit and Integration Tests
 */
@Testcontainers
public class ReusableGeneratorPerformanceAnalysis {

    static final GenericContainer oracle;
    static final GenericContainer postgres;

    static final int numberOfStartAttributes = 5;
    static final int meanNumberOfAttributes = 5;
    static final int maxNumberOfAttributes = 5;

    static final int numberOfStartEntities = 10;

    static final int rngSeed = 1;

    static final IDatabaseAdapter oracleDBAdapter;
    static final IDatabaseAdapter postgresDBAdapter;

    private static final Logger log = LoggerFactory.getLogger(ReusableGeneratorPerformanceAnalysis.class);

    static {
        oracle = new GenericContainer(DockerImageName.parse("mstrepos1/dynamic_datamodels:oracle"))
                .withExposedPorts(1521).withEnv("ORACLE_PWD", "admin")
                .waitingFor(Wait.forLogMessage(".*DATABASE IS READY TO USE!\\s*",1)
                .withStartupTimeout(Duration.ofMinutes(15)))
                .withLabel("de.eposcat.performance", "true")
                .withReuse(true);

        oracle.start();

        postgres = new GenericContainer(DockerImageName.parse("mstrepos1/dynamic_datamodels:postgres"))
                .withExposedPorts(5432).withEnv("POSTGRES_PASSWORD", "admin")
                .waitingFor(Wait.forLogMessage(".*database system is ready to accept connections\\s*",2)
                .withStartupTimeout(Duration.ofMinutes(2)))
                .withLabel("de.eposcat.performance", "true")
                .withReuse(true);

        postgres.start();

        CustomOracleConnectionManager oracleConnectionManager = new CustomOracleConnectionManager(RelationalApproach.JSON, "localhost", oracle.getMappedPort(1521), "json", "json");
        oracleDBAdapter = new JSON_Oracle_DatabaseAdapter(oracleConnectionManager);

        PostgresConnectionManager postgresConnectionManager = new PostgresConnectionManager(RelationalApproach.JSON, "localhost", postgres.getMappedPort(5432), "postgres", "admin");
        postgresDBAdapter = new JSON_Postgres_DatabaseAdapter(postgresConnectionManager);


        Random r = new Random(rngSeed);
        List<PerformanceTestAttribute> queryAttributes = new ArrayList<>(Arrays.asList(
                new PerformanceTestAttribute("fiftyFifty", 50d, () -> String.valueOf(r.nextBoolean())),
                new PerformanceTestAttribute("tenPercent", 10d, () -> String.valueOf(r.nextBoolean()))
        ));

        try {
            log.info("Started Containers, generating initial data if no data exists yet");
            boolean oracleIsEmpty = oracleDBAdapter.findPagesByAttributeName(queryAttributes.get(0).getAttributeName()).size() == 0;
            boolean postgresIsEmpty = postgresDBAdapter.findPagesByAttributeName(queryAttributes.get(0).getAttributeName()).size() == 0;

            if(oracleIsEmpty || postgresIsEmpty){
                StartDataGenerator startDataGenerator = new StartDataGenerator(rngSeed);


                FillerAttributesStats filler = new FillerAttributesStats(numberOfStartAttributes, meanNumberOfAttributes, maxNumberOfAttributes);
                StartData startData = startDataGenerator.generateStartData(numberOfStartEntities, filler, queryAttributes);

                for(Page page : startData.pages){
                    if(oracleIsEmpty){
                        oracleDBAdapter.createPageWithAttributes(page.getTypeName(), page.getAttributes());
                    }

                    if(postgresIsEmpty){
                        postgresDBAdapter.createPageWithAttributes(page.getTypeName(), page.getAttributes());
                    }
                }

                log.info("Added start data to databases oracle: " +oracleIsEmpty+" and/or postgres: " +postgresIsEmpty);
            } else {
                log.info("Initial data is already present in both containers, skipping creating data step..");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void queries(){
        try {
            List<Page> pages = oracleDBAdapter.findPagesByAttributeName("fiftyFifty");
            log.info("CountO: {}", pages.size());

            pages = postgresDBAdapter.findPagesByAttributeName("fiftyFifty");
            log.info("CountP: {}", pages.size());

        } catch (SQLException throwables) {
            fail();
            throwables.printStackTrace();
        }

    }
}
