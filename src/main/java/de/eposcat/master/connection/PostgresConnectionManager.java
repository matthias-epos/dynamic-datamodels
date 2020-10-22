package de.eposcat.master.connection;

import java.util.Properties;

public class PostgresConnectionManager extends AbstractConnectionManager {

    public PostgresConnectionManager(RelationalApproach approach) {
        connectionString = "jdbc:postgresql://localhost/";
        
        switch(approach) {
            case EAV:
                connectionString += "test_eav";
                break;
            //Wird gestrichen?
            case TABLE_PER_TYPE:
                throw new RuntimeException("Not supported type");
            case JSON:
                connectionString += "test_json";
                break;
            case KEY_VALUE_STORE:
                throw new RuntimeException("Not supported type");
               
        }
        
        databaseProperties = new Properties();
        databaseProperties.put("user", "postgres");
        databaseProperties.put("password", "admin");
    }
}
