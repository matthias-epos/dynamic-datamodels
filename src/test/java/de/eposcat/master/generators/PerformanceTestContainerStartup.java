package de.eposcat.master.generators;

import de.eposcat.master.approachImpl.EAV_DatabaseAdapter;
import de.eposcat.master.approachImpl.IDatabaseAdapter;
import de.eposcat.master.approachImpl.JSON_Oracle_DatabaseAdapter;
import de.eposcat.master.approachImpl.JSON_Postgres_DatabaseAdapter;
import de.eposcat.master.connection.CustomOracleConnectionManager;
import de.eposcat.master.connection.PostgresConnectionManager;
import de.eposcat.master.connection.RelationalApproach;
import org.junit.jupiter.api.Disabled;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@Testcontainers
@Disabled
public abstract class PerformanceTestContainerStartup {

    static final int RNG_SEED = 1;
    static final Random RANDOM_GEN = new Random(RNG_SEED);
    static final Map<String, IDatabaseAdapter> ADAPTERS_MAP = new HashMap<>();

    public enum Database {
        DA_ORACLE,
        DA_POSTGRES
    }

    static GenericContainer<?> POSTGRES = ContainterInfo.getPostgresContainer("commonCase");
    static GenericContainer<?> ORACLE = ContainterInfo.getOracleContainer("commonCase");

    static GenericContainer getContainer(Database database){
        switch (database){
            case DA_ORACLE:
                return ORACLE;
            case DA_POSTGRES:
                return POSTGRES;
        }

        return null;
    }

    static void initAdapters(Database database) {

        switch (database){
            case DA_ORACLE:
                CustomOracleConnectionManager customOracleJSOnConnector = new CustomOracleConnectionManager(RelationalApproach.JSON,
                        "localhost", ORACLE.getMappedPort(1521), "json", "json");
                IDatabaseAdapter oracleJsonAdapter = new JSON_Oracle_DatabaseAdapter(customOracleJSOnConnector);
                ADAPTERS_MAP.put("oracleJson", oracleJsonAdapter);

                CustomOracleConnectionManager customOracleEAVConnector = new CustomOracleConnectionManager(RelationalApproach.EAV,
                        "localhost", ORACLE.getMappedPort(1521), "eav", "eav");
                IDatabaseAdapter oracleEavAdapter = new EAV_DatabaseAdapter(customOracleEAVConnector);
                ADAPTERS_MAP.put("oracleEav", oracleEavAdapter);
                break;
            case DA_POSTGRES:
                PostgresConnectionManager postgresJsonConnectionManager = new PostgresConnectionManager(RelationalApproach.JSON,
                        "localhost", POSTGRES.getMappedPort(5432), "postgres", "admin");
                IDatabaseAdapter postgresJsonDBAdapter = new JSON_Postgres_DatabaseAdapter(postgresJsonConnectionManager);
                ADAPTERS_MAP.put("postgresJson", postgresJsonDBAdapter);

                PostgresConnectionManager postgresEavConnectionManager = new PostgresConnectionManager(RelationalApproach.EAV,
                        "localhost", POSTGRES.getMappedPort(5432), "postgres", "admin");
                IDatabaseAdapter postgresEavDBAdapter = new EAV_DatabaseAdapter(postgresEavConnectionManager);
                ADAPTERS_MAP.put("postgresEav", postgresEavDBAdapter);
                break;
        }

    }
}
