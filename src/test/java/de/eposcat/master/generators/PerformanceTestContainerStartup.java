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
import org.junit.jupiter.api.Disabled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Testcontainers
@Disabled
public abstract class PerformanceTestContainerStartup {
    static GenericContainer oracle;
    static GenericContainer postgres;

    static int numberOfStartAttributes;
    static int meanNumberOfAttributes;
    static int maxNumberOfAttributes;

    static int numberOfStartEntities = 10;

    static final int rngSeed = 1;
    static Random r = new Random(rngSeed);

    static List<PerformanceTestAttribute> queryAttributes;

    static IDatabaseAdapter oracleJsonDBAdapter;
    static IDatabaseAdapter postgresJsonDBAdapter;

    static Map<String, IDatabaseAdapter> adapters = new HashMap<>();

    static String setupName = "Override me!!";

    static String[] entityNames;
    static String[] attributeNames;

    private static final Logger log = LoggerFactory.getLogger(PerformanceTestContainerStartup.class);

    static void setupContainers(String label){
        oracle = ContainterInfo.getOracleContainer(label);
        oracle.start();

        postgres = ContainterInfo.getPostgresContainer(label);
        postgres.start();
    }

    public static void setupData(){
        CustomOracleConnectionManager oracleConnectionManager = new CustomOracleConnectionManager(RelationalApproach.JSON, "localhost", oracle.getMappedPort(1521), "json", "json");
        oracleJsonDBAdapter = new JSON_Oracle_DatabaseAdapter(oracleConnectionManager);
        adapters.put("oracleJson", oracleJsonDBAdapter);

        PostgresConnectionManager postgresConnectionManager = new PostgresConnectionManager(RelationalApproach.JSON, "localhost", postgres.getMappedPort(5432), "postgres", "admin");
        postgresJsonDBAdapter = new JSON_Postgres_DatabaseAdapter(postgresConnectionManager);
        adapters.put("postgresJson", postgresJsonDBAdapter);

        try {
            log.info("Started Containers, generating initial data if no data exists yet");
            Map<String, Boolean> isEmptyDB = new HashMap<>();

            for(String key : adapters.keySet()){
                isEmptyDB.put(key, adapters.get(key).findPagesByAttributeName(queryAttributes.get(0).getAttributeName()).size() == 0);
            }

            boolean anyAdapterEmpty = isEmptyDB.containsValue(true);

            if(anyAdapterEmpty){
                StartDataGenerator startDataGenerator = new StartDataGenerator(rngSeed);


                FillerAttributesStats filler = new FillerAttributesStats(numberOfStartAttributes, meanNumberOfAttributes, maxNumberOfAttributes);
                StartData startData = startDataGenerator.generateStartData(numberOfStartEntities, filler, queryAttributes);

                for(Page page : startData.pages){
                    for(String key : adapters.keySet()){
                        if(isEmptyDB.get(key)){
                            adapters.get(key).createPageWithAttributes(page.getTypeName(), page.getAttributes());
                        }
                    }
                }

                entityNames = startData.entityNames;
                attributeNames = startData.attributeNames;

                log.info("Added start data to databases");
                //TODO add which adapters
            } else {
                log.info("Initial data is already present in both containers, skipping creating data step..");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void setupChanges(int numberOfChanges){
        ChangesGenerator generator = new ChangesGenerator(entityNames, attributeNames, getChangesFileName(), 1);

        try {
            generator.generateChangeSets(numberOfChanges);
        } catch (IOException ex){
            log.error("Failed to create change set.");
            log.error(ex.getMessage());
        }
    }

    public static String getChangesFileName(){
        return setupName + "changeSet.txt";
    }
}
