package de.eposcat.master;

import de.eposcat.master.approachImpl.EAV_DatabaseAdapter;
import de.eposcat.master.connection.CustomOracleConnectionManager;
import de.eposcat.master.connection.RelationalApproach;
import de.eposcat.master.model.AttributeBuilder;
import de.eposcat.master.model.AttributeType;
import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
public class PageOracleEavIT extends PageOracleReusableDockerContainer {

    @BeforeAll
    static void initDataBase() {
        CustomOracleConnectionManager connectionManager = new CustomOracleConnectionManager(RelationalApproach.EAV, "localhost", oracle.getMappedPort(1521), "eav", "eav");

        dbAdapter = new EAV_DatabaseAdapter(connectionManager);
        defaultAttribute = new AttributeBuilder().setType(AttributeType.String).setValue("A test value").createAttribute();
    }
}
