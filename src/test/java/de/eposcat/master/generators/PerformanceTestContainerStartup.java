package de.eposcat.master.generators;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.junit.jupiter.api.Disabled;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

import de.eposcat.master.approachImpl.EAV_DatabaseAdapter;
import de.eposcat.master.approachImpl.IDatabaseAdapter;
import de.eposcat.master.approachImpl.JSON_Postgres_DatabaseAdapter;
import de.eposcat.master.connection.PostgresConnectionManager;
import de.eposcat.master.connection.RelationalApproach;

@Testcontainers
@Disabled
public abstract class PerformanceTestContainerStartup {

    static final int RNG_SEED = 1;
    static final Random RANDOM_GEN = new Random(RNG_SEED);

    static GenericContainer<?> POSTGRES = ContainterInfo.getPostgresContainer("performanceTest");

    static final Map<String, IDatabaseAdapter> ADAPTERS_MAP = new HashMap<>();

    static void initAdapters() {
        PostgresConnectionManager postgresJsonConnectionManager = new PostgresConnectionManager(RelationalApproach.JSON,
                "localhost", POSTGRES.getMappedPort(5432), "postgres", "admin");
        IDatabaseAdapter postgresJsonDBAdapter = new JSON_Postgres_DatabaseAdapter(postgresJsonConnectionManager);
        ADAPTERS_MAP.put("postgresJson", postgresJsonDBAdapter);

        PostgresConnectionManager postgresEavConnectionManager = new PostgresConnectionManager(RelationalApproach.EAV,
                "localhost", POSTGRES.getMappedPort(5432), "postgres", "admin");
        IDatabaseAdapter postgresEavDBAdapter = new EAV_DatabaseAdapter(postgresEavConnectionManager);
        ADAPTERS_MAP.put("postgresEav", postgresEavDBAdapter);
    }
}
