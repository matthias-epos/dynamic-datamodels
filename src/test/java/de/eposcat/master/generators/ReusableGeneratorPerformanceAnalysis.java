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

import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * The databases of this class get filled with data when first run and then will be reused for the tests
 * To generate new start data, delete the running containers (it seems like you cannot name the containers with testcontainers, so look for containers based on the images)
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

    static int numberOfStartAttributes = 5;
    static int meanNumberOfAttributes = 5;
    static int maxNumberOfAttributes = 5;

    static int numberOfStartEntities = 10;

    static String setupName = "";

    static final int rngSeed = 1;
    static Random r = new Random(rngSeed);

    static List<PerformanceTestAttribute> queryAttributes;

    static final IDatabaseAdapter oracleJsonDBAdapter;
    static final IDatabaseAdapter postgresJsonDBAdapter;

    static List<Long> oracleIds = null;
    static List<Long> postgresIds = null;

    static Map<String, IDatabaseAdapter> adapters = new HashMap<>();

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
        oracleJsonDBAdapter = new JSON_Oracle_DatabaseAdapter(oracleConnectionManager);
        adapters.put("oracleJson", oracleJsonDBAdapter);

        PostgresConnectionManager postgresConnectionManager = new PostgresConnectionManager(RelationalApproach.JSON, "localhost", postgres.getMappedPort(5432), "postgres", "admin");
        postgresJsonDBAdapter = new JSON_Postgres_DatabaseAdapter(postgresConnectionManager);
        adapters.put("postgresJson", postgresJsonDBAdapter);

        Random r = new Random(rngSeed);

        try {
            //Use a certain setup
            setupBestCaseScenario();

            log.info("Started Containers, generating initial data if no data exists yet");
            Map<String, Boolean> isEmptyDB = new HashMap<>();

            for(String key : adapters.keySet()){
                isEmptyDB.put(key, adapters.get(key).findPagesByAttributeName(queryAttributes.get(0).getAttributeName()).size() == 0);
            }

            boolean anyAdapterEmpty = isEmptyDB.values().contains(Boolean.valueOf(true));

            if(anyAdapterEmpty){
                StartDataGenerator startDataGenerator = new StartDataGenerator(rngSeed);


                FillerAttributesStats filler = new FillerAttributesStats(numberOfStartAttributes, meanNumberOfAttributes, maxNumberOfAttributes);
                StartData startData = startDataGenerator.generateStartData(numberOfStartEntities, filler, queryAttributes);

                oracleIds = new ArrayList<>(startData.pages.size());
                postgresIds = new ArrayList<>(startData.pages.size());


                for(Page page : startData.pages){
                    for(String key : adapters.keySet()){
                        if(isEmptyDB.get(key)){
                            adapters.get(key).createPageWithAttributes(page.getTypeName(), page.getAttributes());
                            //save Ids?
                        }
                    }
                }



                log.info("Added start data to databases");
                //TODO add which adapters

            } else {
                log.info("Initial data is already present in both containers, skipping creating data step..");
            }

//            if(oracleIsEmpty){
//                printIdsToFile(oracleIds, "oracle");
//            } else {
//                oracleIds = loadIdsFromFile(Paths.get("oracle"));
//            }
//
//            if(postgresIsEmpty){
//                printIdsToFile(postgresIds, "postgres");
//            } else {
//                postgresIds = loadIdsFromFile(Paths.get("postgres"));
//            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }




    @Test
    public void queries(){
        try {
            List<Page> pages = oracleJsonDBAdapter.findPagesByAttributeName("fiftyFifty");
            log.info("CountO: {}", pages.size());

            pages = postgresJsonDBAdapter.findPagesByAttributeName("fiftyFifty");
            log.info("CountP: {}", pages.size());

        } catch (SQLException throwables) {
            fail();
            throwables.printStackTrace();
        }

    }

    @Test
    public void simplePerformanceTest(){
        log.info("Started SimplePerformanceTest, Setup: {}", setupName);
        Instant startPt = Instant.now();

        for(String key: adapters.keySet()){
            testAttributeName(adapters.get(key), key);
        }

        Instant endPt = Instant.now();
        log.info("Finished SimplePerformanceTest, duration: {}", Duration.between(startPt,endPt));
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

    // xxxxxxxxxxxxxxxxxxxxxxxxx
    // methods to change arguments after startup, not used yet

    private static void printIdsToFile(List<Long> ids, String filename) {
        try {
            FileWriter writer = new FileWriter(Paths.get(filename).toFile(), false);
            ids.forEach(id -> {
                try {
                    writer.write(id + System.lineSeparator());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static List<Long> loadIdsFromFile(Path path) {
        try {
            return Files.readAllLines(path, Charset.defaultCharset()).stream().map(Long::valueOf).collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void applyToAllPages(IDatabaseAdapter dbAdapter, List<Long> ids){
        StartDataGenerator generator = new StartDataGenerator(rngSeed);

        for(Long id : ids){
            try {
                dbAdapter.loadPage(id);


            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
        }
    }

    public void overrideQueryAttributes(){
        StartDataGenerator generator = new StartDataGenerator(rngSeed);
    }

    static public void setupBestCaseScenario(){
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

    static public void setupCornerCaseScenario(){
        numberOfStartEntities = 100000;

        maxNumberOfAttributes = 200;
        meanNumberOfAttributes = 100;
        numberOfStartAttributes = 1000;

        queryAttributes = new ArrayList<>(Arrays.asList(
                new PerformanceTestAttribute("twentyPercent", 20d, () -> generateBigString(1000)),
                new PerformanceTestAttribute("fiftyPercent", 50d, () -> generateBigString(1000)),
                new PerformanceTestAttribute("seventyPercent", 70d, () -> generateBigString(1000))
        ));
    }

    static public String generateBigString(int sizeInKB){
        byte[] bytes = new byte[sizeInKB * 1000];
        r.nextBytes(bytes);
        return String.valueOf(bytes);
    }

}
