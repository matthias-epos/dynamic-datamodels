package de.eposcat.master;

import de.eposcat.master.approachImpl.JSON_Oracle_DatabaseAdapter;
import de.eposcat.master.connection.CustomOracleConnectionManager;
import de.eposcat.master.connection.RelationalApproach;
import de.eposcat.master.model.AttributeBuilder;
import de.eposcat.master.model.AttributeType;
import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
public class PageOracleJsonIT extends PageOracleReusableDockerContainer {

    @BeforeAll
    static void initDataBase(){
        CustomOracleConnectionManager connectionManager = new CustomOracleConnectionManager(RelationalApproach.JSON, "localhost", oracle.getMappedPort(1521), "json", "json");

        dbAdapter = new JSON_Oracle_DatabaseAdapter(connectionManager);
        defaultAttribute = new AttributeBuilder().setType(AttributeType.String).setValue("A test value").createAttribute();
    }
}
